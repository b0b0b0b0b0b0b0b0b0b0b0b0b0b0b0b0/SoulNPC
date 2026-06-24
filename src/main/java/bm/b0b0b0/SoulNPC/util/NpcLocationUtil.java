package bm.b0b0b0.SoulNPC.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class NpcLocationUtil {

    private NpcLocationUtil() {
    }

    public static Location createAtPlayer(Player player) {
        Location source = player.getLocation();
        Location spawn = source.clone();
        spawn.setYaw(source.getYaw() + 180.0F);
        spawn.setPitch(0.0F);
        return spawn;
    }
}
