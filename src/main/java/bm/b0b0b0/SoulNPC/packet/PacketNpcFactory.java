package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class PacketNpcFactory {

    private PacketNpcFactory() {
    }

    public static NPC create(NpcFileData data, UserProfile profile) {
        World world = Bukkit.getWorld(data.world);
        if (world == null) {
            throw new IllegalStateException("World not found: " + data.world);
        }
        NPC npc = new NPC(profile, data.entityId, Component.empty());
        npc.setLocation(toPacketLocation(data));
        npc.setTeamName("soulnpc-" + data.id);
        npc.setPrefixName(null);
        npc.setSuffixName(null);
        return npc;
    }

    public static Location toPacketLocation(NpcFileData data) {
        return new Location(
                new Vector3d(data.x, data.y, data.z),
                data.yaw,
                data.pitch
        );
    }
}
