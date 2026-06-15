package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.model.NpcAnimationData;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcPoseData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class PacketNpcAnimator {

    private static final float IDLE_SWAY_DEGREES = 12.0F;
    private static final float BOW_PITCH_OFFSET = 45.0F;
    private static final byte HAND_MAIN_ACTIVE = 0x01;
    private static final byte HAND_OFF_ACTIVE = 0x02;

    public void play(NpcRuntime runtime) {
        NpcAnimationData animation = runtime.data().animation;
        switch (animation.type) {
            case SWING_ARM -> swing(runtime, WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM);
            case SWING_OFF_HAND -> swing(runtime, WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND);
            case WAVE -> wave(runtime);
            case GREET, MOB_POSE, NONE -> {
            }
            case BOW -> bow(runtime);
            case IDLE_SWAY -> idleSway(runtime);
            case CUSTOM -> custom(runtime);
            case HURT -> swing(runtime, WrapperPlayServerEntityAnimation.EntityAnimationType.HURT);
            case CRITICAL_HIT -> swing(runtime, WrapperPlayServerEntityAnimation.EntityAnimationType.CRITICAL_HIT);
            case MAGIC_CRITICAL_HIT -> swing(
                    runtime,
                    WrapperPlayServerEntityAnimation.EntityAnimationType.MAGIC_CRITICAL_HIT
            );
            case WAKE_UP -> swing(runtime, WrapperPlayServerEntityAnimation.EntityAnimationType.WAKE_UP);
            case SPIN_ATTACK -> togglePose(runtime, NpcEntityPose.SPIN_ATTACK);
            case FALL_FLYING -> togglePose(runtime, NpcEntityPose.FALL_FLYING);
            case CROUCH -> togglePose(runtime, NpcEntityPose.CROUCHING);
            case SLEEP -> togglePose(runtime, NpcEntityPose.SLEEPING);
            case SWIM -> togglePose(runtime, NpcEntityPose.SWIMMING);
            case USE_MAIN_HAND -> toggleHandUse(runtime, true);
            case USE_OFF_HAND -> toggleHandUse(runtime, false);
        }
    }

    public void applyBaseRotation(NpcRuntime runtime) {
        runtime.resetLookRotation();
        NpcFileData data = runtime.data();
        PacketMobNpc mob = runtime.packetMob();
        if (mob != null) {
            mob.updateRotation(data.yaw, data.pitch);
            return;
        }
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        npc.updateRotation(data.yaw, data.pitch);
    }

    public void wave(NpcRuntime runtime) {
        WrapperPlayServerEntityAnimation.EntityAnimationType type = runtime.toggleWaveHand()
                ? WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                : WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND;
        swing(runtime, type);
    }

    private void bow(NpcRuntime runtime) {
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        NpcFileData data = runtime.data();
        boolean lowered = runtime.toggleBowPhase();
        float pitch = lowered
                ? clampPitch(data.pitch + BOW_PITCH_OFFSET)
                : data.pitch;
        npc.updateRotation(data.yaw, pitch);
        if (lowered) {
            swing(runtime, WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM);
        }
    }

    private void idleSway(NpcRuntime runtime) {
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        NpcFileData data = runtime.data();
        float offset = (float) Math.sin(runtime.animationTicks() * 0.12D) * IDLE_SWAY_DEGREES;
        npc.updateRotation(data.yaw + offset, data.pitch);
    }

    private void custom(NpcRuntime runtime) {
        NpcAnimationData animation = runtime.data().animation;
        List<NpcPoseData> frames = animation.frames;
        NpcPoseData frame;
        if (frames == null || frames.isEmpty()) {
            frame = runtime.data().pose;
        } else {
            int index = runtime.customFrameIndex();
            frame = frames.get(index);
            runtime.setCustomFrameIndex((index + 1) % frames.size());
        }
        applyPose(runtime, frame);
    }

    private void applyPose(NpcRuntime runtime, NpcPoseData frame) {
        NPC npc = runtime.packetNpc();
        if (npc == null || frame == null) {
            return;
        }
        NpcFileData data = runtime.data();
        float yaw = data.yaw + frame.body.y + frame.head.y;
        float pitch = clampPitch(data.pitch + frame.head.x);
        npc.updateRotation(yaw, pitch);

        if (Math.abs(frame.rightArm.x) >= 20.0F || Math.abs(frame.rightArm.z) >= 20.0F) {
            swing(runtime, WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM);
        } else if (Math.abs(frame.leftArm.x) >= 20.0F || Math.abs(frame.leftArm.z) >= 20.0F) {
            swing(runtime, WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND);
        }
    }

    private void togglePose(NpcRuntime runtime, NpcEntityPose pose) {
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        var appearance = runtime.data().appearance;
        if (runtime.toggleAnimationPhase()) {
            PacketPlayerAppearance.applyPoseToViewers(npc, appearance, pose);
        } else {
            PacketPlayerAppearance.applyToViewers(npc, appearance);
        }
    }

    private void toggleHandUse(NpcRuntime runtime, boolean mainHand) {
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        var appearance = runtime.data().appearance;
        if (runtime.toggleAnimationPhase()) {
            byte handState = mainHand ? HAND_MAIN_ACTIVE : HAND_OFF_ACTIVE;
            PacketPlayerAppearance.applyHandStateToViewers(npc, appearance, handState);
        } else {
            PacketPlayerAppearance.applyToViewers(npc, appearance);
        }
    }

    private void swing(NpcRuntime runtime, WrapperPlayServerEntityAnimation.EntityAnimationType type) {
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(runtime.data().entityId, type);
        forEachSpawnedChannel(runtime, channel ->
                PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet)
        );
    }

    private void forEachSpawnedChannel(NpcRuntime runtime, java.util.function.Consumer<Object> consumer) {
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        for (UUID viewerId : runtime.viewers()) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player == null) {
                continue;
            }
            var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) {
                continue;
            }
            Object channel = user.getChannel();
            if (channel != null && npc.hasSpawned(channel)) {
                consumer.accept(channel);
            }
        }
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }
}
