package bm.b0b0b0.SoulNPC.service;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.mob.NpcMobProfileRegistry;
import bm.b0b0b0.SoulNPC.model.NpcAnimationData;
import bm.b0b0b0.SoulNPC.model.NpcAnimationType;
import bm.b0b0b0.SoulNPC.packet.PacketMobPoseService;
import bm.b0b0b0.SoulNPC.packet.PacketNpcAnimator;
import bm.b0b0b0.SoulNPC.packet.PacketNpcGreetService;
import bm.b0b0b0.SoulNPC.packet.PacketNpcLookAtService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

public final class NpcAnimationService {

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final PacketNpcAnimator animator;
    private final PacketNpcLookAtService lookAtService;
    private final PacketNpcGreetService greetService;
    private final PacketMobPoseService mobPoseService;
    private BukkitTask animationTask;

    public NpcAnimationService(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            PacketNpcAnimator animator,
            PacketNpcLookAtService lookAtService,
            PacketNpcGreetService greetService,
            PacketMobPoseService mobPoseService
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.animator = animator;
        this.lookAtService = lookAtService;
        this.greetService = greetService;
        this.mobPoseService = mobPoseService;
    }

    public void start(Collection<NpcRuntime> runtimes) {
        stop();
        int interval = pluginConfig.settings().performance.animationTickInterval;
        if (interval <= 0) {
            return;
        }
        animationTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> tick(runtimes),
                interval,
                interval
        );
    }

    public void stop() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }

    public void tick(Collection<NpcRuntime> runtimes) {
        greetService.tick(runtimes);
        lookAtService.tick(runtimes);

        int batchSize = pluginConfig.settings().performance.animationBatchSize;
        if (batchSize <= 0) {
            batchSize = 16;
        }
        int processed = 0;

        for (NpcRuntime runtime : runtimes) {
            if (processed >= batchSize) {
                return;
            }
            if (!shouldTickAnimation(runtime)) {
                continue;
            }
            int ticks = runtime.incrementAnimationTicks();
            int intervalTicks = Math.max(1, runtime.data().animation.intervalTicks);
            if (ticks % intervalTicks != 0) {
                continue;
            }
            if (shouldMobPose(runtime)) {
                mobPoseService.advance(runtime);
            } else {
                animator.play(runtime);
            }
            processed++;
        }
    }

    public void onMobSpawned(NpcRuntime runtime) {
        if (shouldMobPose(runtime)) {
            mobPoseService.applyInitial(runtime);
        }
    }

    public void resetRuntime(NpcRuntime runtime) {
        runtime.resetAnimationState();
        animator.applyBaseRotation(runtime);
        if (shouldMobPose(runtime)) {
            mobPoseService.applyInitial(runtime);
        }
    }

    private static boolean shouldTickAnimation(NpcRuntime runtime) {
        if (!runtime.isProfileReady() || runtime.viewers().isEmpty()) {
            return false;
        }
        NpcAnimationData animation = runtime.data().animation;
        if (!animation.enabled || animation.type == NpcAnimationType.NONE) {
            return false;
        }
        if (animation.type == NpcAnimationType.GREET) {
            return false;
        }
        if (shouldMobPose(runtime)) {
            return true;
        }
        if (runtime.data().lookAtPlayers && animation.type.conflictsWithLookAt()) {
            return false;
        }
        if (animation.type == NpcAnimationType.CUSTOM) {
            return animation.frames != null && !animation.frames.isEmpty()
                    || runtime.data().pose != null;
        }
        return true;
    }

    private static boolean shouldMobPose(NpcRuntime runtime) {
        NpcAnimationData animation = runtime.data().animation;
        if (!animation.enabled || animation.type != NpcAnimationType.MOB_POSE) {
            return false;
        }
        if (runtime.packetMob() != null && runtime.data().appearance.isPacketMob()) {
            return NpcMobProfileRegistry.resolve(runtime.data()).mobPoseAnimation();
        }
        return runtime.packetNpc() != null && runtime.data().appearance.type.isPlayerModel();
    }
}
