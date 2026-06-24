package bm.b0b0b0.SoulNPC.storage;

import java.util.Locale;

public enum StorageType {
    YAML,
    SQLITE,
    MYSQL;

    public static StorageType fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return YAML;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "sqlite" -> SQLITE;
            case "mysql" -> MYSQL;
            default -> YAML;
        };
    }

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
