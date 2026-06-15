package bm.b0b0b0.SoulNPC.repository;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class YamlNpcRepository implements NpcRepository {

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final Map<String, NpcFileData> cache = new LinkedHashMap<>();

    public YamlNpcRepository(JavaPlugin plugin, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
    }

    @Override
    public Collection<NpcFileData> findAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public Optional<NpcFileData> findById(String id) {
        return Optional.ofNullable(cache.get(normalizeId(id)));
    }

    @Override
    public void save(NpcFileData data) {
        String id = normalizeId(data.id);
        data.id = id;
        data.prepareForYamlSave();
        cache.put(id, data);
        Path file = npcFile(id);
        file.getParent().toFile().mkdirs();
        data.save(file);
    }

    @Override
    public boolean delete(String id) {
        String normalized = normalizeId(id);
        if (!cache.containsKey(normalized)) {
            return false;
        }
        cache.remove(normalized);
        Path file = npcFile(normalized);
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to delete NPC file " + normalized + ": " + exception.getMessage());
        }
        return true;
    }

    @Override
    public void reload() {
        cache.clear();
        Path folder = npcFolder();
        folder.toFile().mkdirs();
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.yml")) {
            for (Path path : stream) {
                NpcFileData data = new NpcFileData();
                data.reload(path);
                if (data.id == null || data.id.isBlank()) {
                    String fileName = path.getFileName().toString();
                    data.id = fileName.substring(0, fileName.length() - 4);
                }
                cache.put(normalizeId(data.id), data);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to load NPC files: " + exception.getMessage());
        }
    }

    private Path npcFolder() {
        return plugin.getDataFolder().toPath().resolve(pluginConfig.settings().general.npcFolder);
    }

    private Path npcFile(String id) {
        return npcFolder().resolve(id + ".yml");
    }

    private static String normalizeId(String id) {
        return id.toLowerCase();
    }
}
