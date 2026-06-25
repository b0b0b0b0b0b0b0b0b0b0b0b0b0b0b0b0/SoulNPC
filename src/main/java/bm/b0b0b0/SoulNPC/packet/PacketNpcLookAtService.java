package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class PacketNpcLookAtService {

    private static final float LOOK_SMOOTH = 0.28F;
    private static final float GREET_TURN_SMOOTH = 0.22F;

    public void tick(Collection<NpcRuntime> runtimes) {
        for (NpcRuntime runtime : runtimes) {
            if (!runtime.isProfileReady() || runtime.viewers().isEmpty()) {
                continue;
            }
            if (!hasRotationHandle(runtime)) {
                continue;
            }
            NpcFileData data = runtime.data();
            if (runtime.isGreeting()) {
                continue;
            }
            if (!data.lookAtPlayers) {
                holdRestRotation(runtime);
                continue;
            }
            World world = Bukkit.getWorld(data.world);
            if (world == null) {
                continue;
            }
            Player nearest = findNearestPlayer(world, data, world.getPlayers());
            if (nearest == null) {
                smoothToBase(runtime, LOOK_SMOOTH);
                continue;
            }
            lookAtSmooth(runtime, nearest, LOOK_SMOOTH, 0.0F);
        }
    }

    public void lookAtSmooth(NpcRuntime runtime, Player player, float smoothFactor, float pitchOffset) {
        if (player == null || !hasRotationHandle(runtime)) {
            return;
        }
        float[] target = NpcLookAtUtil.targetRotation(runtime.data(), player);
        float[] smoothed = runtime.smoothLookToward(target[0], target[1] + pitchOffset, smoothFactor);
        applyRotation(runtime, smoothed[0], smoothed[1]);
    }

    public void smoothToBase(NpcRuntime runtime, float smoothFactor) {
        float baseYaw = runtime.restYaw();
        float basePitch = runtime.restPitch();
        float yawDelta = Math.abs(NpcLookAtUtil.wrapDegrees(baseYaw - runtime.currentLookYaw()));
        float pitchDelta = Math.abs(basePitch - runtime.currentLookPitch());
        if (yawDelta < 0.75F && pitchDelta < 0.75F) {
            holdRestRotation(runtime);
            return;
        }
        float[] smoothed = runtime.smoothLookToward(baseYaw, basePitch, smoothFactor);
        applyRotation(runtime, smoothed[0], smoothed[1]);
    }

    private void holdRestRotation(NpcRuntime runtime) {
        float baseYaw = runtime.restYaw();
        float basePitch = runtime.restPitch();
        runtime.setLookRotation(baseYaw, basePitch);
        applyRotation(runtime, baseYaw, basePitch);
    }

    public float greetTurnSmooth() {
        return GREET_TURN_SMOOTH;
    }

    private static boolean hasRotationHandle(NpcRuntime runtime) {
        return runtime.packetNpc() != null || runtime.packetMob() != null;
    }

    private static void applyRotation(NpcRuntime runtime, float yaw, float pitch) {
        PacketMobNpc mob = runtime.packetMob();
        if (mob != null) {
            mob.updateRotation(yaw, pitch);
            return;
        }
        NPC npc = runtime.packetNpc();
        if (npc != null) {
            npc.updateRotation(yaw, pitch);
        }
    }

    private static Player findNearestPlayer(World world, NpcFileData data, Iterable<Player> candidates) {
        int range = data.lookAtRange <= 0 ? 32 : data.lookAtRange;
        double rangeSquared = (double) range * range;
        var origin = NpcLookAtUtil.npcEyes(data);
        if (origin == null) {
            return null;
        }
        Player nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : candidates) {
            if (player == null || !player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(world)) {
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
}
