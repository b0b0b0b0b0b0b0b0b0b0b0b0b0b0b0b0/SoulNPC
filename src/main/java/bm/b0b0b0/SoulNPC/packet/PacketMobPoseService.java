package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.mob.NpcMobProfileRegistry;
import bm.b0b0b0.SoulNPC.model.NpcAnimationData;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcMobPoseMode;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PacketMobPoseService {

    public void applyInitial(NpcRuntime runtime) {
        List<NpcEntityPose> poses = resolvePoses(runtime);
        if (poses.isEmpty()) {
            return;
        }
        runtime.setMobPoseIndex(0);
        runtime.data().appearance.entityPose = poses.get(0);
        refreshPose(runtime);
    }

    public void advance(NpcRuntime runtime) {
        NpcAnimationData animation = runtime.data().animation;
        List<NpcEntityPose> poses = resolvePoses(runtime);
        if (poses.isEmpty()) {
            return;
        }
        NpcEntityPose next = nextPose(runtime, poses, animation.mobPoseMode);
        runtime.data().appearance.entityPose = next;
        refreshPose(runtime);
    }

    private static void refreshPose(NpcRuntime runtime) {
        PacketMobNpc mob = runtime.packetMob();
        if (mob != null) {
            mob.refreshPose();
            return;
        }
        var npc = runtime.packetNpc();
        if (npc != null && runtime.data().appearance.type.isPlayerModel()) {
            PacketPlayerAppearance.applyToViewers(npc, runtime.data().appearance);
        }
    }

    private static NpcEntityPose nextPose(NpcRuntime runtime, List<NpcEntityPose> poses, NpcMobPoseMode mode) {
        if (mode == NpcMobPoseMode.RANDOM) {
            if (poses.size() == 1) {
                return poses.get(0);
            }
            NpcEntityPose current = runtime.data().appearance.entityPose;
            NpcEntityPose picked;
            do {
                picked = poses.get(ThreadLocalRandom.current().nextInt(poses.size()));
            } while (picked == current);
            return picked;
        }
        int index = (runtime.mobPoseIndex() + 1) % poses.size();
        runtime.setMobPoseIndex(index);
        return poses.get(index);
    }

    private static List<NpcEntityPose> resolvePoses(NpcRuntime runtime) {
        NpcAnimationData animation = runtime.data().animation;
        if (runtime.packetNpc() != null && runtime.data().appearance.type.isPlayerModel()) {
            return resolvePlayerMobPoses(animation);
        }
        var profile = NpcMobProfileRegistry.resolve(runtime.data());
        List<NpcEntityPose> resolved = new ArrayList<>();
        if (animation.mobPoses != null) {
            for (String raw : animation.mobPoses) {
                NpcEntityPose pose = NpcEntityPose.fromString(raw);
                if (!resolved.contains(pose) && profile.supportsPose(pose)) {
                    resolved.add(pose);
                }
            }
        }
        if (resolved.isEmpty()) {
            for (NpcEntityPose pose : profile.supportedPoses()) {
                resolved.add(pose);
            }
        }
        if (resolved.isEmpty()) {
            resolved.add(NpcEntityPose.STANDING);
        }
        return resolved;
    }

    private static List<NpcEntityPose> resolvePlayerMobPoses(NpcAnimationData animation) {
        List<NpcEntityPose> resolved = new ArrayList<>();
        if (animation.mobPoses != null) {
            for (String raw : animation.mobPoses) {
                NpcEntityPose pose = NpcEntityPose.fromString(raw);
                if (!resolved.contains(pose) && NpcEntityPose.playerMobPoses().contains(pose)) {
                    resolved.add(pose);
                }
            }
        }
        if (resolved.isEmpty()) {
            resolved.addAll(NpcEntityPose.playerMobPoses());
        }
        return resolved;
    }
}
