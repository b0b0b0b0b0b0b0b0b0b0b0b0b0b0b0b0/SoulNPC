package bm.b0b0b0.SoulNPC.util;

import bm.b0b0b0.SoulNPC.config.settings.SoulNpcSettings;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.entity.Player;

public final class NpcViewDistance {

    private NpcViewDistance() {
    }

    public static int packetBlocks(SoulNpcSettings.Performance performance) {
        return Math.max(1, performance.packetViewDistance);
    }

    public static int packetBlocks(SoulNpcSettings.Performance performance, NpcFileData data) {
        if (data != null && data.packetViewDistance > 0) {
            return Math.max(1, data.packetViewDistance);
        }
        return packetBlocks(performance);
    }

    public static int hologramBlocks(SoulNpcSettings.Performance performance) {
        if (performance.hologramViewDistance > 0) {
            return Math.max(1, performance.hologramViewDistance);
        }
        return packetBlocks(performance);
    }

    public static int hologramBlocks(SoulNpcSettings.Performance performance, NpcFileData data) {
        if (data != null && data.hologramViewDistance > 0) {
            return Math.max(1, data.hologramViewDistance);
        }
        if (data != null && data.packetViewDistance > 0) {
            return Math.max(1, data.packetViewDistance);
        }
        return hologramBlocks(performance);
    }

    public static boolean canSee(Player player, NpcFileData data) {
        if (data == null) {
            return true;
        }
        if (data.visibilityPermission == null || data.visibilityPermission.isBlank()) {
            return true;
        }
        return player.hasPermission(data.visibilityPermission);
    }

    public static boolean isWithin(Player player, NpcFileData data, int viewDistanceBlocks) {
        if (!data.enabled) {
            return false;
        }
        if (!canSee(player, data)) {
            return false;
        }
        if (!player.getWorld().getName().equals(data.world)) {
            return false;
        }
        double max = (double) viewDistanceBlocks * viewDistanceBlocks;
        double dx = data.x - player.getLocation().getX();
        double dy = data.y - player.getLocation().getY();
        double dz = data.z - player.getLocation().getZ();
        return (dx * dx) + (dy * dy) + (dz * dz) <= max;
    }
}
