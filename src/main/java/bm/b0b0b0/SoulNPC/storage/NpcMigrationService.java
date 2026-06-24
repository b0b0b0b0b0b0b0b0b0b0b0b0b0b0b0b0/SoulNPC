package bm.b0b0b0.SoulNPC.storage;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class NpcMigrationService {

    public record Result(int imported, int skipped, int errors) {
    }

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final DatabaseLifecycle lifecycle;

    public NpcMigrationService(JavaPlugin plugin, PluginConfig pluginConfig, DatabaseLifecycle lifecycle) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.lifecycle = lifecycle;
    }

    public CompletableFuture<Result> migrate(
            StorageType from,
            StorageType to,
            boolean dryRun,
            boolean overwrite
    ) {
        DatabaseLifecycle sourceLifecycle = new DatabaseLifecycle();
        DatabaseLifecycle targetLifecycle = new DatabaseLifecycle();
        NpcStorageBackend source = NpcRepositoryFactory.createBackend(plugin, pluginConfig, from, sourceLifecycle);
        NpcStorageBackend target = NpcRepositoryFactory.createBackend(plugin, pluginConfig, to, targetLifecycle);

        return source.loadAll().thenCompose(sourceData -> target.loadAll().thenCompose(existingTarget -> {
            Map<String, NpcFileData> toWrite = new LinkedHashMap<>();
            int skipped = 0;
            int errors = 0;
            for (Map.Entry<String, NpcFileData> entry : sourceData.entrySet()) {
                String id = entry.getKey();
                if (existingTarget.containsKey(id) && !overwrite) {
                    skipped++;
                    continue;
                }
                toWrite.put(id, entry.getValue());
            }
            if (dryRun) {
                sourceLifecycle.closeAll();
                targetLifecycle.closeAll();
                return CompletableFuture.completedFuture(new Result(toWrite.size(), skipped, errors));
            }
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (Map.Entry<String, NpcFileData> entry : toWrite.entrySet()) {
                NpcFileData data = entry.getValue();
                chain = chain.thenCompose(ignored -> target.save(entry.getKey(), data).exceptionally(error -> {
                    return null;
                }));
            }
            int imported = toWrite.size();
            int finalSkipped = skipped;
            return chain.thenApply(ignored -> {
                sourceLifecycle.closeAll();
                targetLifecycle.closeAll();
                return new Result(imported, finalSkipped, errors);
            });
        }));
    }

    public static StorageType parseType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("empty");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "yaml" -> StorageType.YAML;
            case "sqlite" -> StorageType.SQLITE;
            case "mysql" -> StorageType.MYSQL;
            default -> throw new IllegalArgumentException(value);
        };
    }

    public static void runOnMain(JavaPlugin plugin, Runnable action) {
        Bukkit.getScheduler().runTask(plugin, action);
    }
}
