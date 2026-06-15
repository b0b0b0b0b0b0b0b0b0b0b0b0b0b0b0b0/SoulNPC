package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.model.NpcAnimationData;
import bm.b0b0b0.SoulNPC.model.NpcAnimationType;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcGreetStyle;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public final class PacketNpcGreetService {

    private final PacketNpcLookAtService lookAtService;

    public PacketNpcGreetService(PacketNpcLookAtService lookAtService) {
        this.lookAtService = lookAtService;
    }

    public void tick(Collection<NpcRuntime> runtimes) {
        for (NpcRuntime runtime : runtimes) {
            if (!canProcess(runtime)) {
                continue;
            }
            if (runtime.isGreeting()) {
                tickActiveGreet(runtime);
                continue;
            }
            Player target = findGreetTarget(runtime);
            if (target != null) {
                startGreet(runtime, target);
            }
        }
    }

    private void tickActiveGreet(NpcRuntime runtime) {
        Player player = Bukkit.getPlayer(runtime.greetTargetId());
        if (player == null || !player.isOnline() || player.isDead()) {
            restoreGreetPose(runtime);
            runtime.endGreet();
            return;
        }
        NpcAnimationData animation = runtime.data().animation;
        int turnTicks = Math.max(6, animation.greetTurnTicks);
        int nodTicks = Math.max(8, animation.greetNodTicks);
        int crouchTicks = Math.max(8, animation.greetCrouchTicks);
        float nodDegrees = animation.greetNodDegrees <= 0.0F ? 9.0F : animation.greetNodDegrees;

        switch (runtime.greetPhase()) {
            case TURN -> {
                lookAtService.lookAtSmooth(runtime, player, lookAtService.greetTurnSmooth(), 0.0F);
                if (runtime.incrementGreetPhaseTick() >= turnTicks) {
                    if (shouldCrouchGreet(runtime, animation)) {
                        applyGreetCrouch(runtime);
                        runtime.setGreetPhase(NpcRuntime.GreetPhase.CROUCH);
                    } else {
                        runtime.setGreetPhase(NpcRuntime.GreetPhase.NOD);
                    }
                }
            }
            case CROUCH -> {
                lookAtService.lookAtSmooth(runtime, player, 0.35F, 0.0F);
                if (runtime.incrementGreetPhaseTick() >= crouchTicks) {
                    restoreGreetPose(runtime);
                    runtime.endGreet();
                }
            }
            case NOD -> {
                int nodTick = runtime.greetPhaseTick();
                float progress = nodTicks <= 1 ? 1.0F : nodTick / (float) (nodTicks - 1);
                float nod = (float) Math.sin(progress * Math.PI) * nodDegrees;
                lookAtService.lookAtSmooth(runtime, player, 0.45F, nod);
                if (runtime.incrementGreetPhaseTick() >= nodTicks) {
                    runtime.endGreet();
                }
            }
            case IDLE -> runtime.endGreet();
        }
    }

    private void startGreet(NpcRuntime runtime, Player player) {
        runtime.markGreeted(player.getUniqueId());
        runtime.beginGreet(player.getUniqueId());
        lookAtService.lookAtSmooth(runtime, player, lookAtService.greetTurnSmooth(), 0.0F);
    }

    private static boolean shouldCrouchGreet(NpcRuntime runtime, NpcAnimationData animation) {
        return animation.greetStyle == NpcGreetStyle.CROUCH && runtime.canGreetCrouch();
    }

    private static void applyGreetCrouch(NpcRuntime runtime) {
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        PacketPlayerAppearance.applyPoseToViewers(npc, runtime.data().appearance, NpcEntityPose.CROUCHING);
        runtime.setGreetCrouchActive(true);
    }

    private static void restoreGreetPose(NpcRuntime runtime) {
        if (!runtime.greetCrouchActive() || runtime.packetNpc() == null) {
            runtime.setGreetCrouchActive(false);
            return;
        }
        PacketPlayerAppearance.applyToViewers(runtime.packetNpc(), runtime.data().appearance);
        runtime.setGreetCrouchActive(false);
    }

    private static Player findGreetTarget(NpcRuntime runtime) {
        NpcFileData data = runtime.data();
        NpcAnimationData animation = data.animation;
        int range = animation.greetRange <= 0 ? 8 : animation.greetRange;
        double rangeSquared = (double) range * range;
        long cooldownMillis = Math.max(1, animation.greetCooldownSeconds) * 1000L;

        World world = Bukkit.getWorld(data.world);
        if (world == null) {
            return null;
        }
        var origin = NpcLookAtUtil.npcEyes(data);
        if (origin == null) {
            return null;
        }
        Player nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (UUID viewerId : runtime.viewers()) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player == null || !player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(world)) {
                continue;
            }
            if (!runtime.canGreetPlayer(player.getUniqueId(), cooldownMillis)) {
                continue;
            }
            double distanceSquared = player.getEyeLocation().distanceSquared(origin);
            if (distanceSquared > rangeSquared || distanceSquared >= bestDistance) {
                continue;
            }
            bestDistance = distanceSquared;
            nearest = player;
        }
        return nearest;
    }

    private static boolean canProcess(NpcRuntime runtime) {
        if (!runtime.isProfileReady() || runtime.viewers().isEmpty()) {
            return false;
        }
        NpcAnimationData animation = runtime.data().animation;
        return animation.enabled && animation.type == NpcAnimationType.GREET;
    }
}
