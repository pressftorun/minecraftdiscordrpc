package dev.worldpresence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.io.IOException;
import java.time.Instant;

@Mod(value = "world_presence", dist = Dist.CLIENT)
public final class WorldPresence {
    static final Logger LOG = LoggerFactory.getLogger("world_presence");
    static WorldPresence instance;
    private final Path path = FMLPaths.CONFIGDIR.get().resolve("world-presence.json");
    private Config config = new Config();
    private DiscordIpc rpc;
    private long started;
    private long nextUpdate;

    public WorldPresence(ModContainer container) {
        instance = this;
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(parent));
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            if (!Files.exists(path)) {
                writeConfig(path, "");
            }
            Config loaded = gson.fromJson(Files.readString(path), Config.class);
            if (loaded != null && PresenceText.validApplicationId(loaded.applicationId)) config = loaded;
            else LOG.warn("Некорректный ID приложения в {}. Укажите ID через меню модов", path);
        } catch (Exception e) {
            LOG.error("Не удалось прочитать конфиг {}. Укажите ID через меню модов", path, e);
        }
        restartRpc();
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
            started = Instant.now().getEpochSecond();
            nextUpdate = 0;
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            started = 0;
            nextUpdate = 0;
            if (rpc != null) rpc.setActivity(PresenceText.menuActivity(Minecraft.getInstance().getUser().getName()));
        });
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> {
            if (rpc != null) rpc.close();
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            var client = Minecraft.getInstance();
            if (rpc == null) return;
            boolean inWorld = client.player != null && client.level != null;
            // Login/logout own the timer. Respawn and dimension loading are not new sessions.
            if (started != 0 && !inWorld) return;
            long now = System.nanoTime();
            if (now < nextUpdate) return;
            nextUpdate = now + 1_000_000_000L;
            if (started == 0) {
                rpc.setActivity(PresenceText.menuActivity(client.getUser().getName()));
                return;
            }
            String location = "Одиночная игра";
            if (!client.hasSingleplayerServer()) {
                var server = client.getCurrentServer();
                location = "Сервер: " + (server == null ? "неизвестен" : server.ip);
            }
            var position = client.player.blockPosition();
            var biome = client.level.getBiome(position).unwrapKey();
            String biomeName = biome.map(key -> VanillaNames.biome(key.identifier().toString()))
                    .orElse("Неизвестный биом");
            String dimensionName = VanillaNames.dimension(client.level.dimension().identifier().toString());
            JsonObject activity = PresenceText.baseActivity();
            activity.addProperty("details", PresenceText.details(client.player.getGameProfile().name(), location));
            activity.addProperty("state", PresenceText.stats(biomeName, dimensionName, client.level.getOverworldClockTime()));
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", started);
            activity.add("timestamps", timestamps);
            rpc.setActivity(activity);
        });
    }

    String applicationId() {
        return config.applicationId;
    }

    void saveApplicationId(String value) throws IOException {
        String id = value.strip();
        writeConfig(path, id);
        Config updated = new Config();
        updated.applicationId = id;
        config = updated;
        restartRpc();
    }

    static void writeConfig(Path path, String id) throws IOException {
        if (!PresenceText.validApplicationId(id)) throw new IllegalArgumentException("Invalid ID приложения");
        Config updated = new Config();
        updated.applicationId = id;
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "world-presence-", ".tmp");
        try {
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(updated));
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void restartRpc() {
        if (rpc != null) rpc.close();
        rpc = config.applicationId.isEmpty() ? null : new DiscordIpc(config.applicationId);
        nextUpdate = 0;
    }

    private static final class Config {
        String applicationId = "";
    }
}
