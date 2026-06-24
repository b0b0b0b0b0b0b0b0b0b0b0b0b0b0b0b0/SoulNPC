package bm.b0b0b0.SoulNPC.model;

import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum NpcEntityPose {
    STANDING,
    SITTING,
    SLEEPING,
    CROUCHING,
    SWIMMING,
    SPIN_ATTACK,
    FALL_FLYING;

    private static final Set<NpcEntityPose> PLAYER_MOB_POSES = EnumSet.of(
            STANDING,
            CROUCHING,
            SLEEPING,
            SWIMMING,
            SPIN_ATTACK,
            FALL_FLYING
    );

    private static final List<NpcEntityPose> PLAYER_GUI_POSES = List.of(
            STANDING,
            CROUCHING,
            SITTING,
            SLEEPING,
            SWIMMING
    );

    public static NpcEntityPose fromString(String value) {
        if (value == null || value.isBlank()) {
            return STANDING;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "SNEAKING", "SNEAK", "CROUCH" -> CROUCHING;
            case "CRAWLING", "CRAWL" -> SWIMMING;
            case "LYING" -> SLEEPING;
            case "TRIDENT", "TRIDENT_RIPTIDE", "RIPTIDE" -> SPIN_ATTACK;
            case "ELYTRA", "GLIDE" -> FALL_FLYING;
            default -> {
                try {
                    yield valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield STANDING;
                }
            }
        };
    }

    public EntityPose toPacketPose() {
        return switch (this) {
            case STANDING -> EntityPose.STANDING;
            case SITTING -> EntityPose.SITTING;
            case SLEEPING -> EntityPose.SLEEPING;
            case CROUCHING -> EntityPose.CROUCHING;
            case SWIMMING -> EntityPose.SWIMMING;
            case SPIN_ATTACK -> EntityPose.SPIN_ATTACK;
            case FALL_FLYING -> EntityPose.FALL_FLYING;
        };
    }

    public static Set<NpcEntityPose> playerMobPoses() {
        return PLAYER_MOB_POSES;
    }

    public static NpcEntityPose nextPlayerGuiPose(NpcEntityPose current, boolean reverse) {
        List<NpcEntityPose> poses = PLAYER_GUI_POSES;
        int index = poses.indexOf(current);
        if (index < 0) {
            return reverse ? poses.get(poses.size() - 1) : poses.get(0);
        }
        int next = reverse
                ? (index - 1 + poses.size()) % poses.size()
                : (index + 1) % poses.size();
        return poses.get(next);
    }
}
