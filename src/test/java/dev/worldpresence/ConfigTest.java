package dev.worldpresence;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;

final class ConfigTest {
    static void run() throws Exception {
        var directory = Files.createTempDirectory("world-presence-config-test-");
        var config = directory.resolve("config.json");
        var blocked = directory.resolve("not-a-file");
        var sentinel = blocked.resolve("keep.txt");
        try {
            WorldPresence.writeConfig(config, "123456789012345678");
            String original = Files.readString(config);
            assert JsonParser.parseString(original).getAsJsonObject().get("applicationId")
                    .getAsString().equals("123456789012345678");
            try {
                WorldPresence.writeConfig(config, "bad-id");
                throw new AssertionError("Invalid ID was saved");
            } catch (IllegalArgumentException expected) { }
            assert Files.readString(config).equals(original);
            WorldPresence.writeConfig(config, "");
            assert JsonParser.parseString(Files.readString(config)).getAsJsonObject()
                    .get("applicationId").getAsString().isEmpty();
            Files.createDirectory(blocked);
            Files.writeString(sentinel, "preserve");
            try {
                WorldPresence.writeConfig(blocked, "123456789012345678");
                throw new AssertionError("Saving to a directory should fail");
            } catch (IOException expected) { }
            assert Files.readString(sentinel).equals("preserve");
            try (var files = Files.list(directory)) {
                assert files.noneMatch(path -> path.toString().endsWith(".tmp"));
            }
            System.out.println("Config checks passed: save, replace, disable, invalid ID, failed write preserves data");
        } finally {
            Files.deleteIfExists(sentinel);
            Files.deleteIfExists(blocked);
            Files.deleteIfExists(config);
            Files.deleteIfExists(directory);
        }
    }
}
