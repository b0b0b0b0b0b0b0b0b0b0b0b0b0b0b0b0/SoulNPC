package bm.b0b0b0.SoulNPC.model;

public enum NpcDisplayType {
    AUTO,
    PLAYER,
    MOB,
    @Deprecated
    FOX,
    @Deprecated
    ARMOR_STAND,
    @Deprecated
    MANNEQUIN;

    public boolean isPlayerModel() {
        return this == PLAYER || this == MANNEQUIN;
    }
}
