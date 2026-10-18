package dev.worldpresence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

/** Windows Discord IPC. Only the worker touches the pipe, including its shutdown. */
final class DiscordIpc implements AutoCloseable {
    private static final long RETRY_NANOS = TimeUnit.SECONDS.toNanos(15);
    private final String applicationId;
    private final String pipePrefix;
    private final CompletableFuture<CheckResult> checkResult;
    private final java.util.concurrent.ScheduledExecutorService worker;
    private volatile JsonObject activity;
    private volatile boolean closed;
    private RandomAccessFile pipe;
    private FileInputStream input;
    private boolean ready;
    private long nextConnect;
    private long connectedAt;
    private long nextPublish;
    private JsonObject sentActivity;
    private int opcode = -1;
    private byte[] payload;
    private int received;

    DiscordIpc(String applicationId) {
        // RandomAccessFile strips the extended-path prefix; use the Windows device namespace.
        this(applicationId, "\\\\.\\pipe\\discord-ipc-");
    }

    DiscordIpc(String applicationId, String pipePrefix) {
        this(applicationId, pipePrefix, null);
    }

    private DiscordIpc(String applicationId, String pipePrefix, CompletableFuture<CheckResult> checkResult) {
        this.applicationId = applicationId;
        this.pipePrefix = pipePrefix;
        this.checkResult = checkResult;
        worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "world-presence-discord");
            thread.setDaemon(true);
            return thread;
        });
        worker.scheduleWithFixedDelay(this::update, 0, 100, TimeUnit.MILLISECONDS);
    }

    record CheckResult(boolean successful, String message) { }

    static CompletableFuture<CheckResult> checkApplicationId(String id) {
        return checkApplicationId(id, "\\\\.\\pipe\\discord-ipc-");
    }

    static CompletableFuture<CheckResult> checkApplicationId(String id, String pipePrefix) {
        if (!PresenceText.validApplicationId(id) || id.isEmpty()) {
            return CompletableFuture.completedFuture(new CheckResult(false, "Введите корректный ID приложения"));
        }
        var result = new CompletableFuture<CheckResult>();
        var probe = new DiscordIpc(id, pipePrefix, result);
        result.whenComplete((value, error) -> probe.close());
        result.completeOnTimeout(new CheckResult(false, "Discord не ответил за 10 секунд"), 10, TimeUnit.SECONDS);
        return result;
    }

    void setActivity(JsonObject activity) {
        if (!closed) this.activity = activity == null ? null : activity.deepCopy();
    }

    private void update() {
        if (closed) return;
        long now = System.nanoTime();
        try {
            if (pipe == null) {
                if ((activity == null && checkResult == null) || now < nextConnect) return;
                nextConnect = now + RETRY_NANOS;
                connect();
            }
            readAvailable();
            if (closed) return;
            if (!ready) {
                if (now - connectedAt > RETRY_NANOS) throw new IOException("Discord handshake timed out");
                return;
            }
            JsonObject latest = activity;
            if (now < nextPublish && Objects.equals(latest, sentActivity)) return;
            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", latest);
            JsonObject command = new JsonObject();
            command.addProperty("cmd", "SET_ACTIVITY");
            command.addProperty("nonce", UUID.randomUUID().toString());
            command.add("args", args);
            send(1, command.toString().getBytes(StandardCharsets.UTF_8));
            sentActivity = latest;
            nextPublish = now + RETRY_NANOS;
        } catch (IOException | RuntimeException e) {
            if (checkResult != null) checkResult.complete(new CheckResult(false,
                    pipe == null ? "Discord недоступен. Запустите Discord" : "Ошибка связи с Discord"));
            if (!closed) WorldPresence.LOG.debug("Discord IPC disconnected; will retry", e);
            disconnect();
            nextConnect = now + RETRY_NANOS;
        }
    }

    private void connect() throws IOException {
        if (!System.getProperty("os.name").startsWith("Windows")) throw new IOException("Windows only");
        IOException firstFailure = null;
        for (int i = 0; i < 10; i++) {
            try {
                pipe = new RandomAccessFile(pipePrefix + i, "rw");
            } catch (IOException e) {
                if (firstFailure == null) firstFailure = e;
                continue;
            }
            input = new FileInputStream(pipe.getFD());
            connectedAt = System.nanoTime();
            JsonObject handshake = new JsonObject();
            handshake.addProperty("v", 1);
            handshake.addProperty("client_id", applicationId);
            send(0, handshake.toString().getBytes(StandardCharsets.UTF_8));
            return;
        }
        throw new IOException("Discord IPC unavailable", firstFailure);
    }

    private void readAvailable() throws IOException {
        // Windows available() uses PeekNamedPipe. A pending synchronous read also
        // blocks writes and CloseHandle on this handle, so read only available bytes.
        for (int frames = 0; frames < 32; frames++) {
            int available = input.available();
            if (opcode == -1) {
                if (available < 8) return;
                opcode = Integer.reverseBytes(pipe.readInt());
                int length = Integer.reverseBytes(pipe.readInt());
                if (opcode < 0 || opcode > 4 || length < 0 || length > 1_048_576) {
                    throw new IOException("Invalid Discord IPC frame");
                }
                payload = new byte[length];
                received = 0;
                available -= 8;
            }
            int count = Math.min(available, payload.length - received);
            pipe.readFully(payload, received, count);
            received += count;
            if (received < payload.length) return;
            int completedOpcode = opcode;
            byte[] bytes = payload;
            opcode = -1;
            payload = null;
            if (completedOpcode == 2) {
                reportRejection(JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject());
                throw new IOException("Discord closed the connection");
            }
            if (completedOpcode == 3) send(4, bytes);
            if (completedOpcode == 1) {
                JsonObject message = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                String event = message.has("evt") && !message.get("evt").isJsonNull()
                        ? message.get("evt").getAsString() : "";
                if (event.equals("READY")) {
                    ready = true;
                    if (checkResult != null) checkResult.complete(new CheckResult(true, "Discord принял ID приложения"));
                } else if (event.equals("ERROR")) {
                    JsonObject data = message.has("data") && message.get("data").isJsonObject()
                            ? message.getAsJsonObject("data") : new JsonObject();
                    reportRejection(data);
                    WorldPresence.LOG.warn("Discord RPC: {}", data);
                }
            }
            if (closed) return;
        }
    }

    private void reportRejection(JsonObject data) {
        if (checkResult == null) return;
        String code = data.has("code") ? " (" + data.get("code").getAsString() + ")" : "";
        checkResult.complete(new CheckResult(false, "Discord отклонил ID" + code));
    }

    private void send(int opcode, byte[] bytes) throws IOException {
        byte[] frame = ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(opcode).putInt(bytes.length).put(bytes).array();
        pipe.write(frame);
    }

    private void disconnect() {
        if (pipe != null) {
            try { pipe.close(); } catch (IOException ignored) { }
        }
        pipe = null;
        input = null;
        ready = false;
        opcode = -1;
        payload = null;
        sentActivity = null;
        nextPublish = 0;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        worker.execute(this::disconnect);
        worker.shutdown();
    }
}
