package bm.b0b0b0.SoulNPC.model;

public enum NpcFoxVariant {
    RED,
    SNOW;

    public int packetId() {
        return ordinal();
    }
}
