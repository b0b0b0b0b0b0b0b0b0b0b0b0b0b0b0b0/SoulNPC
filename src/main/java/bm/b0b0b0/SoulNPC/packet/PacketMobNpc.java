package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.mob.MobMetadataBuilder;
import bm.b0b0b0.SoulNPC.mob.NpcEntityTypeResolver;
import bm.b0b0b0.SoulNPC.mob.NpcMobProfile;
import bm.b0b0b0.SoulNPC.mob.NpcMobProfileRegistry;
import bm.b0b0b0.SoulNPC.mob.NpcMobPoseSupport;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import bm.b0b0b0.SoulNPC.util.NpcUuids;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PacketMobNpc {

    private final int entityId;
    private final NpcFileData data;
    private final Set<Object> spawnedChannels = new HashSet<>();

    public PacketMobNpc(NpcFileData data) {
        this.data = data;
        this.entityId = data.entityId;
    }

    public int entityId() {
        return entityId;
    }

    public boolean hasSpawned(Object channel) {
        return spawnedChannels.contains(channel);
    }

    public Set<Object> channels() {
        return Set.copyOf(spawnedChannels);
    }

    public void spawn(Object channel, ItemStackFactory itemStackFactory) {
        if (channel == null || spawnedChannels.contains(channel)) {
            return;
        }
        Location location = spawnLocation();
        EntityType entityType = resolveEntityType(data.appearance);
        if (entityType == null) {
            return;
        }
        UUID uuid = NpcUuids.forNpc(data.id);
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                uuid,
                entityType,
                location,
                location.getYaw(),
                0,
                null
        );
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, spawn);
        sendMetadata(channel);
        if (itemStackFactory != null) {
            PacketMobEquipment.apply(channel, entityId, data.appearance, itemStackFactory);
        }
        spawnedChannels.add(channel);
    }

    public void spawn(Object channel) {
        spawn(channel, null);
    }

    public void refreshEquipment(ItemStackFactory itemStackFactory) {
        for (Object channel : Set.copyOf(spawnedChannels)) {
            PacketMobEquipment.refresh(channel, entityId, data.appearance, itemStackFactory);
        }
    }

    public void refreshPose() {
        for (Object channel : Set.copyOf(spawnedChannels)) {
            sendMetadata(channel);
        }
    }

    private void sendMetadata(Object channel) {
        if (channel == null) {
            return;
        }
        var metadata = MobMetadataBuilder.build(data.appearance, profile());
        if (metadata.isEmpty()) {
            return;
        }
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet);
    }

    public void despawn(Object channel) {
        if (channel == null || !spawnedChannels.remove(channel)) {
            return;
        }
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(entityId);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, destroy);
    }

    public void despawnAll() {
        for (Object channel : Set.copyOf(spawnedChannels)) {
            despawn(channel);
        }
    }

    public void teleportToData() {
        teleport(spawnLocation());
    }

    public void teleport(Location location) {
        if (location == null || spawnedChannels.isEmpty()) {
            return;
        }
        WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(
                entityId,
                location.getPosition(),
                location.getYaw(),
                location.getPitch(),
                false
        );
        for (Object channel : spawnedChannels) {
            PacketEvents.getAPI().getProtocolManager().sendPacket(channel, teleport);
        }
    }

    public void updateRotation(float yaw, float pitch) {
        if (spawnedChannels.isEmpty()) {
            return;
        }
        if (isHeadLookOnly()) {
            WrapperPlayServerEntityHeadLook headLook = new WrapperPlayServerEntityHeadLook(entityId, yaw);
            for (Object channel : spawnedChannels) {
                PacketEvents.getAPI().getProtocolManager().sendPacket(channel, headLook);
            }
            return;
        }
        float bodyPitch = bodyPitch(pitch);
        WrapperPlayServerEntityRotation rotation = new WrapperPlayServerEntityRotation(
                entityId,
                yaw,
                bodyPitch,
                true
        );
        WrapperPlayServerEntityHeadLook headLook = new WrapperPlayServerEntityHeadLook(entityId, yaw);
        for (Object channel : spawnedChannels) {
            PacketEvents.getAPI().getProtocolManager().sendPacket(channel, rotation);
            PacketEvents.getAPI().getProtocolManager().sendPacket(channel, headLook);
        }
    }

    private boolean isHeadLookOnly() {
        return isSleepingHeadLookOnly() || profile().lookAtHeadOnly();
    }

    private boolean isSleepingHeadLookOnly() {
        NpcMobProfile profile = profile();
        if (!profile.sleepingHeadLookOnly()) {
            return false;
        }
        if (data.appearance.entityPose == NpcEntityPose.SLEEPING) {
            return true;
        }
        return profile.poseSupport() == NpcMobPoseSupport.DISPLAY
                && data.appearance.mobDisplayPose == NpcMobDisplayPose.ON_BACK;
    }

    private float bodyPitch(float pitch) {
        NpcMobProfile profile = profile();
        if (!profile.lookAtPitch()) {
            return 0.0F;
        }
        return pitch;
    }

    private Location spawnLocation() {
        Location location = PacketNpcFactory.toPacketLocation(data);
        if (!profile().lookAtPitch()) {
            return new Location(location.getPosition(), location.getYaw(), 0.0F);
        }
        return location;
    }

    private NpcMobProfile profile() {
        return NpcMobProfileRegistry.resolve(data);
    }

    public static EntityType resolveEntityType(NpcAppearanceData appearance) {
        return NpcEntityTypeResolver.resolvePacketType(appearance);
    }
}
