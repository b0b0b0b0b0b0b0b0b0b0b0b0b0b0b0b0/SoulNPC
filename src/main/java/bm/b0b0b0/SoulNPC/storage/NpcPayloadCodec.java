package bm.b0b0b0.SoulNPC.storage;

import bm.b0b0b0.SoulNPC.model.NpcActionType;
import bm.b0b0b0.SoulNPC.model.NpcClickBinding;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcInteractionAction;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NpcPayloadCodec {

    private static Logger logger;

    private NpcPayloadCodec() {
    }

    public static void setLogger(Logger log) {
        logger = log;
    }

    public static String encode(NpcFileData data) throws IOException {
        NpcFileData copy = data;
        copy.prepareForYamlSave();
        Path temp = Files.createTempFile("soulnpc-payload-", ".yml");
        try {
            copy.save(temp);
            return Files.readString(temp);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static NpcFileData decode(String payload, String fallbackId) throws IOException {
        Path temp = Files.createTempFile("soulnpc-payload-", ".yml");
        try {
            Files.writeString(temp, payload == null ? "" : payload);
            NpcFileData data = new NpcFileData();
            data.reload(temp);
            applyLegacyMigrations(payload, data);
            repairCorruptedActions(payload, data);
            if (data.id == null || data.id.isBlank()) {
                data.id = fallbackId == null ? "unknown" : fallbackId;
            }
            data.interaction.ensureActionsMigrated();
            return data;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static NpcFileData decodeFromPath(Path path) throws IOException {
        String raw = Files.readString(path);
        NpcFileData data = new NpcFileData();
        data.reload(path);
        applyLegacyMigrations(raw, data);
        repairCorruptedActions(raw, data);
        if (data.id == null || data.id.isBlank()) {
            String fileName = path.getFileName().toString();
            if (fileName.endsWith(".yml")) {
                data.id = fileName.substring(0, fileName.length() - 4);
            }
        }
        data.interaction.ensureActionsMigrated();
        return data;
    }

    private static void applyLegacyMigrations(String rawYaml, NpcFileData data) {
        if (rawYaml == null || rawYaml.isBlank()) {
            return;
        }
        YamlConfiguration raw;
        try {
            raw = new YamlConfiguration();
            raw.loadFromString(rawYaml);
        } catch (InvalidConfigurationException exception) {
            logFine("Skipping Bukkit legacy migration — Elytrium payload is not compatible with Bukkit YAML parser");
            return;
        }
        data.appearance.migrateLegacyDescription(raw.getString("appearance.description"));
        migrateLegacyExtraLineStrings(raw, data);
    }

    private static void logFine(String message) {
        if (logger != null) {
            logger.log(Level.FINE, message);
        }
    }

    private static void migrateLegacyExtraLineStrings(YamlConfiguration raw, NpcFileData data) {
        List<?> entries = raw.getList("appearance.extra-lines");
        if (entries == null || entries.isEmpty()) {
            return;
        }
        if (!(entries.get(0) instanceof String)) {
            return;
        }
        List<String> legacy = new ArrayList<>(entries.size());
        for (Object entry : entries) {
            if (entry instanceof String line) {
                legacy.add(line);
            }
        }
        data.appearance.migrateLegacyExtraLineStrings(legacy);
    }

    private static void repairCorruptedActions(String rawYaml, NpcFileData data) {
        data.interaction.normalizeActions();
        if (!data.interaction.actions.isEmpty()) {
            return;
        }
        Matcher clickMatcher = Pattern.compile("(?m)^click:\\s*(\\w+)\\s*$").matcher(rawYaml);
        if (!clickMatcher.find()) {
            return;
        }
        NpcInteractionAction action = new NpcInteractionAction();
        try {
            action.click = NpcClickBinding.valueOf(clickMatcher.group(1).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return;
        }
        Matcher typeMatcher = Pattern.compile("(?m)^[ \\t]{6}type:\\s*(\\w+)\\s*$").matcher(rawYaml);
        if (typeMatcher.find()) {
            try {
                action.type = NpcActionType.valueOf(typeMatcher.group(1).trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        Matcher valueMatcher = Pattern.compile("(?m)^[ \\t]{6}value:\\s*(.*)$").matcher(rawYaml);
        if (valueMatcher.find()) {
            action.value = unquoteYamlScalar(valueMatcher.group(1).trim());
        }
        if (!action.isActionable()) {
            return;
        }
        data.interaction.actions.add(action);
        logFine("Recovered interaction action from corrupted YAML layout");
    }

    private static String unquoteYamlScalar(String raw) {
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        if (raw.length() >= 2 && raw.startsWith("'") && raw.endsWith("'")) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }
}
