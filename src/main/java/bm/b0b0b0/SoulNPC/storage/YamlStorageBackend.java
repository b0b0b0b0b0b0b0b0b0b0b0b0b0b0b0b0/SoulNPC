package bm.b0b0b0.SoulNPC.storage;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class YamlStorageBackend implements NpcStorageBackend {

    private final JavaPlugin plugin;
    private final Path npcFolder;
    private final ExecutorService executor;

    public YamlStorageBackend(JavaPlugin plugin, Path npcFolder, ExecutorService executor) {
        this.plugin = plugin;
        this.npcFolder = npcFolder;
        this.executor = executor;
    }

    @Override
    public StorageType type() {
        return StorageType.YAML;
    }

    @Override
    public CompletableFuture<Map<String, NpcFileData>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, NpcFileData> result = new LinkedHashMap<>();
            try {
                Files.createDirectories(npcFolder);
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to create NPC folder: " + exception.getMessage());
                return result;
            }
            if (!Files.isDirectory(npcFolder)) {
                return result;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(npcFolder, "*.yml")) {
                for (Path path : stream) {
                    try {
                        NpcFileData data = NpcPayloadCodec.decodeFromPath(path);
                        result.put(normalizeId(data.id), data);
                    } catch (Exception exception) {
                        plugin.getLogger().warning("Failed to load NPC file " + path.getFileName() + ": "
                                + exception.getMessage());
                    }
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to scan NPC folder: " + exception.getMessage());
            }
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(String id, NpcFileData data) {
        return CompletableFuture.runAsync(() -> {
            String normalized = normalizeId(id);
            data.id = normalized;
            data.prepareForYamlSave();
            Path file = npcFolder.resolve(normalized + ".yml");
            try {
                Files.createDirectories(npcFolder);
                data.save(file);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to save NPC " + normalized + ": " + exception.getMessage(),
                        exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            Path file = npcFolder.resolve(normalizeId(id) + ".yml");
            try {
                Files.deleteIfExists(file);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to delete NPC file " + id + ": " + exception.getMessage(),
                        exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<String> nextAutoId(Map<String, NpcFileData> currentCache) {
        return CompletableFuture.supplyAsync(() -> {
            int candidate = 1;
            while (currentCache.containsKey(String.valueOf(candidate))) {
                candidate++;
            }
            return String.valueOf(candidate);
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    public Path npcFolder() {
        return npcFolder;
    }

    private static String normalizeId(String id) {
        return id.toLowerCase();
    }
}
