package bm.b0b0b0.SoulNPC.model;

public enum NpcMobPoseMode {
    SEQUENTIAL,
    RANDOM;

    public static NpcMobPoseMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return SEQUENTIAL;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return SEQUENTIAL;
        }
    }
}
