package bm.b0b0b0.SoulNPC.mob;

import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;

import java.util.ArrayList;
import java.util.List;

public final class MobMetadataBuilder {

    private static final int FLAGS_INDEX = 0;
    private static final int NO_GRAVITY_INDEX = 5;
    private static final int POSE_INDEX = 6;
    private static final int FOX_VARIANT_INDEX = 18;
    private static final int FOX_FLAGS_INDEX = 19;
    private static final int TAMEABLE_FLAGS_INDEX = 18;
    private static final int BAT_FLAGS_INDEX = 16;
    private static final int GUARDIAN_ATTACKING_INDEX = 16;

    private static final byte FLAG_GLOWING = 0x40;
    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte FOX_FLAG_SITTING = 0x01;
    private static final byte FOX_FLAG_SLEEPING = 0x20;
    private static final byte TAMEABLE_FLAG_SITTING = 0x01;
    private static final byte TAMEABLE_FLAG_TAMED = 0x04;
    private static final byte BAT_FLAG_HANGING = 0x01;

    private MobMetadataBuilder() {
    }

    public static List<EntityData<?>> build(NpcAppearanceData appearance, NpcMobProfile profile) {
        List<EntityData<?>> metadata = new ArrayList<>(4);
        byte flags = entityFlags(appearance);
        if (flags != 0) {
            metadata.add(new EntityData<>(FLAGS_INDEX, EntityDataTypes.BYTE, flags));
        }
        if (appearance.noGravity) {
            metadata.add(new EntityData<>(NO_GRAVITY_INDEX, EntityDataTypes.BOOLEAN, true));
        }
        switch (profile.poseSupport()) {
            case FOX -> addFoxPose(metadata, appearance);
            case TAMEABLE -> addTameablePose(metadata, appearance, profile);
            case LIVING -> addLivingPose(metadata, appearance, profile);
            case DISPLAY -> addDisplayPose(metadata, appearance, profile);
            case NONE -> {
            }
        }
        addEntityExtras(metadata, appearance);
        return metadata;
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

    private static void addFoxPose(List<EntityData<?>> metadata, NpcAppearanceData appearance) {
        metadata.add(new EntityData<>(
                FOX_VARIANT_INDEX,
                EntityDataTypes.INT,
                NpcEntityTypeResolver.resolveFoxVariant(appearance).packetId()
        ));
        metadata.add(new EntityData<>(FOX_FLAGS_INDEX, EntityDataTypes.BYTE, foxFlags(appearance.entityPose)));
    }

    private static void addTameablePose(
            List<EntityData<?>> metadata,
            NpcAppearanceData appearance,
            NpcMobProfile profile
    ) {
        NpcEntityPose pose = profile.supportsPose(appearance.entityPose)
                ? appearance.entityPose
                : NpcEntityPose.STANDING;
        if (pose == NpcEntityPose.SITTING) {
            metadata.add(new EntityData<>(
                    TAMEABLE_FLAGS_INDEX,
                    EntityDataTypes.BYTE,
                    (byte) (TAMEABLE_FLAG_SITTING | TAMEABLE_FLAG_TAMED)
            ));
        } else {
            metadata.add(new EntityData<>(TAMEABLE_FLAGS_INDEX, EntityDataTypes.BYTE, (byte) 0));
        }
        metadata.add(new EntityData<>(POSE_INDEX, EntityDataTypes.ENTITY_POSE, NpcEntityPose.STANDING.toPacketPose()));
    }

    private static void addLivingPose(
            List<EntityData<?>> metadata,
            NpcAppearanceData appearance,
            NpcMobProfile profile
    ) {
        NpcEntityPose pose = profile.supportsPose(appearance.entityPose)
                ? appearance.entityPose
                : NpcEntityPose.STANDING;
        metadata.add(new EntityData<>(POSE_INDEX, EntityDataTypes.ENTITY_POSE, pose.toPacketPose()));
    }

    private static void addDisplayPose(
            List<EntityData<?>> metadata,
            NpcAppearanceData appearance,
            NpcMobProfile profile
    ) {
        NpcMobDisplayPose displayPose = profile.resolveDisplayPose(appearance.mobDisplayPose);
        String entityId = appearance.resolvedEntityType();
        if ("bat".equals(entityId)) {
            addBatDisplay(metadata, displayPose);
            return;
        }
        addEntityPoseDisplay(metadata, displayPose);
    }

    private static void addEntityPoseDisplay(List<EntityData<?>> metadata, NpcMobDisplayPose displayPose) {
        NpcEntityPose pose = switch (displayPose) {
            case ON_BACK -> NpcEntityPose.SLEEPING;
            case STANDING, HANGING -> NpcEntityPose.STANDING;
        };
        metadata.add(new EntityData<>(POSE_INDEX, EntityDataTypes.ENTITY_POSE, pose.toPacketPose()));
    }

    private static void addBatDisplay(List<EntityData<?>> metadata, NpcMobDisplayPose displayPose) {
        byte flags = displayPose == NpcMobDisplayPose.HANGING ? BAT_FLAG_HANGING : 0;
        metadata.add(new EntityData<>(BAT_FLAGS_INDEX, EntityDataTypes.BYTE, flags));
    }

    private static void addEntityExtras(List<EntityData<?>> metadata, NpcAppearanceData appearance) {
        String entityId = appearance.resolvedEntityType();
        if (entityId == null) {
            return;
        }
        switch (entityId) {
            case "guardian", "elder_guardian" -> metadata.add(new EntityData<>(
                    GUARDIAN_ATTACKING_INDEX,
                    EntityDataTypes.BOOLEAN,
                    false
            ));
            default -> {
            }
        }
    }

    private static byte foxFlags(NpcEntityPose pose) {
        return switch (pose) {
            case SITTING -> FOX_FLAG_SITTING;
            case SLEEPING -> FOX_FLAG_SLEEPING;
            case STANDING, CROUCHING, SWIMMING, SPIN_ATTACK, FALL_FLYING -> 0;
        };
    }
}
