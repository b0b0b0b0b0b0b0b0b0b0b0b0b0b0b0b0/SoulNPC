package bm.b0b0b0.SoulNPC.model;

public enum NpcClickBinding {
    LEFT,
    RIGHT,
    MIDDLE,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    ANY;

    public boolean matches(NpcClickType clickType) {
        if (this == ANY) {
            return true;
        }
        return name().equals(clickType.name());
    }

    public static NpcClickBinding fromClickType(NpcClickType clickType) {
        return valueOf(clickType.name());
    }
}
