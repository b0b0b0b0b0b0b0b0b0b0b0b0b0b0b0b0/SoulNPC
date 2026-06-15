package bm.b0b0b0.SoulNPC.model;

public enum NpcDisplayType {
    AUTO,
    PLAYER,
    /** Packet-моб; вид задаётся в {@code entity-type}. */
    MOB,
    /** @deprecated используй {@link #MOB} + {@code entity-type: fox} */
    @Deprecated
    FOX,
    /** @deprecated используй {@link #MOB} + {@code entity-type: armor_stand} */
    @Deprecated
    ARMOR_STAND,
    /** @deprecated используй {@link #PLAYER} */
    @Deprecated
    MANNEQUIN;

    public static NpcDisplayType fromString(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        String normalized = value.trim().toUpperCase();
        if ("MANNEQUIN".equals(normalized)) {
            return PLAYER;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return AUTO;
        }
    }

    public boolean isPlayerModel() {
        return this == PLAYER || this == MANNEQUIN;
    }

    public boolean isLegacyMobType() {
        return this == FOX || this == ARMOR_STAND || this == MOB;
    }
}
