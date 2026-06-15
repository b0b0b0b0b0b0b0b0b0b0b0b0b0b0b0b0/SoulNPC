package bm.b0b0b0.SoulNPC.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;

public final class PacketNpcNametagHider {

    private PacketNpcNametagHider() {
    }

    public static void hide(Object channel, NPC npc) {
        if (channel == null || npc == null) {
            return;
        }
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                npc.getTeamName(),
                WrapperPlayServerTeams.TeamMode.CREATE,
                new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                        Component.empty(),
                        Component.empty(),
                        Component.empty(),
                        WrapperPlayServerTeams.NameTagVisibility.NEVER,
                        WrapperPlayServerTeams.CollisionRule.NEVER,
                        null,
                        WrapperPlayServerTeams.OptionData.NONE
                ),
                npc.getProfile().getName()
        );
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet);
    }
}
