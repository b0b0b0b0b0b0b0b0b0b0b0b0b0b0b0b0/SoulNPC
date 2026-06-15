package bm.b0b0b0.SoulNPC.mob;

import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;

import java.util.EnumSet;
import java.util.Set;

public final class NpcMobProfile {

    private final NpcMobPoseSupport poseSupport;
    private final float eyeHeight;
    private final float aimCenterY;
    private final float hologramBaseOffset;
    private final double horizontalRadius;
    private final double verticalHalfHeight;
    private final double raycastCenterY;
    private final double raycastVerticalHalf;
    private final double interactReach;
    private final boolean lookAtPitch;
    private final boolean lookAtHeadOnly;
    private final float lookAtYawOffset;
    private final boolean sleepingHeadLookOnly;
    private final boolean mobPoseAnimation;
    private final Set<NpcEntityPose> supportedPoses;
    private final Set<NpcMobDisplayPose> allowedDisplayPoses;

    private NpcMobProfile(Builder builder) {
        this.poseSupport = builder.poseSupport;
        this.eyeHeight = builder.eyeHeight;
        this.aimCenterY = builder.aimCenterY;
        this.hologramBaseOffset = builder.hologramBaseOffset;
        this.horizontalRadius = builder.horizontalRadius;
        this.verticalHalfHeight = builder.verticalHalfHeight;
        this.raycastCenterY = builder.raycastCenterY;
        this.raycastVerticalHalf = builder.raycastVerticalHalf;
        this.interactReach = builder.interactReach;
        this.lookAtPitch = builder.lookAtPitch;
        this.lookAtHeadOnly = builder.lookAtHeadOnly;
        this.lookAtYawOffset = builder.lookAtYawOffset;
        this.sleepingHeadLookOnly = builder.sleepingHeadLookOnly;
        this.mobPoseAnimation = builder.mobPoseAnimation;
        this.supportedPoses = Set.copyOf(builder.supportedPoses);
        this.allowedDisplayPoses = Set.copyOf(builder.allowedDisplayPoses);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static NpcMobProfile fromDimensions(float height, float width) {
        float halfHeight = height * 0.5F;
        return builder()
                .poseSupport(NpcMobPoseSupport.NONE)
                .eyeHeight(Math.max(0.2F, height * 0.85F))
                .aimCenterY(halfHeight)
                .hologramBaseOffset(height + 0.25F)
                .horizontalRadius(Math.max(0.2D, width * 0.5D))
                .verticalHalfHeight(Math.max(0.2D, halfHeight))
                .raycastCenterY(halfHeight)
                .raycastVerticalHalf(Math.max(0.2D, halfHeight))
                .interactReach(5.0D)
                .lookAtPitch(false)
                .sleepingHeadLookOnly(false)
                .mobPoseAnimation(false)
                .allowedDisplayPoses(NpcMobDisplayPose.STANDING)
                .supportedPoses(EnumSet.of(NpcEntityPose.STANDING))
                .build();
    }

    public NpcMobPoseSupport poseSupport() {
        return poseSupport;
    }

    public float eyeHeight() {
        return eyeHeight;
    }

    public float aimCenterY() {
        return aimCenterY;
    }

    public float hologramBaseOffset() {
        return hologramBaseOffset;
    }

    public double horizontalRadius() {
        return horizontalRadius;
    }

    public double verticalHalfHeight() {
        return verticalHalfHeight;
    }

    public double raycastCenterY(double baseY) {
        return baseY + raycastCenterY;
    }

    public double raycastVerticalHalf() {
        return raycastVerticalHalf;
    }

    public double interactReach() {
        return interactReach;
    }

    public boolean lookAtPitch() {
        return lookAtPitch;
    }

    public boolean lookAtHeadOnly() {
        return lookAtHeadOnly;
    }

    public float lookAtYawOffset() {
        return lookAtYawOffset;
    }

    public boolean sleepingHeadLookOnly() {
        return sleepingHeadLookOnly;
    }

    /** Цикл MOB_POSE в анимации — только у мобов с явным профилем (лиса, волк, …). */
    public boolean mobPoseAnimation() {
        return mobPoseAnimation;
    }

    public Set<NpcEntityPose> supportedPoses() {
        return supportedPoses;
    }

    public boolean supportsPose(NpcEntityPose pose) {
        return supportedPoses.contains(pose);
    }

    public Set<NpcMobDisplayPose> allowedDisplayPoses() {
        return allowedDisplayPoses;
    }

    public boolean supportsDisplayPose(NpcMobDisplayPose pose) {
        return allowedDisplayPoses.contains(pose);
    }

    public NpcMobDisplayPose resolveDisplayPose(NpcMobDisplayPose requested) {
        if (requested != null && supportsDisplayPose(requested)) {
            return requested;
        }
        return NpcMobDisplayPose.STANDING;
    }

    public static final class Builder {
        private NpcMobPoseSupport poseSupport = NpcMobPoseSupport.LIVING;
        private float eyeHeight = 1.0F;
        private float aimCenterY = 0.5F;
        private float hologramBaseOffset = 2.0F;
        private double horizontalRadius = 0.4D;
        private double verticalHalfHeight = 0.9D;
        private double raycastCenterY = 0.5D;
        private double raycastVerticalHalf = 0.9D;
        private double interactReach = 5.0D;
        private boolean lookAtPitch;
        private boolean lookAtHeadOnly;
        private float lookAtYawOffset;
        private boolean sleepingHeadLookOnly;
        private boolean mobPoseAnimation;
        private Set<NpcEntityPose> supportedPoses = EnumSet.of(NpcEntityPose.STANDING);
        private Set<NpcMobDisplayPose> allowedDisplayPoses = EnumSet.of(NpcMobDisplayPose.STANDING);

        public Builder poseSupport(NpcMobPoseSupport poseSupport) {
            this.poseSupport = poseSupport;
            return this;
        }

        public Builder eyeHeight(float eyeHeight) {
            this.eyeHeight = eyeHeight;
            return this;
        }

        public Builder aimCenterY(float aimCenterY) {
            this.aimCenterY = aimCenterY;
            return this;
        }

        public Builder hologramBaseOffset(float hologramBaseOffset) {
            this.hologramBaseOffset = hologramBaseOffset;
            return this;
        }

        public Builder horizontalRadius(double horizontalRadius) {
            this.horizontalRadius = horizontalRadius;
            return this;
        }

        public Builder verticalHalfHeight(double verticalHalfHeight) {
            this.verticalHalfHeight = verticalHalfHeight;
            return this;
        }

        public Builder raycastCenterY(double raycastCenterY) {
            this.raycastCenterY = raycastCenterY;
            return this;
        }

        public Builder raycastVerticalHalf(double raycastVerticalHalf) {
            this.raycastVerticalHalf = raycastVerticalHalf;
            return this;
        }

        public Builder interactReach(double interactReach) {
            this.interactReach = interactReach;
            return this;
        }

        public Builder lookAtPitch(boolean lookAtPitch) {
            this.lookAtPitch = lookAtPitch;
            return this;
        }

        public Builder lookAtHeadOnly(boolean lookAtHeadOnly) {
            this.lookAtHeadOnly = lookAtHeadOnly;
            return this;
        }

        public Builder lookAtYawOffset(float lookAtYawOffset) {
            this.lookAtYawOffset = lookAtYawOffset;
            return this;
        }

        public Builder sleepingHeadLookOnly(boolean sleepingHeadLookOnly) {
            this.sleepingHeadLookOnly = sleepingHeadLookOnly;
            return this;
        }

        public Builder mobPoseAnimation(boolean mobPoseAnimation) {
            this.mobPoseAnimation = mobPoseAnimation;
            return this;
        }

        public Builder allowedDisplayPoses(NpcMobDisplayPose... poses) {
            this.allowedDisplayPoses = EnumSet.noneOf(NpcMobDisplayPose.class);
            for (NpcMobDisplayPose pose : poses) {
                this.allowedDisplayPoses.add(pose);
            }
            return this;
        }

        public Builder supportedPoses(Set<NpcEntityPose> supportedPoses) {
            this.supportedPoses = EnumSet.copyOf(supportedPoses);
            return this;
        }

        public Builder supportedPoses(NpcEntityPose... poses) {
            this.supportedPoses = EnumSet.noneOf(NpcEntityPose.class);
            for (NpcEntityPose pose : poses) {
                this.supportedPoses.add(pose);
            }
            return this;
        }

        public NpcMobProfile build() {
            return new NpcMobProfile(this);
        }
    }
}
