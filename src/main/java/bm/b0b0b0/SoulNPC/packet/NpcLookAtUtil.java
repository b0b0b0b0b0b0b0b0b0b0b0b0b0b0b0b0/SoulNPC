package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.mob.NpcMobProfileRegistry;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class NpcLookAtUtil {

    private static final float PLAYER_EYE_HEIGHT = 1.62F;

    private NpcLookAtUtil() {
    }

    static float eyeHeight(NpcFileData data) {
        if (data.appearance.isPacketMob()) {
            return NpcMobProfileRegistry.resolve(data).eyeHeight();
        }
        return PLAYER_EYE_HEIGHT * data.appearance.resolvedScale();
    }

    static float[] targetRotation(NpcFileData data, Player player) {
        Location eye = npcEyes(data);
        if (eye == null) {
            return new float[]{data.yaw, data.pitch};
        }
        float[] rot = rotationTo(eye, player.getEyeLocation());
        if (data.appearance.isPacketMob()) {
            rot[0] = wrapDegrees(rot[0] + NpcMobProfileRegistry.resolve(data).lookAtYawOffset());
        }
        return rot;
    }

    public static Location npcEyes(NpcFileData data) {
        if (Bukkit.getWorld(data.world) == null) {
            return null;
        }
        return new Location(
                Bukkit.getWorld(data.world),
                data.x,
                data.y + eyeHeight(data),
                data.z
        );
    }

    static float[] rotationTo(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontal));
        return new float[]{yaw, clampPitch(pitch)};
    }

    public static float lerp(float from, float to, float factor) {
        return from + (to - from) * factor;
    }

    public static float lerpAngle(float from, float to, float factor) {
        float delta = wrapDegrees(to - from);
        return from + delta * factor;
    }

    static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }
}
