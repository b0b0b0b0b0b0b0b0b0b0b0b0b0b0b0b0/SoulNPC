package bm.b0b0b0.SoulNPC.lang;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MessageService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final TagResolver CMD_SUGGEST = TagResolver.resolver("cmd", MessageService::cmdSuggestTag);

    private final PluginConfig pluginConfig;
    private final Map<String, YamlConfiguration> locales = new HashMap<>();

    public MessageService(JavaPlugin plugin, PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
        Path langFolder = plugin.getDataFolder().toPath().resolve("lang");
        LangLocaleSynchronizer.syncAll(plugin, langFolder);
        loadLocale(langFolder, "ru");
        loadLocale(langFolder, "en");
    }

    public void reload(JavaPlugin plugin) {
        locales.clear();
        Path langFolder = plugin.getDataFolder().toPath().resolve("lang");
        LangLocaleSynchronizer.syncAll(plugin, langFolder);
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
        return MINI.deserialize(raw, mergeResolvers(prefix, resolvers));
    }

    public List<Component> messageList(Player player, String path, TagResolver... resolvers) {
        String locale = resolveLocale(player);
        YamlConfiguration config = locales.getOrDefault(locale, locales.get("ru"));
        List<String> lines = config.getStringList(path);
        List<Component> result = new ArrayList<>(lines.size());
        String prefix = config.getString("prefix", "");
        TagResolver merged = mergeResolvers(prefix, resolvers);
        for (String line : lines) {
            line = line.replace("<prefix>", prefix);
            line = bracesToMiniMessage(line);
            result.add(MINI.deserialize(line, merged));
        }
        return result;
    }

    public Component raw(String miniMessage, TagResolver... resolvers) {
        return MINI.deserialize(miniMessage, mergeResolvers("", resolvers));
    }

    public Component chatCancelButton(Player player) {
        String locale = resolveLocale(player);
        YamlConfiguration config = locales.getOrDefault(locale, locales.get("ru"));
        String raw = config.getString("gui.edit.chat-cancel-button", "<red>[Отменить]</red>");
        return MINI.deserialize(bracesToMiniMessage(raw), mergeResolvers("", new TagResolver[0]));
    }

    public Component guiTitle(String path, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        YamlConfiguration config = locales.get(pluginConfig.settings().general.defaultLocale);
        if (config == null) {
            config = locales.get("ru");
        }
        String raw = config.getString(path, path);
        raw = bracesToMiniMessage(raw);
        return MINI.deserialize(raw, mergeResolvers("", resolvers));
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

    private static String bracesToMiniMessage(String raw) {
        return raw.replaceAll("\\{([a-zA-Z0-9_-]+)\\}", "<$1>");
    }

    private static TagResolver mergeResolvers(String prefix, TagResolver... resolvers) {
        List<TagResolver> all = new ArrayList<>(resolvers.length + 2);
        all.add(CMD_SUGGEST);
        all.add(Placeholder.parsed("prefix", prefix));
        for (TagResolver resolver : resolvers) {
            all.add(resolver);
        }
        return TagResolver.resolver(all.toArray(TagResolver[]::new));
    }

    private static Tag cmdSuggestTag(ArgumentQueue args, Context context) {
        String commandTemplate = args.popOr("Usage: <cmd:'/command'[:color]>").value();
        TextColor color = NamedTextColor.WHITE;
        if (args.hasNext()) {
            TextColor resolved = NamedTextColor.NAMES.value(args.pop().value());
            if (resolved != null) {
                color = resolved;
            }
        }
        String command = PLAIN.serialize(context.deserialize(commandTemplate));
        return Tag.selfClosingInserting(
                Component.text(command, color).clickEvent(ClickEvent.suggestCommand(command))
        );
    }

    private void loadLocale(Path langFolder, String code) {
        Path file = langFolder.resolve(code + ".yml");
        if (!Files.exists(file)) {
            return;
        }
        locales.put(code, YamlConfiguration.loadConfiguration(file.toFile()));
    }
}
