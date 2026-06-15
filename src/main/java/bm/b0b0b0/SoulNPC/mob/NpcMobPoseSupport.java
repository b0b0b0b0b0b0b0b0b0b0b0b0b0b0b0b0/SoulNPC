package bm.b0b0b0.SoulNPC.mob;

public enum NpcMobPoseSupport {
    /** Позы через metadata не отправляются. */
    NONE,
    /** EntityPose на индексе 6 — только где реально работает (осторожно со SLEEPING). */
    LIVING,
    /** Лиса: variant + sitting/sleeping flags. */
    FOX,
    /** Волк, кошка, попугай: sitting/tamed на индексе 18 (Tameable Animal). */
    TAMEABLE,
    /** Статичная поза из mob-display-pose (тихоня, летучая мышь). */
    DISPLAY
}
