package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.appearance.SkinService;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.hologram.NpcTextLabels;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import bm.b0b0b0.SoulNPC.util.NpcUuids;
import bm.b0b0b0.SoulNPC.util.NpcViewDistance;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.UUID;

public final class PacketNpcViewerService {

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final SkinService skinService;
    private final ItemStackFactory itemStackFactory;
    private final NpcTextLabels textLabels;

    public PacketNpcViewerService(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            SkinService skinService,
            ItemStackFactory itemStackFactory,
            NpcTextLabels textLabels
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.skinService = skinService;
        this.itemStackFactory = itemStackFactory;
        this.textLabels = textLabels;
    }

    public void prepareProfile(NpcRuntime runtime, Runnable onReady) {
        prepareProfile(runtime, onReady, null);
    }

    public void prepareProfile(NpcRuntime runtime, Runnable onReady, java.util.function.Consumer<Throwable> onError) {
        NpcFileData data = runtime.data();
        if (data.appearance.isPacketMob()) {
            var packetType = PacketMobNpc.resolveEntityType(data.appearance);
            if (packetType == null) {
                plugin.getLogger().warning("[SoulNPC] Unknown mob type for " + data.id + ": "
                        + data.appearance.resolvedEntityType());
                if (onReady != null) {
                    onReady.run();
                }
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                PacketMobNpc mob = new PacketMobNpc(data);
                runtime.setPacketMob(mob);
                runtime.resetLookRotation();
                showToNearbyPlayers(runtime);
                if (onReady != null) {
                    onReady.run();
                }
            });
            return;
        }
        skinService.resolveAppearance(data.appearance, data.id, paperProfile -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            UUID npcUuid = NpcUuids.forNpc(data.id);
            UserProfile userProfile = PacketProfileConverter.toUserProfile(
                    paperProfile,
                    npcUuid,
                    NpcUuids.profileName(data.id)
            );
            NPC npc = PacketNpcFactory.create(data, userProfile);
            PacketPlayerEquipment.apply(npc, data.appearance, itemStackFactory);
            runtime.setPacketNpc(npc);
            runtime.resetLookRotation();
            PacketNpcDebug.log(
                    plugin,
                    pluginConfig,
                    "profile ready npc=" + data.id
                            + " entityId=" + data.entityId
                            + " packetName=" + userProfile.getName()
                            + " uuid=" + npcUuid
                            + " skinPartsIndex=" + PacketPlayerAppearance.currentSkinPartsIndex()
            );
            showToNearbyPlayers(runtime);
            if (onReady != null) {
                onReady.run();
            }
        }), error -> {
            plugin.getLogger().warning("[SoulNPC] Skin for " + data.id + ": " + error.getMessage());
            if (onError != null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> onError.accept(error));
            }
        });
    }

    public void tick(Collection<NpcRuntime> runtimes) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickForPlayer(player, runtimes);
        }
    }

    public void tickForPlayer(Player player, Collection<NpcRuntime> runtimes) {
        var performance = pluginConfig.settings().performance;
        int batchSize = performance.packetSpawnBatchSize;
        int processed = 0;

        for (NpcRuntime runtime : runtimes) {
            NpcFileData data = runtime.data();
            int packetViewDistance = NpcViewDistance.packetBlocks(performance, data);
            int hologramViewDistance = NpcViewDistance.hologramBlocks(performance, data);
            if (processed < batchSize && runtime.isProfileReady()) {
                if (!data.enabled || !NpcViewDistance.isWithin(player, data, packetViewDistance)) {
                    hideFrom(player, runtime);
                } else {
                    showTo(player, runtime);
                    processed++;
                }
            }
            textLabels.updateVisibilityForPlayer(player, runtime, hologramViewDistance);
        }
    }

    public void showToNearbyPlayers(NpcRuntime runtime) {
        var performance = pluginConfig.settings().performance;
        NpcFileData data = runtime.data();
        int packetViewDistance = NpcViewDistance.packetBlocks(performance, data);
        int hologramViewDistance = NpcViewDistance.hologramBlocks(performance, data);
        if (!data.enabled || !runtime.isProfileReady()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (NpcViewDistance.isWithin(player, data, packetViewDistance)) {
                showTo(player, runtime);
            } else {
                hideFrom(player, runtime);
            }
            textLabels.updateVisibilityForPlayer(player, runtime, hologramViewDistance);
        }
        syncNametag(runtime);
    }

    public void syncPlayerPose(NpcRuntime runtime) {
        NPC npc = runtime.packetNpc();
        if (npc == null || !runtime.data().appearance.type.isPlayerModel()) {
            return;
        }
        boolean needsSeat = PacketPlayerAppearance.needsSeat(runtime.data().appearance);
        PacketPlayerSeat existingSeat = runtime.playerSeat();
        if (!needsSeat) {
            if (existingSeat != null) {
                existingSeat.despawnAll();
            }
            runtime.refreshPlayerSeat();
            var location = PacketNpcFactory.toPacketLocation(runtime.data());
            if (!PacketPlayerSwimSupport.usesSwimMotion(runtime)) {
                npc.setLocation(location);
                npc.teleport(location);
            }
        } else {
            runtime.refreshPlayerSeat();
            PacketPlayerSeat seat = runtime.playerSeat();
            if (seat != null) {
                for (Object channel : npc.getChannels()) {
                    seat.spawn(channel, npc.getId());
                }
            }
        }
        PacketPlayerAppearance.applyToViewers(npc, runtime.data().appearance);
    }

    public void showTo(Player player, NpcRuntime runtime) {
        if (runtime.isVisibleTo(player.getUniqueId())) {
            return;
        }
        Object channel = channel(player);
        if (channel == null) {
            return;
        }
        PacketMobNpc mob = runtime.packetMob();
        if (mob != null) {
            if (!mob.hasSpawned(channel)) {
                mob.spawn(channel, itemStackFactory);
            }
            runtime.addViewer(player.getUniqueId());
            PacketNpcNametagSync.syncMob(channel, runtime.data());
            return;
        }
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        if (npc.getId() == player.getEntityId()) {
            plugin.getLogger().warning("[SoulNPC] Entity ID collision for NPC " + runtime.data().id
                    + " and player " + player.getName() + " (npcEntityId=" + npc.getId() + ")");
            return;
        }
        PacketNpcDebug.log(
                plugin,
                pluginConfig,
                "showTo npc=" + runtime.data().id
                        + " viewer=" + player.getName()
                        + " npcEntityId=" + npc.getId()
                        + " playerEntityId=" + player.getEntityId()
                        + " spawned=" + npc.hasSpawned(channel)
        );
        if (!npc.hasSpawned(channel)) {
            PacketPlayerSeat seat = runtime.playerSeat();
            if (seat != null) {
                seat.spawnSeat(channel);
            }
            npc.spawn(channel);
            PacketNpcDebug.log(plugin, pluginConfig, "sent npc.spawn npc=" + runtime.data().id
                    + " viewer=" + player.getName());
            PacketPlayerAppearance.apply(channel, npc.getId(), runtime.data().appearance);
            PacketNpcDebug.log(plugin, pluginConfig, "sent entity metadata npc=" + runtime.data().id
                    + " viewer=" + player.getName());
            PacketPlayerEquipment.refresh(npc);
            if (seat != null) {
                seat.mount(channel, npc.getId());
            }
            scheduleTabListRemove(player, npc);
        }
        runtime.addViewer(player.getUniqueId());
        syncNametag(player, runtime);
    }

    public void syncNametag(NpcRuntime runtime) {
        NpcFileData data = runtime.data();
        NPC npc = runtime.packetNpc();
        if (npc != null) {
            for (Object channel : npc.getChannels()) {
                PacketNpcNametagSync.sync(channel, npc, data.appearance);
            }
        }
        PacketMobNpc mob = runtime.packetMob();
        if (mob != null) {
            for (Object channel : mob.channels()) {
                PacketNpcNametagSync.syncMob(channel, data);
            }
        }
    }

    public void syncNametag(Player player, NpcRuntime runtime) {
        Object channel = channel(player);
        NPC npc = runtime.packetNpc();
        if (channel == null || npc == null || !npc.hasSpawned(channel)) {
            return;
        }
        PacketNpcNametagSync.sync(channel, npc, runtime.data().appearance);
    }

    public void refreshEquipment(NpcRuntime runtime) {
        NPC npc = runtime.packetNpc();
        if (npc == null || !runtime.data().appearance.type.isPlayerModel()) {
            return;
        }
        PacketPlayerEquipment.apply(npc, runtime.data().appearance, itemStackFactory);
        PacketPlayerEquipment.refresh(npc);
    }

    public void refreshGlow(NpcRuntime runtime) {
        NPC npc = runtime.packetNpc();
        if (npc != null) {
            PacketPlayerAppearance.applyToViewers(npc, runtime.data().appearance);
        }
        PacketMobNpc mob = runtime.packetMob();
        if (mob != null) {
            mob.refreshPose();
            mob.refreshEquipment(itemStackFactory);
        }
        syncNametag(runtime);
    }

    public void hideFrom(Player player, NpcRuntime runtime) {
        if (!runtime.isVisibleTo(player.getUniqueId())) {
            return;
        }
        Object channel = channel(player);
        PacketMobNpc mob = runtime.packetMob();
        if (mob != null) {
            if (channel != null) {
                mob.despawn(channel);
            }
            runtime.removeViewer(player.getUniqueId());
            return;
        }
        NPC npc = runtime.packetNpc();
        if (npc != null) {
            if (channel != null && npc.hasSpawned(channel)) {
                PacketPlayerSeat seat = runtime.playerSeat();
                if (seat != null) {
                    seat.despawn(channel);
                }
                npc.despawn(channel);
                removeFromTabList(channel, npc.getProfile().getUUID());
            }
        }
        runtime.removeViewer(player.getUniqueId());
    }

    public void despawnAll(Collection<NpcRuntime> runtimes) {
        for (NpcRuntime runtime : runtimes) {
            PacketMobNpc mob = runtime.packetMob();
            if (mob != null) {
                mob.despawnAll();
            }
            NPC npc = runtime.packetNpc();
            if (npc != null) {
                UUID profileUuid = npc.getProfile().getUUID();
                PacketPlayerSeat seat = runtime.playerSeat();
                if (seat != null) {
                    seat.despawnAll();
                }
                for (Object channel : npc.getChannels()) {
                    removeFromTabList(channel, profileUuid);
                }
                npc.despawnAll();
            }
            runtime.clearViewers();
        }
    }

    private void scheduleTabListRemove(Player player, NPC npc) {
        UUID profileUuid = npc.getProfile().getUUID();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Object channel = channel(player);
            if (channel == null || !npc.hasSpawned(channel)) {
                return;
            }
            removeFromTabList(channel, profileUuid);
        }, 2L);
    }

    private void removeFromTabList(Object channel, UUID profileUuid) {
        WrapperPlayServerPlayerInfoRemove remove = new WrapperPlayServerPlayerInfoRemove(profileUuid);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, remove);
    }

    private Object channel(Player player) {
        var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            return null;
        }
        return user.getChannel();
    }
}
