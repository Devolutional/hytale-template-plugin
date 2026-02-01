package org.alias.rpgPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;

public class Configuration {
    public static Boolean runNetworkPacketDebugger = false;
    public static Long playerHookFrequency = 30L;

    // Cached last-modified time for the config file (milliseconds since epoch).
    // Volatile so reads don't need synchronization.
    private static volatile long configLastModified = 0L;
    private static final Object CONFIG_LOCK = new Object();

    public static Boolean getRunNetworkPacketDebugger() {
        // Only reload from disk if the file changed to avoid a disk read on every call.
        reloadIfChanged(false);
        return runNetworkPacketDebugger;
    }

    public static Long getPlayerHookFrequency() {
        // Only reload from disk if the file changed to avoid a disk read on every call.
        reloadIfChanged(false);
        return playerHookFrequency;
    }

    public static void setRunNetworkPacketDebugger(Boolean _runNetworkPacketDebugger) {
        runNetworkPacketDebugger = _runNetworkPacketDebugger;
    }
    public static void setPlayerHookFrequency(Long _playerHookFrequency) {
        playerHookFrequency = _playerHookFrequency;
    }

    public static void save() {

        synchronized (Configuration.class) {
            try {
                Path configPath = getConfigPath();
                Files.createDirectories(configPath.getParent());

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject root = new JsonObject();
                root.addProperty("runNetworkPacketDebugger", runNetworkPacketDebugger != null && runNetworkPacketDebugger);
                root.addProperty("playerHookFrequency", playerHookFrequency != null ? playerHookFrequency : 30L);

                String json = gson.toJson(root);
                // Write string (modern API) and update cached last-modified timestamp
                Files.writeString(configPath, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                try {
                    FileTime ft = Files.getLastModifiedTime(configPath);
                    configLastModified = ft.toMillis();
                } catch (IOException ignored) {
                    configLastModified = System.currentTimeMillis();
                }
            } catch (IOException e) {
                System.err.println("Failed to save configuration: " + e.getMessage());
                e.printStackTrace();
            }
        }

    }

    public static void load() {

        // For backward-compatibility, provide a full forced load API that ignores caching.
        reloadIfChanged(true);

    }

    /**
     * Reload config only if file changed or if forced.
     * This avoids disk I/O on every getter call while still allowing hot-reload when the file changes.
     */
    private static void reloadIfChanged(boolean force) {
        synchronized (CONFIG_LOCK) {
            try {
                Path configPath = getConfigPath();
                if (!Files.exists(configPath)) {
                    if (force) {
                        // nothing to load; keep defaults
                        configLastModified = 0L;
                    }
                    return;
                }

                long lastModified;
                try {
                    FileTime ft = Files.getLastModifiedTime(configPath);
                    lastModified = ft.toMillis();
                } catch (IOException ex) {
                    // If we can't read the timestamp, fallback to forcing a read only if requested
                    lastModified = 0L;
                }

                if (!force && lastModified != 0L && lastModified == configLastModified) {
                    return; // no change
                }

                // Read file content
                String json = Files.readString(configPath, StandardCharsets.UTF_8);
                Gson gson = new Gson();
                JsonElement parsed = gson.fromJson(json, JsonElement.class);
                if (parsed != null && parsed.isJsonObject()) {
                    JsonObject root = parsed.getAsJsonObject();
                    if (root.has("runNetworkPacketDebugger")) {
                        try {
                            runNetworkPacketDebugger = root.get("runNetworkPacketDebugger").getAsBoolean();
                        } catch (Exception ignored) {}
                    }
                    if (root.has("playerHookFrequency")) {
                        try {
                            playerHookFrequency = root.get("playerHookFrequency").getAsLong();
                        } catch (Exception ignored) {}
                    }
                }

                // Update last modified cache (best effort)
                if (lastModified != 0L) {
                    configLastModified = lastModified;
                } else {
                    try {
                        configLastModified = Files.getLastModifiedTime(configPath).toMillis();
                    } catch (IOException ignored) {
                        configLastModified = System.currentTimeMillis();
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to reload configuration: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Resolve the configuration file path relative to the JAR (or classes) location.
     * Result: {path-to-jar-or-classes}/config/rpg/configuration-server.json
     */
    private static Path getConfigPath() {
        try {
            URL location = Configuration.class.getProtectionDomain().getCodeSource().getLocation();
            Path base = Paths.get(location.toURI());
            // If running from a JAR file, use the parent directory of the JAR.
            if (Files.isRegularFile(base)) {
                base = base.getParent();
            }
            return base.resolve("config").resolve("rpg").resolve("configuration-server.json");
        } catch (URISyntaxException e) {
            // Fall back to working directory
            System.err.println("Warning: failed to resolve JAR location, falling back to working directory: " + e.getMessage());
            return Paths.get("config", "rpg", "configuration-server.json").toAbsolutePath();
        }
    }
}
