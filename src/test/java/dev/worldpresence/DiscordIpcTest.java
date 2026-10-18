package dev.worldpresence;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real Windows named pipes, on a unique name that cannot contact the user's Discord. */
final class DiscordIpcTest {
    static void run() throws Exception {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            System.out.println("Windows IPC integration check skipped on this OS");
            return;
        }
        String name = "world-presence-test-" + UUID.randomUUID();
        String prefix = "\\\\.\\pipe\\" + name + "-";
        var output = new LinkedBlockingQueue<String>();
        Process server = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-File",
                Path.of("src/test/resources/mock-discord.ps1").toAbsolutePath().toString(), name + "-0")
                .redirectErrorStream(true).start();
        Thread reader = new Thread(() -> {
            try (var lines = server.inputReader()) { lines.lines().forEach(output::add); }
            catch (Exception e) { output.add(e.toString()); }
        });
        reader.setDaemon(true);
        reader.start();
        try {
            await(output, "LISTENING", null);
            try (var rpc = new DiscordIpc("123456789012345678", prefix)) {
                JsonObject world = new JsonObject();
                world.addProperty("details", "Test world");
                world.addProperty("state", "Unicode: Привет 😀");
                JsonObject timestamps = new JsonObject();
                timestamps.addProperty("start", 1234567890);
                world.add("timestamps", timestamps);
                rpc.setActivity(world);
                await(output, "WORLD_OK", null);
                long slowestUpdate = 0;
                for (int i = 1; i <= 3; i++) {
                    long changedAt = System.nanoTime();
                    world.addProperty("details", "Update " + i);
                    rpc.setActivity(world);
                    await(output, "UPDATE_" + i, null);
                    long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - changedAt);
                    assert elapsed < 2000 : "IPC update took " + elapsed + " ms";
                    slowestUpdate = Math.max(slowestUpdate, elapsed);
                }
                System.out.println("Changed activity reached mock Discord within " + slowestUpdate + " ms");
                rpc.setActivity(PresenceText.menuActivity("Alex"));
                await(output, "LISTENING_AGAIN", null);
                await(output, "RECONNECTED", () -> rpc.setActivity(PresenceText.menuActivity("Alex")));
            }
            await(output, "LISTENING_NEW_ID", null);
            try (var rpc = new DiscordIpc("234567890123456789", prefix)) {
                rpc.setActivity(PresenceText.menuActivity("Alex"));
                await(output, "NEW_ID_OK", null);
            }
            await(output, "CLOSED", null);
            if (!server.waitFor(5, TimeUnit.SECONDS) || server.exitValue() != 0) {
                throw new AssertionError("Mock Discord did not exit cleanly");
            }
            System.out.println("Windows IPC checks passed: handshake, UTF-8, ping, menu, reconnect, ID change, shutdown");
        } finally {
            server.destroyForcibly();
        }
        var invalid = DiscordIpc.checkApplicationId("bad-id", prefix);
        assert invalid.isDone() && !invalid.join().successful();
        var unavailable = DiscordIpc.checkApplicationId("123456789012345678", prefix);
        assert unavailable.get(5, TimeUnit.SECONDS).message().contains("недоступен");
        for (String mode : new String[]{"ready", "reject", "error", "timeout", "cancel"}) checkProbe(mode);
        System.out.println("Application ID checks passed: READY, rejection, ERROR, unavailable, timeout, cancellation; no activity sent");
    }

    private static void checkProbe(String mode) throws Exception {
        String name = "world-presence-probe-test-" + UUID.randomUUID();
        var output = new LinkedBlockingQueue<String>();
        Process server = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-File",
                Path.of("src/test/resources/mock-discord.ps1").toAbsolutePath().toString(), name + "-0", mode)
                .redirectErrorStream(true).start();
        Thread reader = new Thread(() -> {
            try (var lines = server.inputReader()) { lines.lines().forEach(output::add); }
            catch (Exception e) { output.add(e.toString()); }
        });
        reader.setDaemon(true);
        reader.start();
        try {
            await(output, "LISTENING", null);
            var probe = DiscordIpc.checkApplicationId("123456789012345678", "\\\\.\\pipe\\" + name + "-");
            try {
                await(output, "PROBE_HANDSHAKE", null);
                if (mode.equals("cancel")) {
                    probe.cancel(false);
                    assert probe.isCancelled();
                } else {
                    var result = probe.get(12, TimeUnit.SECONDS);
                    assert result.successful() == mode.equals("ready") : mode + ": " + result;
                    if (mode.equals("reject") || mode.equals("error")) assert result.message().contains("4000");
                    if (mode.equals("timeout")) assert result.message().contains("10 секунд");
                }
                await(output, "PROBE_CLOSED", null);
                assert server.waitFor(5, TimeUnit.SECONDS) && server.exitValue() == 0;
            } finally {
                probe.cancel(false);
            }
        } finally {
            server.destroyForcibly();
        }
    }

    private static void await(LinkedBlockingQueue<String> output, String expected, Runnable retry) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(35);
        StringBuilder received = new StringBuilder();
        while (System.nanoTime() < deadline) {
            String line = output.poll(100, TimeUnit.MILLISECONDS);
            if (expected.equals(line)) return;
            if (line != null) received.append(line).append('\n');
            if (retry != null) retry.run();
        }
        Thread.getAllStackTraces().forEach((thread, stack) -> {
            if (thread.getName().startsWith("world-presence")) {
                received.append(thread.getName()).append(':').append('\n');
                for (StackTraceElement element : stack) received.append(element).append('\n');
            }
        });
        throw new AssertionError("Waiting for " + expected + ":\n" + received);
    }
}
