package bm.b0b0b0.SoulNPC.util;

import bm.b0b0b0.SoulNPC.mob.NpcMobProfile;
import bm.b0b0b0.SoulNPC.mob.NpcMobProfileRegistry;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.packet.NpcLookAtUtil;

public final class NpcInteractionAimUtil {

    private static final double PLAYER_HEIGHT = 1.8D;

    private NpcInteractionAimUtil() {
    }

    public static double aimCenterY(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return playerRaycastCenterY(data);
        }
        NpcMobProfile profile = NpcMobProfileRegistry.resolve(data);
        return data.y + profile.aimCenterY();
    }

    public static double horizontalRadius(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return 0.4D * data.appearance.resolvedScale();
        }
        return NpcMobProfileRegistry.resolve(data).horizontalRadius();
    }

    public static double verticalHalfHeight(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return playerRaycastVerticalHalf(data);
        }
        return NpcMobProfileRegistry.resolve(data).verticalHalfHeight();
    }

    public static double raycastCenterY(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return playerRaycastCenterY(data);
        }
        NpcMobProfile profile = NpcMobProfileRegistry.resolve(data);
        return profile.raycastCenterY(data.y);
    }

    public static double raycastVerticalHalf(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return playerRaycastVerticalHalf(data);
        }
        return NpcMobProfileRegistry.resolve(data).raycastVerticalHalf();
    }

    public static double eyeOrAimY(NpcFileData data) {
        if (data.appearance.isPacketMob()) {
            return aimCenterY(data);
        }
        var eyes = NpcLookAtUtil.npcEyes(data);
        return eyes == null ? data.y + 1.62F * data.appearance.resolvedScale() : eyes.getY();
    }

    private static double playerRaycastCenterY(NpcFileData data) {
        float scale = data.appearance.resolvedScale();
        NpcEntityPose pose = data.appearance.entityPose == null ? NpcEntityPose.STANDING : data.appearance.entityPose;
        return data.y + switch (pose) {
            case SITTING -> 0.55D * scale;
            case SLEEPING -> 0.25D * scale;
            case CROUCHING -> 0.75D * scale;
            case SWIMMING -> 0.45D * scale;
            default -> PLAYER_HEIGHT * 0.5D * scale;
        };
    }

    private static double playerRaycastVerticalHalf(NpcFileData data) {
        float scale = data.appearance.resolvedScale();
        NpcEntityPose pose = data.appearance.entityPose == null ? NpcEntityPose.STANDING : data.appearance.entityPose;
        return switch (pose) {
            case SITTING -> 0.55D * scale;
            case SLEEPING -> 0.28D * scale;
            case CROUCHING -> 0.75D * scale;
            case SWIMMING -> 0.45D * scale;
            default -> PLAYER_HEIGHT * 0.5D * scale;
        };
    }
}
