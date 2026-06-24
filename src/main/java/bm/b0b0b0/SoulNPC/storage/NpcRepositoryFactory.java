package bm.b0b0b0.SoulNPC.storage;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

public final class NpcRepositoryFactory {

    private NpcRepositoryFactory() {
    }

    public static NpcStorageBackend createActiveBackend(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            DatabaseLifecycle lifecycle
    ) {
        return createBackend(plugin, pluginConfig, StorageType.fromConfig(pluginConfig.settings().storage.type), lifecycle);
    }

    public static NpcStorageBackend createBackend(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            StorageType type,
            DatabaseLifecycle lifecycle
    ) {
        ExecutorService executor = StorageExecutors.create(type.configKey());
        NpcStorageBackend backend = switch (type) {
            case SQLITE -> new SqliteStorageBackend(
                    plugin,
                    plugin.getDataFolder().toPath().resolve(pluginConfig.settings().storage.sqlite.file),
                    executor
            );
            case MYSQL -> new MySqlStorageBackend(
                    plugin,
                    pluginConfig.settings().storage.mysql,
                    executor
            );
            case YAML -> new YamlStorageBackend(
                    plugin,
                    yamlFolder(plugin, pluginConfig),
                    executor
            );
        };
        lifecycle.register(backend);
        return backend;
    }

    public static Path yamlFolder(JavaPlugin plugin, PluginConfig pluginConfig) {
        return plugin.getDataFolder().toPath().resolve(pluginConfig.yamlNpcFolder());
    }
}
