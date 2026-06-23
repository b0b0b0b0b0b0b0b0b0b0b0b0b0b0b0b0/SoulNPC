package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.appearance.SkinService;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import bm.b0b0b0.SoulNPC.util.NpcUuids;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.UUID;

public final class PacketNpcViewerService {

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final SkinService skinService;
    private final ItemStackFactory itemStackFactory;

    public PacketNpcViewerService(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            SkinService skinService,
            ItemStackFactory itemStackFactory
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.skinService = skinService;
        this.itemStackFactory = itemStackFactory;
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
                PacketMobNpc mob = new PacketMobNpc(data, packetType);
                runtime.setPacketMob(mob);
                runtime.resetLookRotation();
                showToNearbyPlayers(runtime);
                if (onReady != null) {
                    onReady.run();
                }
            });
            return;
        }
        String profileName = data.appearance.profile;
        if (profileName == null || profileName.isBlank()) {
            profileName = data.id;
        }
        String resolved = profileName;
        skinService.resolveProfile(resolved, paperProfile -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            UUID npcUuid = NpcUuids.forNpc(data.id);
            String tabProfileName = NpcUuids.profileName(data.id);
            UserProfile userProfile = PacketProfileConverter.toUserProfile(paperProfile, npcUuid, tabProfileName);
            NPC npc = PacketNpcFactory.create(data, userProfile);
            PacketPlayerEquipment.apply(npc, data.appearance, itemStackFactory);
            runtime.setPacketNpc(npc);
            runtime.resetLookRotation();
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
        int viewDistance = pluginConfig.settings().performance.packetViewDistance;
        double viewDistanceSquared = (double) viewDistance * viewDistance;
        int batchSize = pluginConfig.settings().performance.packetSpawnBatchSize;
        int processed = 0;

        for (NpcRuntime runtime : runtimes) {
            if (processed >= batchSize) {
                return;
            }
            if (!runtime.isProfileReady()) {
                continue;
            }
            NpcFileData data = runtime.data();
            if (!data.enabled) {
                hideFrom(player, runtime);
                continue;
            }
            if (!player.getWorld().getName().equals(data.world)) {
                hideFrom(player, runtime);
                continue;
            }
            Location playerLocation = player.getLocation();
            double dx = data.x - playerLocation.getX();
            double dy = data.y - playerLocation.getY();
            double dz = data.z - playerLocation.getZ();
            if ((dx * dx) + (dy * dy) + (dz * dz) > viewDistanceSquared) {
                hideFrom(player, runtime);
                continue;
            }
            showTo(player, runtime);
            processed++;
        }
    }

    public void showToNearbyPlayers(NpcRuntime runtime) {
        int viewDistance = pluginConfig.settings().performance.packetViewDistance;
        double viewDistanceSquared = (double) viewDistance * viewDistance;
        NpcFileData data = runtime.data();
        if (!data.enabled || !runtime.isProfileReady()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().equals(data.world)) {
                continue;
            }
            Location playerLocation = player.getLocation();
            double dx = data.x - playerLocation.getX();
            double dy = data.y - playerLocation.getY();
            double dz = data.z - playerLocation.getZ();
            if ((dx * dx) + (dy * dy) + (dz * dz) <= viewDistanceSquared) {
                showTo(player, runtime);
            }
        }
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
            npc.setLocation(location);
            npc.teleport(location);
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
                mob.spawn(channel);
            }
            runtime.addViewer(player.getUniqueId());
            return;
        }
        NPC npc = runtime.packetNpc();
        if (npc == null) {
            return;
        }
        if (!npc.hasSpawned(channel)) {
            PacketPlayerSeat seat = runtime.playerSeat();
            if (seat != null) {
                seat.spawnSeat(channel);
            }
            npc.spawn(channel);
            if (runtime.data().appearance.useTextDisplay) {
                PacketNpcNametagHider.hide(channel, npc);
            }
            PacketPlayerAppearance.apply(channel, npc.getId(), runtime.data().appearance);
            PacketPlayerEquipment.refresh(npc);
            if (seat != null) {
                seat.mount(channel, npc.getId());
            }
            scheduleTabListRemove(player, npc);
        }
        runtime.addViewer(player.getUniqueId());
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
