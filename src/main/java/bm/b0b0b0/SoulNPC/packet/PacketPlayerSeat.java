package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.util.NpcEntityIds;
import bm.b0b0b0.SoulNPC.util.NpcUuids;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Невидимая стойка для позы SITTING у player NPC — клиент не рисует сидение только по EntityPose.
 */
public final class PacketPlayerSeat {

    private static final int ENTITY_FLAGS_INDEX = 0;
    private static final int NO_GRAVITY_INDEX = 5;
    private static final int ARMOR_STAND_FLAGS_INDEX = 15;

    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte STAND_FLAG_SMALL = 0x01;
    private static final byte STAND_FLAG_MARKER = 0x10;

    private final int seatEntityId;
    private final UUID seatUuid;
    private final NpcFileData data;
    private final Set<Object> spawnedChannels = new HashSet<>();

    public PacketPlayerSeat(NpcFileData data) {
        this.data = data;
        this.seatEntityId = NpcEntityIds.seatFor(data.id, data.entityId);
        this.seatUuid = NpcUuids.forNpcSeat(data.id);
    }

    public int seatEntityId() {
        return seatEntityId;
    }

    public boolean hasSpawned(Object channel) {
        return spawnedChannels.contains(channel);
    }

    public void spawnSeat(Object channel) {
        if (channel == null || spawnedChannels.contains(channel)) {
            return;
        }
        Location location = seatLocation();
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                seatEntityId,
                seatUuid,
                EntityTypes.ARMOR_STAND,
                location,
                data.yaw,
                0,
                null
        );
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, spawn);
        sendMetadata(channel);
        spawnedChannels.add(channel);
    }

    public void mount(Object channel, int playerEntityId) {
        if (channel == null || !spawnedChannels.contains(channel)) {
            return;
        }
        WrapperPlayServerSetPassengers mount = new WrapperPlayServerSetPassengers(
                seatEntityId,
                new int[]{playerEntityId}
        );
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, mount);
    }

    public void spawn(Object channel, int playerEntityId) {
        spawnSeat(channel);
        mount(channel, playerEntityId);
    }

    public void despawn(Object channel) {
        if (channel == null || !spawnedChannels.remove(channel)) {
            return;
        }
        WrapperPlayServerSetPassengers unmount = new WrapperPlayServerSetPassengers(seatEntityId, new int[0]);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, unmount);
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(seatEntityId);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, destroy);
    }

    public void despawnAll() {
        for (Object channel : Set.copyOf(spawnedChannels)) {
            despawn(channel);
        }
    }

    private void sendMetadata(Object channel) {
        List<EntityData<?>> metadata = List.of(
                new EntityData<>(ENTITY_FLAGS_INDEX, EntityDataTypes.BYTE, FLAG_INVISIBLE),
                new EntityData<>(NO_GRAVITY_INDEX, EntityDataTypes.BOOLEAN, true),
                new EntityData<>(ARMOR_STAND_FLAGS_INDEX, EntityDataTypes.BYTE, (byte) (STAND_FLAG_SMALL | STAND_FLAG_MARKER))
        );
        PacketEvents.getAPI().getProtocolManager().sendPacket(
                channel,
                new WrapperPlayServerEntityMetadata(seatEntityId, metadata)
        );
    }

    private Location seatLocation() {
        return new Location(
                PacketNpcFactory.toPacketLocation(data).getPosition(),
                data.yaw,
                0.0F
        );
    }
}
