package bm.b0b0b0.SoulNPC.storage;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class NpcPayloadCodec {

    private NpcPayloadCodec() {
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
            if (data.id == null || data.id.isBlank()) {
                data.id = fallbackId == null ? "unknown" : fallbackId;
            }
            return data;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static NpcFileData decodeFromPath(Path path) throws IOException {
        NpcFileData data = new NpcFileData();
        data.reload(path);
        String raw = Files.readString(path);
        applyLegacyMigrations(raw, data);
        if (data.id == null || data.id.isBlank()) {
            String fileName = path.getFileName().toString();
            if (fileName.endsWith(".yml")) {
                data.id = fileName.substring(0, fileName.length() - 4);
            }
        }
        return data;
    }

    private static void applyLegacyMigrations(String rawYaml, NpcFileData data) {
        if (rawYaml == null || rawYaml.isBlank()) {
            return;
        }
        YamlConfiguration raw = YamlConfiguration.loadConfiguration(new StringReader(rawYaml));
        data.appearance.migrateLegacyDescription(raw.getString("appearance.description"));
        migrateLegacyExtraLineStrings(raw, data);
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
}
