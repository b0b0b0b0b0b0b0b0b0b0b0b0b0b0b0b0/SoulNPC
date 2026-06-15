package bm.b0b0b0.SoulNPC.model;

import java.util.Locale;
import java.util.Optional;

public enum NpcFoxVariant {
    RED,
    SNOW;

    public static NpcFoxVariant fromString(String value) {
        return tryParse(value).orElse(RED);
    }

    public static Optional<NpcFoxVariant> tryParse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "SNOW", "WHITE", "1" -> Optional.of(SNOW);
            case "RED", "0" -> Optional.of(RED);
            default -> Optional.empty();
        };
    }

    public static String[] tabChoices() {
        return new String[]{"red", "snow"};
    }

    public int packetId() {
        return ordinal();
    }
}
