package bm.b0b0b0.SoulNPC.util;

import bm.b0b0b0.SoulNPC.mob.NpcMobProfileRegistry;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Optional;

public final class NpcInteractionRaycast {

    private static final double DEFAULT_MAX_DISTANCE = 5.0D;

    private NpcInteractionRaycast() {
    }

    public static Optional<NpcRuntime> findTargeted(Player player, Collection<NpcRuntime> runtimes) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        NpcRuntime best = null;
        double bestDistance = Double.MAX_VALUE;

        for (NpcRuntime runtime : runtimes) {
            if (!runtime.isProfileReady() || !runtime.isVisibleTo(player.getUniqueId())) {
                continue;
            }
            NpcFileData data = runtime.data();
            if (!data.enabled || !data.interaction.enabled) {
                continue;
            }
            if (!player.getWorld().getName().equals(data.world)) {
                continue;
            }
            double maxDistance = interactReach(data);
            double alongRay = distanceAlongRay(
                    eye,
                    direction,
                    data.x,
                    NpcInteractionAimUtil.raycastCenterY(data),
                    data.z,
                    NpcInteractionAimUtil.horizontalRadius(data),
                    NpcInteractionAimUtil.raycastVerticalHalf(data),
                    maxDistance
            );
            if (alongRay < 0.0D || alongRay >= bestDistance) {
                continue;
            }
            bestDistance = alongRay;
            best = runtime;
        }
        return Optional.ofNullable(best);
    }

    static double distanceAlongRay(
            Location eye,
            Vector direction,
            double centerX,
            double centerY,
            double centerZ,
            double horizontalRadius,
            double verticalHalf,
            double maxDistance
    ) {
        double ox = eye.getX();
        double oy = eye.getY();
        double oz = eye.getZ();
        double dx = direction.getX();
        double dy = direction.getY();
        double dz = direction.getZ();

        double t = (centerX - ox) * dx + (centerY - oy) * dy + (centerZ - oz) * dz;
        if (t < 0.05D || t > maxDistance) {
            return -1.0D;
        }

        double closestX = ox + dx * t;
        double closestY = oy + dy * t;
        double closestZ = oz + dz * t;
        double horizontalDistanceSquared = square(closestX - centerX) + square(closestZ - centerZ);
        if (horizontalDistanceSquared > square(horizontalRadius)) {
            return -1.0D;
        }
        if (Math.abs(closestY - centerY) > verticalHalf) {
            return -1.0D;
        }
        return t;
    }

    private static double interactReach(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return DEFAULT_MAX_DISTANCE;
        }
        return NpcMobProfileRegistry.resolve(data).interactReach();
    }

    private static double square(double value) {
        return value * value;
    }
}
