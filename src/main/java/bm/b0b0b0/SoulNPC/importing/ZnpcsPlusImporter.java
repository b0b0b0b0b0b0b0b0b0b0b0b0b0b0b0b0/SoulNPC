package bm.b0b0b0.SoulNPC.importing;

import bm.b0b0b0.SoulNPC.model.NpcActionType;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcClickBinding;
import bm.b0b0b0.SoulNPC.model.NpcDisplayType;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcHologramLineData;
import bm.b0b0b0.SoulNPC.model.NpcInteractionAction;
import bm.b0b0b0.SoulNPC.model.NpcInteractionData;
import bm.b0b0b0.SoulNPC.model.NpcSkinSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class ZnpcsPlusImporter {

    public record Result(int imported, int skipped, int errors) {
    }

    private ZnpcsPlusImporter() {
    }

    public static CompletableFuture<Map<String, NpcFileData>> readFolderAsync(Path folder) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, NpcFileData> result = new LinkedHashMap<>();
            if (folder == null || !Files.isDirectory(folder)) {
                return result;
            }
            try (Stream<Path> files = Files.list(folder)) {
                files.filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                        .forEach(path -> {
                            try {
                                NpcFileData data = parseFile(path.toFile());
                                if (data != null && data.id != null && !data.id.isBlank()) {
                                    result.put(data.id.toLowerCase(Locale.ROOT), data);
                                }
                            } catch (Exception ignored) {
                            }
                        });
            } catch (Exception ignored) {
            }
            return result;
        });
    }

    public static NpcFileData parseFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            id = dot > 0 ? name.substring(0, dot) : name;
        }
        NpcFileData data = new NpcFileData(id.toLowerCase(Locale.ROOT));
        data.enabled = yaml.getBoolean("enabled", true);
        data.world = yaml.getString("world", "world");

        ConfigurationSection location = yaml.getConfigurationSection("location");
        if (location != null) {
            data.x = location.getDouble("x", data.x);
            data.y = location.getDouble("y", data.y);
            data.z = location.getDouble("z", data.z);
            data.yaw = (float) location.getDouble("yaw", data.yaw);
            data.pitch = (float) location.getDouble("pitch", data.pitch);
        } else {
            data.x = yaml.getDouble("x", data.x);
            data.y = yaml.getDouble("y", data.y);
            data.z = yaml.getDouble("z", data.z);
            data.yaw = (float) yaml.getDouble("yaw", data.yaw);
            data.pitch = (float) yaml.getDouble("pitch", data.pitch);
        }

        String type = yaml.getString("type", "PLAYER");
        applyType(data.appearance, type);

        ConfigurationSection properties = yaml.getConfigurationSection("properties");
        if (properties != null) {
            applyProperties(data, properties);
        }

        ConfigurationSection hologram = yaml.getConfigurationSection("hologram");
        if (hologram != null) {
            applyHologram(data.appearance, hologram);
        } else if (yaml.isList("hologram.lines")) {
            applyHologramLines(data.appearance, yaml.getStringList("hologram.lines"));
        }

        applyActions(data.interaction, yaml);
        data.prepareForYamlSave();
        return data;
    }

    private static void applyType(NpcAppearanceData appearance, String type) {
        if (type == null || type.isBlank()) {
            return;
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if ("PLAYER".equals(normalized)) {
            appearance.type = NpcDisplayType.PLAYER;
            return;
        }
        appearance.type = NpcDisplayType.MOB;
        appearance.entityType = normalized.toLowerCase(Locale.ROOT);
    }

    private static void applyProperties(NpcFileData data, ConfigurationSection properties) {
        NpcAppearanceData appearance = data.appearance;
        String skin = firstNonBlank(properties.getString("skin"), properties.getString("skin_name"));
        if (skin != null) {
            appearance.skinSource = NpcSkinSource.NICK;
            appearance.profile = skin;
        }
        String skinUrl = firstNonBlank(properties.getString("skin_url"), properties.getString("skinurl"));
        if (skinUrl != null) {
            appearance.skinSource = NpcSkinSource.URL;
            appearance.skinUrl = skinUrl;
        }
        String displayName = firstNonBlank(properties.getString("displayname"), properties.getString("display_name"), properties.getString("name"));
        if (displayName != null) {
            appearance.name = legacyToMiniMessage(displayName);
        }
        if (properties.contains("glow")) {
            appearance.glow = properties.getBoolean("glow");
        }
        String glowColor = firstNonBlank(properties.getString("glow_name"), properties.getString("glow_color"), properties.getString("glowcolor"));
        if (glowColor != null) {
            appearance.glowColor = glowColor.toLowerCase(Locale.ROOT);
        }
        if (properties.contains("look") || properties.contains("look_at")) {
            data.lookAtPlayers = properties.getBoolean("look", properties.getBoolean("look_at", data.lookAtPlayers));
        }
        if (properties.contains("scale")) {
            appearance.scale = (float) properties.getDouble("scale", appearance.scale);
        }
        if (properties.contains("visibility_distance")) {
            data.packetViewDistance = properties.getInt("visibility_distance");
        }
        String permission = firstNonBlank(properties.getString("permission"), properties.getString("visibility_permission"));
        if (permission != null) {
            data.visibilityPermission = permission;
            data.interaction.permission = permission;
        }
        applyMobPropertyMap(appearance, properties);
    }

    private static void applyMobPropertyMap(NpcAppearanceData appearance, ConfigurationSection properties) {
        if (appearance.mobProperties == null) {
            appearance.mobProperties = new LinkedHashMap<>();
        }
        copyProperty(properties, appearance.mobProperties, "baby", "is_baby");
        copyProperty(properties, appearance.mobProperties, "fox_variant", "fox_type");
        copyProperty(properties, appearance.mobProperties, "sheep_color", "color");
        copyProperty(properties, appearance.mobProperties, "creeper_charged", "powered");
        copyProperty(properties, appearance.mobProperties, "villager_profession", "profession");
        copyProperty(properties, appearance.mobProperties, "wolf_collar", "collar_color");
        if (properties.contains("sitting")) {
            appearance.entityPose = properties.getBoolean("sitting") ? NpcEntityPose.SITTING : NpcEntityPose.STANDING;
        }
    }

    private static void copyProperty(ConfigurationSection section, Map<String, String> target, String key, String altKey) {
        if (section.contains(key)) {
            target.put(key, String.valueOf(section.get(key)));
        } else if (section.contains(altKey)) {
            target.put(key, String.valueOf(section.get(altKey)));
        }
    }

    private static void applyHologram(NpcAppearanceData appearance, ConfigurationSection hologram) {
        if (hologram.contains("offset")) {
            appearance.hologramBaseOffset = (float) hologram.getDouble("offset", appearance.hologramBaseOffset);
        }
        if (hologram.isList("lines")) {
            applyHologramLines(appearance, hologram.getStringList("lines"));
        }
    }

    private static void applyHologramLines(NpcAppearanceData appearance, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        appearance.ensureExtraLines();
        if (!appearance.extraLines.isEmpty()) {
            return;
        }
        boolean first = true;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (first && (appearance.name == null || appearance.name.isBlank() || "<white>NPC</white>".equals(appearance.name))) {
                appearance.name = legacyToMiniMessage(line);
                first = false;
                continue;
            }
            appearance.extraLines.add(NpcHologramLineData.of(legacyToMiniMessage(line)));
            first = false;
        }
    }

    private static void applyActions(NpcInteractionData interaction, YamlConfiguration yaml) {
        if (yaml.isList("actions")) {
            for (Object entry : yaml.getList("actions")) {
                if (entry instanceof ConfigurationSection section) {
                    importActionSection(interaction, section);
                } else if (entry instanceof Map<?, ?> map) {
                    importActionMap(interaction, map);
                }
            }
            return;
        }
        ConfigurationSection actions = yaml.getConfigurationSection("actions");
        if (actions == null) {
            return;
        }
        for (String clickKey : actions.getKeys(false)) {
            NpcClickBinding binding = clickBinding(clickKey);
            if (binding == null) {
                continue;
            }
            List<String> lines = actions.getStringList(clickKey);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                interaction.actions.add(NpcInteractionActionParserCompat.fromLegacyLine(line.trim(), binding));
            }
        }
    }

    private static void importActionSection(NpcInteractionData interaction, ConfigurationSection section) {
        String click = section.getString("click", "RIGHT");
        NpcClickBinding binding = clickBinding(click);
        if (binding == null) {
            return;
        }
        List<Map<?, ?>> actionList = section.getMapList("actions");
        for (Map<?, ?> actionMap : actionList) {
            NpcInteractionAction action = new NpcInteractionAction();
            action.click = binding;
            Object typeValue = actionMap.containsKey("type") ? actionMap.get("type") : "MESSAGE";
            action.type = actionType(String.valueOf(typeValue));
            Object value = actionMap.get("value");
            if (value == null) {
                value = actionMap.get("text");
            }
            action.value = value == null ? "" : String.valueOf(value);
            interaction.actions.add(action);
        }
    }

    private static void importActionMap(NpcInteractionData interaction, Map<?, ?> map) {
        Object click = map.get("click");
        NpcClickBinding binding = clickBinding(click == null ? "RIGHT" : String.valueOf(click));
        if (binding == null) {
            return;
        }
        Object actions = map.get("actions");
        if (!(actions instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> actionMap)) {
                continue;
            }
            NpcInteractionAction action = new NpcInteractionAction();
            action.click = binding;
            Object typeValue = actionMap.containsKey("type") ? actionMap.get("type") : "MESSAGE";
            action.type = actionType(String.valueOf(typeValue));
            Object value = actionMap.get("value");
            if (value == null) {
                value = actionMap.get("text");
            }
            action.value = value == null ? "" : String.valueOf(value);
            interaction.actions.add(action);
        }
    }

    private static NpcClickBinding clickBinding(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT).replace('-', '_')) {
            case "LEFT", "LEFT_CLICK", "CLICK_LEFT" -> NpcClickBinding.LEFT;
            case "RIGHT", "RIGHT_CLICK", "CLICK_RIGHT" -> NpcClickBinding.RIGHT;
            case "MIDDLE", "MIDDLE_CLICK", "CREATIVE" -> NpcClickBinding.MIDDLE;
            case "SHIFT_LEFT", "SHIFT_LEFT_CLICK" -> NpcClickBinding.SHIFT_LEFT;
            case "SHIFT_RIGHT", "SHIFT_RIGHT_CLICK" -> NpcClickBinding.SHIFT_RIGHT;
            case "ANY" -> NpcClickBinding.ANY;
            default -> null;
        };
    }

    private static NpcActionType actionType(String raw) {
        if (raw == null) {
            return NpcActionType.MESSAGE;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "MESSAGE", "MSG" -> NpcActionType.MESSAGE;
            case "CONSOLE", "CONSOLE_CMD", "CMD_CONSOLE" -> NpcActionType.CONSOLE_CMD;
            case "OP", "OP_CMD" -> NpcActionType.OP_CMD;
            case "PLAYER", "PLAYER_CMD", "CMD" -> NpcActionType.PLAYER_CMD;
            case "SWITCH_SERVER", "SERVER", "BUNGEE" -> NpcActionType.SWITCH_SERVER;
            default -> NpcActionType.MESSAGE;
        };
    }

    private static String legacyToMiniMessage(String input) {
        if (input == null) {
            return "";
        }
        if (input.contains("<") && input.contains(">")) {
            return input;
        }
        return input.replace('&', '§');
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static final class NpcInteractionActionParserCompat {
        private NpcInteractionActionParserCompat() {
        }

        static NpcInteractionAction fromLegacyLine(String raw, NpcClickBinding binding) {
            return bm.b0b0b0.SoulNPC.model.NpcInteractionActionParser.fromLegacyLine(raw, binding);
        }
    }
}
