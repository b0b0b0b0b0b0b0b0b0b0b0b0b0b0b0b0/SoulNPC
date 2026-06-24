package bm.b0b0b0.SoulNPC.lang;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

final class LangLocaleSynchronizer {

    static final int CURRENT_VERSION = 1;

    private LangLocaleSynchronizer() {
    }

    static void syncAll(JavaPlugin plugin, Path langFolder) {
        langFolder.toFile().mkdirs();
        sync(plugin, langFolder, "ru");
        sync(plugin, langFolder, "en");
    }

    private static void sync(JavaPlugin plugin, Path langFolder, String code) {
        YamlConfiguration defaults = loadDefaults(plugin, code);
        if (defaults == null) {
            return;
        }
        defaults.set("meta.version", CURRENT_VERSION);

        Path target = langFolder.resolve(code + ".yml");
        if (!Files.exists(target)) {
            try {
                defaults.save(target.toFile());
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to create lang/" + code + ".yml: " + exception.getMessage());
            }
            return;
        }

        YamlConfiguration disk = YamlConfiguration.loadConfiguration(target.toFile());
        int merged = mergeMissing(defaults, disk);
        disk.set("meta.version", CURRENT_VERSION);
        if (merged <= 0) {
            return;
        }
        try {
            disk.save(target.toFile());
            Logger logger = plugin.getLogger();
            logger.info("[SoulNPC] lang/" + code + ".yml: добавлено ключей из JAR — " + merged);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to update lang/" + code + ".yml: " + exception.getMessage());
        }
    }

    private static YamlConfiguration loadDefaults(JavaPlugin plugin, String code) {
        try (InputStream stream = plugin.getResource("lang/" + code + ".yml")) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to read lang/" + code + ".yml from JAR: " + exception.getMessage());
            return null;
        }
    }

    private static int mergeMissing(ConfigurationSection defaults, ConfigurationSection target) {
        int merged = 0;
        for (String key : defaults.getKeys(false)) {
            if ("meta".equals(key)) {
                continue;
            }
            merged += mergeKey(defaults, target, key);
        }
        return merged;
    }

    private static int mergeKey(ConfigurationSection defaults, ConfigurationSection target, String key) {
        if (defaults.isConfigurationSection(key)) {
            if (!target.isConfigurationSection(key)) {
                target.createSection(key);
            }
            return mergeMissing(defaults.getConfigurationSection(key), target.getConfigurationSection(key));
        }
        if (!target.contains(key)) {
            target.set(key, defaults.get(key));
            return 1;
        }
        if (defaults.isList(key) && target.isList(key)) {
            return mergeList(defaults.getList(key), target.getList(key), target, key);
        }
        return 0;
    }

    private static int mergeList(List<?> defaults, List<?> existing, ConfigurationSection target, String key) {
        List<Object> merged = new ArrayList<>(existing);
        int added = 0;
        for (Object item : defaults) {
            if (!merged.contains(item)) {
                merged.add(item);
                added++;
            }
        }
        if (added > 0) {
            target.set(key, merged);
        }
        return added;
    }
}
