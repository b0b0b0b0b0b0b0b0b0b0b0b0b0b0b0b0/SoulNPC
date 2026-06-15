package bm.b0b0b0.SoulNPC.model;

public enum NpcGreetStyle {
    /** Кивок головой (pitch). */
    NOD,
    /** Один раз присесть (EntityPose.CROUCHING), только player NPC. */
    CROUCH;

    public static NpcGreetStyle fromString(String value) {
        if (value == null || value.isBlank()) {
            return NOD;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return NOD;
        }
    }
}
