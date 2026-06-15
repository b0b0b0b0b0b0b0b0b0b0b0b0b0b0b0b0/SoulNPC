package bm.b0b0b0.SoulNPC.model;

import java.util.Locale;

public enum NpcAnimationType {
    NONE,
    SWING_ARM,
    SWING_OFF_HAND,
    WAVE,
    GREET,
    BOW,
    IDLE_SWAY,
    CUSTOM,
    /** Цикл поз моба (лиса: STANDING, SITTING, SLEEPING) или player NPC. */
    MOB_POSE,
    /** Пакет: красный экран урона. */
    HURT,
    /** Пакет: критический удар. */
    CRITICAL_HIT,
    /** Пакет: зачарованный критический удар. */
    MAGIC_CRITICAL_HIT,
    /** Пакет: вставание с кровати. */
    WAKE_UP,
    /** Поза: riptide / кручение трезубца (main-hand: TRIDENT). */
    SPIN_ATTACK,
    /** Поза: полёт на элитрах. */
    FALL_FLYING,
    /** Метаданные: поднятая main hand (лук, трезубец, еда). */
    USE_MAIN_HAND,
    /** Метаданные: поднятая off hand (щит, факел). */
    USE_OFF_HAND,
    /** Поза: присед. */
    CROUCH,
    /** Поза: сон. */
    SLEEP,
    /** Поза: плавание / ползание. */
    SWIM;

    public boolean conflictsWithLookAt() {
        return this == BOW || this == IDLE_SWAY || this == CUSTOM;
    }

    public static NpcAnimationType fromString(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TRIDENT", "TRIDENT_RIPTIDE", "RIPTIDE" -> SPIN_ATTACK;
            case "TRIDENT_CHARGE", "TRIDENT_USE", "BOW_CHARGE", "EAT", "DRINK", "USE_ITEM" -> USE_MAIN_HAND;
            case "ENCHANTED_HIT", "CRITICAL_HIT_MAGIC" -> MAGIC_CRITICAL_HIT;
            case "ELYTRA", "GLIDE" -> FALL_FLYING;
            case "SNEAK", "SNEAKING", "CROUCHING" -> CROUCH;
            case "SLEEPING", "LYING" -> SLEEP;
            case "SWIMMING", "CRAWL", "CRAWLING" -> SWIM;
            default -> {
                try {
                    yield valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield NONE;
                }
            }
        };
    }
}
