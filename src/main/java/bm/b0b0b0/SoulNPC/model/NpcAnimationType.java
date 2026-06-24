package bm.b0b0b0.SoulNPC.model;

public enum NpcAnimationType {
    NONE,
    SWING_ARM,
    SWING_OFF_HAND,
    WAVE,
    GREET,
    BOW,
    IDLE_SWAY,
    CUSTOM,
    MOB_POSE,
    HURT,
    CRITICAL_HIT,
    MAGIC_CRITICAL_HIT,
    WAKE_UP,
    SPIN_ATTACK,
    FALL_FLYING,
    USE_MAIN_HAND,
    USE_OFF_HAND,
    CROUCH,
    SLEEP,
    SWIM;

    public boolean conflictsWithLookAt() {
        return this == BOW || this == IDLE_SWAY || this == CUSTOM;
    }
}
