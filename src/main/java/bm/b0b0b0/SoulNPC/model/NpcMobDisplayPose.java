package bm.b0b0b0.SoulNPC.model;

public enum NpcMobDisplayPose {
    /** Обычная стойка (тихоня стоит, летучая мышь летит). */
    STANDING,
    /** На спину (тихоня и похожие — EntityPose.SLEEPING). */
    ON_BACK,
    /** Вниз головой / висит (летучая мышь — флаг hanging). */
    HANGING;

    public static NpcMobDisplayPose fromString(String value) {
        if (value == null || value.isBlank()) {
            return STANDING;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_');
        return switch (normalized) {
            case "ON_BACK", "LYING", "BACK", "SUPINE", "SLEEPING" -> ON_BACK;
            case "HANGING", "UPSIDE_DOWN", "UPSIDE", "INVERTED" -> HANGING;
            case "UPRIGHT", "NORMAL", "STANDING" -> STANDING;
            default -> {
                try {
                    yield valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield STANDING;
                }
            }
        };
    }
}
