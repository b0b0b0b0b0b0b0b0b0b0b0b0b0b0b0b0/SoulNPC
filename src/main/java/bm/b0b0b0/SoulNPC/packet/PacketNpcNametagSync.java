package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.appearance.NpcGlowColors;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.util.NpcUuids;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public final class PacketNpcNametagSync {

    private PacketNpcNametagSync() {
    }

    public static void sync(Object channel, NPC npc, NpcAppearanceData appearance) {
        if (channel == null || npc == null || appearance == null) {
            return;
        }
        removeTeam(channel, npc.getTeamName());
        npc.setPrefixName(null);
        npc.setSuffixName(null);
        sendTeam(
                channel,
                npc.getTeamName(),
                npc.getProfile().getName(),
                Component.empty(),
                Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                appearance
        );
    }

    public static void syncMob(Object channel, NpcFileData data) {
        if (channel == null || data == null || data.appearance == null) {
            return;
        }
        String teamName = teamName(data.id);
        removeTeam(channel, teamName);
        sendTeam(
                channel,
                teamName,
                NpcUuids.forNpc(data.id).toString(),
                Component.empty(),
                Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                data.appearance
        );
    }

    private static void removeTeam(Object channel, String teamName) {
        WrapperPlayServerTeams remove = new WrapperPlayServerTeams(
                teamName,
                WrapperPlayServerTeams.TeamMode.REMOVE,
                Optional.empty()
        );
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, remove);
    }

    private static void sendTeam(
            Object channel,
            String teamName,
            String memberName,
            Component prefix,
            Component suffix,
            WrapperPlayServerTeams.NameTagVisibility nameTagVisibility,
            NpcAppearanceData appearance
    ) {
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                teamName,
                WrapperPlayServerTeams.TeamMode.CREATE,
                new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                        Component.text(teamName),
                        prefix,
                        suffix,
                        nameTagVisibility,
                        collisionRule(appearance),
                        teamColor(appearance),
                        WrapperPlayServerTeams.OptionData.NONE
                ),
                memberName
        );
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet);
    }

    private static WrapperPlayServerTeams.CollisionRule collisionRule(NpcAppearanceData appearance) {
        return appearance.collidable
                ? WrapperPlayServerTeams.CollisionRule.ALWAYS
                : WrapperPlayServerTeams.CollisionRule.NEVER;
    }

    private static NamedTextColor teamColor(NpcAppearanceData appearance) {
        return NpcGlowColors.resolveColor(appearance.glowColor);
    }

    private static String teamName(String npcId) {
        return "soulnpc-" + npcId;
    }
}
