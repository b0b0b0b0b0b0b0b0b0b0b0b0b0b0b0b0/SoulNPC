package bm.b0b0b0.SoulNPC.util;

import bm.b0b0b0.SoulNPC.mob.NpcMobProfile;
import bm.b0b0b0.SoulNPC.mob.NpcMobProfileRegistry;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.packet.NpcLookAtUtil;

public final class NpcInteractionAimUtil {

    private NpcInteractionAimUtil() {
    }

    public static double aimCenterY(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return data.y + 1.0F * data.appearance.resolvedScale();
        }
        NpcMobProfile profile = NpcMobProfileRegistry.resolve(data);
        return data.y + profile.aimCenterY();
    }

    public static double horizontalRadius(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return 0.42D * data.appearance.resolvedScale();
        }
        return NpcMobProfileRegistry.resolve(data).horizontalRadius();
    }

    public static double verticalHalfHeight(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return 0.95D * data.appearance.resolvedScale();
        }
        return NpcMobProfileRegistry.resolve(data).verticalHalfHeight();
    }

    public static double raycastCenterY(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return aimCenterY(data);
        }
        NpcMobProfile profile = NpcMobProfileRegistry.resolve(data);
        return profile.raycastCenterY(data.y);
    }

    public static double raycastVerticalHalf(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return verticalHalfHeight(data);
        }
        return NpcMobProfileRegistry.resolve(data).raycastVerticalHalf();
    }

    public static double eyeOrAimY(NpcFileData data) {
        if (data.appearance.isPacketMob()) {
            return aimCenterY(data);
        }
        var eyes = NpcLookAtUtil.npcEyes(data);
        return eyes == null ? data.y + 1.0F : eyes.getY();
    }
}
