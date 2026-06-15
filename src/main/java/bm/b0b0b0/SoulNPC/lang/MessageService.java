package bm.b0b0b0.SoulNPC.lang;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MessageService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final PluginConfig pluginConfig;
    private final Map<String, YamlConfiguration> locales = new HashMap<>();

    public MessageService(JavaPlugin plugin, PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
        Path langFolder = plugin.getDataFolder().toPath().resolve("lang");
        langFolder.toFile().mkdirs();
        installLocale(plugin, langFolder, "ru");
        installLocale(plugin, langFolder, "en");
        loadLocale(langFolder, "ru");
        loadLocale(langFolder, "en");
    }

    public void reload(JavaPlugin plugin) {
        locales.clear();
        Path langFolder = plugin.getDataFolder().toPath().resolve("lang");
        loadLocale(langFolder, "ru");
        loadLocale(langFolder, "en");
    }

    public Component message(Player player, String path, TagResolver... resolvers) {
        String locale = resolveLocale(player);
        YamlConfiguration config = locales.getOrDefault(locale, locales.get("ru"));
        String raw = config.getString(path, path);
        String prefix = config.getString("prefix", "");
        raw = raw.replace("<prefix>", prefix);
        raw = bracesToMiniMessage(raw);
        List<TagResolver> all = new ArrayList<>(resolvers.length + 1);
        all.add(Placeholder.parsed("prefix", prefix));
        for (TagResolver resolver : resolvers) {
            all.add(resolver);
        }
        return MINI.deserialize(raw, TagResolver.resolver(all.toArray(TagResolver[]::new)));
    }

    public List<Component> messageList(Player player, String path, TagResolver... resolvers) {
        String locale = resolveLocale(player);
        YamlConfiguration config = locales.getOrDefault(locale, locales.get("ru"));
        List<String> lines = config.getStringList(path);
        List<Component> result = new ArrayList<>(lines.size());
        String prefix = config.getString("prefix", "");
        TagResolver[] all = new TagResolver[resolvers.length + 1];
        all[0] = Placeholder.parsed("prefix", prefix);
        System.arraycopy(resolvers, 0, all, 1, resolvers.length);
        for (String line : lines) {
            line = line.replace("<prefix>", prefix);
            line = bracesToMiniMessage(line);
            result.add(MINI.deserialize(line, TagResolver.resolver(all)));
        }
        return result;
    }

    public Component raw(String miniMessage, TagResolver... resolvers) {
        return MINI.deserialize(miniMessage, TagResolver.resolver(resolvers));
    }

    public Component chatCancelButton(Player player) {
        String locale = resolveLocale(player);
        YamlConfiguration config = locales.getOrDefault(locale, locales.get("ru"));
        String raw = config.getString("gui.edit.chat-cancel-button", "<red>[Отменить]</red>");
        return MINI.deserialize(bracesToMiniMessage(raw));
    }

    public Component guiTitle(String path, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        YamlConfiguration config = locales.get(pluginConfig.settings().general.defaultLocale);
        if (config == null) {
            config = locales.get("ru");
        }
        String raw = config.getString(path, path);
        raw = bracesToMiniMessage(raw);
        return MINI.deserialize(raw, TagResolver.resolver(resolvers));
    }

    public String plain(Player player, String path) {
        String locale = resolveLocale(player);
        YamlConfiguration config = locales.getOrDefault(locale, locales.get("ru"));
        return config.getString(path, path);
    }

    public List<String> plainList(Player player, String path) {
        String locale = resolveLocale(player);
        YamlConfiguration config = locales.getOrDefault(locale, locales.get("ru"));
        return config.getStringList(path);
    }

    private String resolveLocale(Player player) {
        String configured = pluginConfig.settings().general.defaultLocale;
        if (player != null) {
            String locale = player.locale().toLanguageTag().toLowerCase(Locale.ROOT);
            if (locale.startsWith("ru") && locales.containsKey("ru")) {
                return "ru";
            }
            if (locale.startsWith("en") && locales.containsKey("en")) {
                return "en";
            }
        }
        if (locales.containsKey(configured)) {
            return configured;
        }
        return "ru";
    }

    private void installLocale(JavaPlugin plugin, Path langFolder, String code) {
        Path target = langFolder.resolve(code + ".yml");
        if (Files.exists(target)) {
            return;
        }
        try (InputStream stream = plugin.getResource("lang/" + code + ".yml")) {
            if (stream == null) {
                return;
            }
            Files.createDirectories(langFolder);
            Files.copy(stream, target);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to install lang/" + code + ".yml: " + exception.getMessage());
        }
    }

    private static String bracesToMiniMessage(String raw) {
        return raw.replaceAll("\\{([a-zA-Z0-9_-]+)\\}", "<$1>");
    }

    private void loadLocale(Path langFolder, String code) {
        Path file = langFolder.resolve(code + ".yml");
        if (!Files.exists(file)) {
            return;
        }
        locales.put(code, YamlConfiguration.loadConfiguration(file.toFile()));
    }
}
