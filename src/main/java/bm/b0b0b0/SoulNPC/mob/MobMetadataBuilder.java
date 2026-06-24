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

    private static final int SHEEP_COLOR_INDEX = 16;
    private static final int CREEPER_CHARGED_INDEX = 17;
    private static final int VILLAGER_PROFESSION_INDEX = 18;
    private static final int WOLF_COLLAR_INDEX = 20;
    private static final int BABY_INDEX = 16;

    private MobMetadataBuilder() {
    }

    public static List<EntityData<?>> build(NpcAppearanceData appearance, NpcMobProfile profile) {
        List<EntityData<?>> metadata = new ArrayList<>(4);
        byte flags = entityFlags(appearance);
        metadata.add(new EntityData<>(FLAGS_INDEX, EntityDataTypes.BYTE, flags));
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
        applyMobProperties(metadata, appearance);
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

    private static void applyMobProperties(List<EntityData<?>> metadata, NpcAppearanceData appearance) {
        if (appearance.mobProperties == null || appearance.mobProperties.isEmpty()) {
            return;
        }
        String entityId = appearance.resolvedEntityType();
        if (entityId == null) {
            return;
        }
        String baby = appearance.mobProperties.get("baby");
        if (baby != null) {
            metadata.add(new EntityData<>(BABY_INDEX, EntityDataTypes.BOOLEAN, Boolean.parseBoolean(baby)));
        }
        if ("fox".equals(entityId) || entityId.startsWith("fox")) {
            String variant = appearance.mobProperties.get("fox_variant");
            if (variant != null) {
                metadata.add(new EntityData<>(
                        FOX_VARIANT_INDEX,
                        EntityDataTypes.INT,
                        parseFoxVariantId(variant)
                ));
            }
        }
        if ("sheep".equals(entityId)) {
            String color = appearance.mobProperties.get("sheep_color");
            if (color != null) {
                metadata.add(new EntityData<>(SHEEP_COLOR_INDEX, EntityDataTypes.BYTE, (byte) parseDyeColor(color)));
            }
        }
        if ("creeper".equals(entityId)) {
            String charged = appearance.mobProperties.get("creeper_charged");
            if (charged != null) {
                metadata.add(new EntityData<>(CREEPER_CHARGED_INDEX, EntityDataTypes.BOOLEAN, Boolean.parseBoolean(charged)));
            }
        }
        if ("villager".equals(entityId)) {
            String profession = appearance.mobProperties.get("villager_profession");
            if (profession != null) {
                metadata.add(new EntityData<>(VILLAGER_PROFESSION_INDEX, EntityDataTypes.INT, parseProfession(profession)));
            }
        }
        if ("wolf".equals(entityId)) {
            String collar = appearance.mobProperties.get("wolf_collar");
            if (collar != null) {
                metadata.add(new EntityData<>(WOLF_COLLAR_INDEX, EntityDataTypes.BYTE, (byte) parseDyeColor(collar)));
            }
        }
    }

    private static int parseFoxVariantId(String raw) {
        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if ("SNOW".equals(normalized) || "1".equals(normalized)) {
            return 1;
        }
        return 0;
    }

    private static int parseProfession(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "farmer" -> 1;
                case "librarian" -> 2;
                case "priest", "cleric" -> 3;
                case "blacksmith", "weaponsmith" -> 4;
                case "butcher" -> 5;
                default -> 0;
            };
        }
    }

    private static int parseDyeColor(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "white" -> 0;
                case "orange" -> 1;
                case "magenta" -> 2;
                case "light_blue" -> 3;
                case "yellow" -> 4;
                case "lime" -> 5;
                case "pink" -> 6;
                case "gray", "grey" -> 7;
                case "light_gray", "light_grey", "silver" -> 8;
                case "cyan" -> 9;
                case "purple" -> 10;
                case "blue" -> 11;
                case "brown" -> 12;
                case "green" -> 13;
                case "red" -> 14;
                case "black" -> 15;
                default -> 0;
            };
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
