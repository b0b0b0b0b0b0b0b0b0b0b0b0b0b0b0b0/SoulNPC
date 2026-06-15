package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PacketPlayerAppearance {

    private static final int FLAGS_INDEX = 0;
    private static final int NO_GRAVITY_INDEX = 5;
    private static final int POSE_INDEX = 6;
    private static final int HAND_STATES_INDEX = 8;

    private static final byte FLAG_GLOWING = 0x40;
    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte HAND_MAIN_ACTIVE = 0x01;

    private PacketPlayerAppearance() {
    }

    public static void apply(Object channel, int entityId, NpcAppearanceData appearance) {
        if (channel == null || appearance == null || !appearance.type.isPlayerModel()) {
            return;
        }
        sendMetadata(channel, entityId, appearance, null, null);
        sendScale(channel, entityId, appearance);
    }

    public static void applyToViewers(NPC npc, NpcAppearanceData appearance) {
        applyPoseToViewers(npc, appearance, null);
    }

    public static void applyPoseToViewers(NPC npc, NpcAppearanceData appearance, NpcEntityPose poseOverride) {
        if (npc == null || appearance == null) {
            return;
        }
        for (Object channel : npc.getChannels()) {
            applyPose(channel, npc.getId(), appearance, poseOverride);
        }
    }

    public static void applyHandStateToViewers(NPC npc, NpcAppearanceData appearance, byte handState) {
        if (npc == null || appearance == null) {
            return;
        }
        for (Object channel : npc.getChannels()) {
            applyHandState(channel, npc.getId(), appearance, handState);
        }
    }

    public static void applyPose(Object channel, int entityId, NpcAppearanceData appearance, NpcEntityPose poseOverride) {
        if (channel == null || appearance == null || !appearance.type.isPlayerModel()) {
            return;
        }
        sendMetadata(channel, entityId, appearance, poseOverride, null);
    }

    public static void applyHandState(Object channel, int entityId, NpcAppearanceData appearance, byte handState) {
        if (channel == null || appearance == null || !appearance.type.isPlayerModel()) {
            return;
        }
        sendMetadata(channel, entityId, appearance, null, handState);
    }

    static List<EntityData<?>> buildMetadata(NpcAppearanceData appearance) {
        return buildMetadata(appearance, null, null);
    }

    static List<EntityData<?>> buildMetadata(NpcAppearanceData appearance, NpcEntityPose poseOverride) {
        return buildMetadata(appearance, poseOverride, null);
    }

    static List<EntityData<?>> buildMetadata(
            NpcAppearanceData appearance,
            NpcEntityPose poseOverride,
            Byte handState
    ) {
        if (!appearance.type.isPlayerModel()) {
            return List.of();
        }
        List<EntityData<?>> metadata = new ArrayList<>(4);
        byte flags = entityFlags(appearance);
        if (flags != 0) {
            metadata.add(new EntityData<>(FLAGS_INDEX, EntityDataTypes.BYTE, flags));
        }
        if (appearance.noGravity) {
            metadata.add(new EntityData<>(NO_GRAVITY_INDEX, EntityDataTypes.BOOLEAN, true));
        }
        NpcEntityPose pose = resolvePose(appearance, poseOverride);
        metadata.add(new EntityData<>(
                POSE_INDEX,
                EntityDataTypes.ENTITY_POSE,
                pose.toPacketPose()
        ));
        Byte effectiveHandState = resolveHandState(pose, handState);
        if (effectiveHandState != null) {
            metadata.add(new EntityData<>(HAND_STATES_INDEX, EntityDataTypes.BYTE, effectiveHandState));
        }
        return metadata;
    }

    private static Byte resolveHandState(NpcEntityPose pose, Byte handState) {
        if (handState != null) {
            return handState;
        }
        if (pose == NpcEntityPose.SPIN_ATTACK) {
            return HAND_MAIN_ACTIVE;
        }
        return null;
    }

    private static NpcEntityPose resolvePose(NpcAppearanceData appearance, NpcEntityPose poseOverride) {
        if (poseOverride != null) {
            return poseOverride;
        }
        return resolvedPose(appearance);
    }

    private static NpcEntityPose resolvedPose(NpcAppearanceData appearance) {
        if (appearance.entityPose == NpcEntityPose.SITTING && appearance.type.isPlayerModel()) {
            return NpcEntityPose.STANDING;
        }
        return appearance.entityPose;
    }

    public static boolean needsSeat(NpcAppearanceData appearance) {
        return appearance != null
                && appearance.type.isPlayerModel()
                && appearance.entityPose == NpcEntityPose.SITTING;
    }

    private static byte entityFlags(NpcAppearanceData appearance) {
        byte flags = 0;
        if (appearance.glow) {
            flags |= FLAG_GLOWING;
        }
        if (appearance.invisible) {
            flags |= FLAG_INVISIBLE;
        }
        return flags;
    }

    private static void sendMetadata(
            Object channel,
            int entityId,
            NpcAppearanceData appearance,
            NpcEntityPose poseOverride,
            Byte handState
    ) {
        List<EntityData<?>> metadata = buildMetadata(appearance, poseOverride, handState);
        if (metadata.isEmpty()) {
            return;
        }
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet);
    }

    private static void sendScale(Object channel, int entityId, NpcAppearanceData appearance) {
        float scale = appearance.resolvedScale();
        if (Math.abs(scale - 1.0F) < 0.001F) {
            return;
        }
        WrapperPlayServerUpdateAttributes.Property property = new WrapperPlayServerUpdateAttributes.Property(
                Attributes.SCALE,
                scale,
                Collections.emptyList()
        );
        WrapperPlayServerUpdateAttributes packet = new WrapperPlayServerUpdateAttributes(
                entityId,
                List.of(property)
        );
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet);
    }
}
