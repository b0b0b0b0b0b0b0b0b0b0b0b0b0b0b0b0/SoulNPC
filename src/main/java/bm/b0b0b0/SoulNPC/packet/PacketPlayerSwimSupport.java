package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.model.NpcAnimationData;
import bm.b0b0b0.SoulNPC.model.NpcAnimationType;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

public final class PacketPlayerSwimSupport {

    private static final float BOB_AMPLITUDE_AIR = 0.06F;
    private static final float BOB_AMPLITUDE_WATER = 0.035F;

    private PacketPlayerSwimSupport() {
    }

    public static boolean usesSwimMotion(NpcRuntime runtime) {
        if (!runtime.isProfileReady() || runtime.viewers().isEmpty()) {
            return false;
        }
        if (runtime.packetNpc() == null || !runtime.data().appearance.type.isPlayerModel()) {
            return false;
        }
        NpcEntityPose pose = runtime.data().appearance.entityPose;
        if (pose == NpcEntityPose.SWIMMING) {
            return true;
        }
        NpcAnimationData animation = runtime.data().animation;
        return animation.enabled && animation.type == NpcAnimationType.SWIM;
    }

    public static float swimBasePitch(NpcFileData data) {
        if (data.appearance.entityPose == NpcEntityPose.SWIMMING) {
            return 0.0F;
        }
        if (data.appearance.isPacketMob()) {
            return 0.0F;
        }
        return data.pitch;
    }

    public static boolean isInFluid(NpcFileData data) {
        World world = Bukkit.getWorld(data.world);
        if (world == null) {
            return false;
        }
        int x = (int) Math.floor(data.x);
        int z = (int) Math.floor(data.z);
        int feetY = (int) Math.floor(data.y);
        int bodyY = (int) Math.floor(data.y + 0.85D);
        return isWaterLike(world.getBlockAt(x, feetY, z).getType())
                || isWaterLike(world.getBlockAt(x, bodyY, z).getType());
    }

    public static void tick(NpcRuntime runtime) {
        if (!usesSwimMotion(runtime)) {
            return;
        }
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        NpcFileData data = runtime.data();
        int phase = runtime.incrementSwimTicks();
        PacketPlayerAppearance.applyPoseToViewers(npc, data.appearance, NpcEntityPose.SWIMMING);

        float amplitude = isInFluid(data) ? BOB_AMPLITUDE_WATER : BOB_AMPLITUDE_AIR;
        double bob = Math.sin(phase * 0.14D) * amplitude;
        Location base = PacketNpcFactory.toPacketLocation(data);
        Vector3d basePos = base.getPosition();
        Location floated = new Location(
                new Vector3d(basePos.getX(), basePos.getY() + bob, basePos.getZ()),
                runtime.currentLookYaw(),
                swimBasePitch(data)
        );
        npc.setLocation(floated);
        npc.teleport(floated);
        npc.updateRotation(runtime.currentLookYaw(), swimBasePitch(data));
    }

    private static boolean isWaterLike(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return material == Material.WATER
                || material == Material.BUBBLE_COLUMN
                || name.endsWith("_WATER")
                || name.contains("KELP")
                || name.contains("SEAGRASS");
    }
}
