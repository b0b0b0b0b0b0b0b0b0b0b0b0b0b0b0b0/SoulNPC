package bm.b0b0b0.SoulNPC.mob;

import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.entity.EntityType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class NpcMobProfileRegistry {

    private static final Map<String, NpcMobProfile> OVERRIDES = new HashMap<>();

    static {
        registerFox();
        registerWolf();
        registerBat();
        registerAllay();
        registerArmorStand();
        registerCreaking();
        registerGhast();
        registerDolphin();
        registerPiglin();
        registerPiglinBrute();
        registerPanda();
        registerEnderDragon();
        registerWither();
        registerElderGuardian();
    }

    private NpcMobProfileRegistry() {
    }

    public static NpcMobProfile resolve(NpcFileData data) {
        String entityId = NpcEntityTypeResolver.resolveEntityId(data.appearance);
        if (entityId == null) {
            return NpcMobProfile.fromDimensions(1.8F, 0.6F);
        }
        return resolveForEntityId(entityId);
    }

    public static NpcMobProfile resolveForEntityId(String entityId) {
        String key = normalizeKey(NpcEntityTypeResolver.canonicalMobId(entityId));
        NpcMobProfile override = OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        return fromBukkit(key);
    }

    public static void register(String entityId, NpcMobProfile profile) {
        OVERRIDES.put(normalizeKey(entityId), profile);
    }

    private static NpcMobProfile fromBukkit(String entityId) {
        EntityType entityType = parseBukkitType(entityId);
        float height = NpcMobDimensions.height(entityId, entityType);
        float width = NpcMobDimensions.width(entityId, entityType);
        if (entityType == null) {
            return NpcMobProfile.fromDimensions(height, width);
        }
        NpcMobProfile profile = NpcMobProfile.fromDimensions(height, width);
        if (!entityType.isAlive()) {
            return NpcMobProfile.builder()
                    .poseSupport(NpcMobPoseSupport.NONE)
                    .eyeHeight(profile.eyeHeight())
                    .aimCenterY(profile.aimCenterY())
                    .hologramBaseOffset(profile.hologramBaseOffset())
                    .horizontalRadius(profile.horizontalRadius())
                    .verticalHalfHeight(profile.verticalHalfHeight())
                    .raycastCenterY(profile.aimCenterY())
                    .raycastVerticalHalf(profile.verticalHalfHeight())
                    .supportedPoses(EnumSet.of(NpcEntityPose.STANDING))
                    .build();
        }
        return profile;
    }

    private static EntityType parseBukkitType(String entityId) {
        try {
            return EntityType.valueOf(entityId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void registerFox() {
        register("fox", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.FOX)
                .mobPoseAnimation(true)
                .eyeHeight(0.4F)
                .aimCenterY(0.35F)
                .hologramBaseOffset(1.15F)
                .horizontalRadius(0.32D)
                .verticalHalfHeight(0.35D)
                .raycastCenterY(0.55D)
                .raycastVerticalHalf(0.75D)
                .sleepingHeadLookOnly(true)
                .supportedPoses(NpcEntityPose.STANDING, NpcEntityPose.SITTING, NpcEntityPose.SLEEPING)
                .build());
    }

    private static void registerWolf() {
        register("wolf", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.TAMEABLE)
                .mobPoseAnimation(true)
                .eyeHeight(0.72F)
                .aimCenterY(0.425F)
                .hologramBaseOffset(1.1F)
                .horizontalRadius(0.35D)
                .verticalHalfHeight(0.425D)
                .raycastCenterY(0.55D)
                .raycastVerticalHalf(0.55D)
                .lookAtPitch(false)
                .supportedPoses(NpcEntityPose.STANDING, NpcEntityPose.SITTING)
                .build());
    }

    private static void registerBat() {
        register("bat", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.DISPLAY)
                .mobPoseAnimation(false)
                .eyeHeight(0.45F)
                .aimCenterY(0.25F)
                .hologramBaseOffset(0.65F)
                .horizontalRadius(0.28D)
                .verticalHalfHeight(0.25D)
                .raycastCenterY(0.25D)
                .raycastVerticalHalf(0.25D)
                .lookAtPitch(false)
                .allowedDisplayPoses(NpcMobDisplayPose.STANDING, NpcMobDisplayPose.HANGING)
                .supportedPoses(NpcEntityPose.STANDING)
                .build());
    }

    private static void registerAllay() {
        register("allay", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.DISPLAY)
                .mobPoseAnimation(false)
                .eyeHeight(0.5F)
                .aimCenterY(0.3F)
                .hologramBaseOffset(0.8F)
                .horizontalRadius(0.25D)
                .verticalHalfHeight(0.3D)
                .raycastCenterY(0.3D)
                .raycastVerticalHalf(0.3D)
                .lookAtPitch(false)
                .sleepingHeadLookOnly(true)
                .allowedDisplayPoses(NpcMobDisplayPose.STANDING, NpcMobDisplayPose.ON_BACK)
                .supportedPoses(NpcEntityPose.STANDING)
                .build());
    }

    private static void registerArmorStand() {
        register("armor_stand", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.NONE)
                .eyeHeight(1.375F)
                .aimCenterY(1.0F)
                .hologramBaseOffset(2.1F)
                .horizontalRadius(0.28D)
                .verticalHalfHeight(0.95D)
                .raycastCenterY(1.0D)
                .raycastVerticalHalf(0.95D)
                .supportedPoses(NpcEntityPose.STANDING)
                .build());
    }

    private static void registerCreaking() {
        register("creaking", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.NONE)
                .eyeHeight(2.3F)
                .aimCenterY(1.35F)
                .hologramBaseOffset(3.5F)
                .horizontalRadius(0.45D)
                .verticalHalfHeight(1.35D)
                .raycastCenterY(1.35D)
                .raycastVerticalHalf(1.35D)
                .lookAtPitch(false)
                .supportedPoses(NpcEntityPose.STANDING)
                .build());
    }

    private static void registerGhast() {
        register("ghast", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.NONE)
                .eyeHeight(3.4F)
                .aimCenterY(2.0F)
                .hologramBaseOffset(7.25F)
                .horizontalRadius(2.0D)
                .verticalHalfHeight(2.0D)
                .raycastCenterY(2.0D)
                .raycastVerticalHalf(2.0D)
                .lookAtPitch(false)
                .lookAtHeadOnly(true)
                .supportedPoses(NpcEntityPose.STANDING)
                .build());
    }

    private static void registerDolphin() {
        register("dolphin", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.NONE)
                .eyeHeight(0.45F)
                .aimCenterY(0.3F)
                .hologramBaseOffset(1.0F)
                .horizontalRadius(0.45D)
                .verticalHalfHeight(0.3D)
                .raycastCenterY(0.3D)
                .raycastVerticalHalf(0.3D)
                .lookAtPitch(false)
                .lookAtHeadOnly(true)
                .supportedPoses(NpcEntityPose.STANDING)
                .build());
    }

    private static void registerPiglin() {
        register("piglin", jitterFreeHumanoid(1.66F, 1.0F, 2.15F));
    }

    private static void registerPiglinBrute() {
        register("piglin_brute", jitterFreeHumanoid(1.9F, 1.15F, 2.4F));
    }

    private static NpcMobProfile jitterFreeHumanoid(
            float eyeHeight,
            float aimCenterY,
            float hologramOffset
    ) {
        return NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.NONE)
                .eyeHeight(eyeHeight)
                .aimCenterY(aimCenterY)
                .hologramBaseOffset(hologramOffset)
                .horizontalRadius(0.35D)
                .verticalHalfHeight(aimCenterY)
                .raycastCenterY(aimCenterY)
                .raycastVerticalHalf(aimCenterY)
                .lookAtPitch(false)
                .lookAtHeadOnly(true)
                .supportedPoses(NpcEntityPose.STANDING)
                .build();
    }

    private static void registerPanda() {
        register("panda", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.LIVING)
                .mobPoseAnimation(true)
                .eyeHeight(1.05F)
                .aimCenterY(0.625F)
                .hologramBaseOffset(1.65F)
                .horizontalRadius(0.65D)
                .verticalHalfHeight(0.625D)
                .raycastCenterY(0.625D)
                .raycastVerticalHalf(0.625D)
                .lookAtPitch(false)
                .supportedPoses(NpcEntityPose.STANDING, NpcEntityPose.SITTING)
                .build());
    }

    private static void registerEnderDragon() {
        register("ender_dragon", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.NONE)
                .eyeHeight(3.0F)
                .aimCenterY(2.5F)
                .hologramBaseOffset(5.0F)
                .horizontalRadius(5.0D)
                .verticalHalfHeight(3.0D)
                .raycastCenterY(2.5D)
                .raycastVerticalHalf(4.0D)
                .interactReach(12.0D)
                .lookAtPitch(false)
                .lookAtYawOffset(180.0F)
                .lookAtHeadOnly(false)
                .supportedPoses(NpcEntityPose.STANDING)
                .build());
    }

    private static void registerWither() {
        register("wither", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.NONE)
                .eyeHeight(2.8F)
                .aimCenterY(1.75F)
                .hologramBaseOffset(4.0F)
                .horizontalRadius(1.5D)
                .verticalHalfHeight(1.75D)
                .raycastCenterY(1.75D)
                .raycastVerticalHalf(1.75D)
                .interactReach(8.0D)
                .lookAtPitch(false)
                .supportedPoses(NpcEntityPose.STANDING)
                .build());
    }

    private static void registerElderGuardian() {
        register("elder_guardian", NpcMobProfile.builder()
                .poseSupport(NpcMobPoseSupport.NONE)
                .eyeHeight(1.7F)
                .aimCenterY(1.0F)
                .hologramBaseOffset(2.5F)
                .horizontalRadius(1.0D)
                .verticalHalfHeight(1.0D)
                .raycastCenterY(1.0D)
                .raycastVerticalHalf(1.0D)
                .lookAtPitch(false)
                .lookAtHeadOnly(true)
                .supportedPoses(NpcEntityPose.STANDING)
                .build());
    }

    private static String normalizeKey(String entityId) {
        return entityId.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
