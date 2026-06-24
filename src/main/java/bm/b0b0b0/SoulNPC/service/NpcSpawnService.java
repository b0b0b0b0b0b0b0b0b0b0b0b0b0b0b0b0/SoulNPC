package bm.b0b0b0.SoulNPC.service;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.effect.NpcGroundItemEffectService;
import bm.b0b0b0.SoulNPC.hologram.NpcTextLabels;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.packet.PacketNpcLookAtService;
import bm.b0b0b0.SoulNPC.packet.PacketNpcViewerService;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.util.NpcEntityIds;
import bm.b0b0b0.SoulNPC.util.NpcIdValidator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class NpcSpawnService {

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final NpcRepository repository;
    private final PacketNpcViewerService viewerService;
    private final NpcTextLabels textLabels;
    private final NpcAnimationService animationService;
    private final PacketNpcLookAtService lookAtService;
    private final NpcGroundItemEffectService groundItemEffectService;
    private final Map<String, NpcRuntime> runtimes = new LinkedHashMap<>();
    private final Map<Integer, String> entityIndex = new LinkedHashMap<>();
    private final Map<Integer, String> hologramEntityIndex = new LinkedHashMap<>();
    private BukkitTask viewerTask;

    public NpcSpawnService(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            NpcRepository repository,
            PacketNpcViewerService viewerService,
            NpcTextLabels textLabels,
            NpcAnimationService animationService,
            PacketNpcLookAtService lookAtService,
            NpcGroundItemEffectService groundItemEffectService
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.repository = repository;
        this.viewerService = viewerService;
        this.textLabels = textLabels;
        this.animationService = animationService;
        this.lookAtService = lookAtService;
        this.groundItemEffectService = groundItemEffectService;
    }

    public void start() {
        stop();
        int interval = pluginConfig.settings().performance.packetTickInterval;
        if (interval <= 0) {
            interval = 10;
        }
        viewerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            viewerService.tick(runtimes.values());
        }, interval, interval);
        animationService.start(runtimes.values());
        groundItemEffectService.start(runtimes.values());
    }

    public void stop() {
        groundItemEffectService.stop();
        animationService.stop();
        if (viewerTask != null) {
            viewerTask.cancel();
            viewerTask = null;
        }
        viewerService.despawnAll(runtimes.values());
        textLabels.removeAll(runtimes.values());
        textLabels.purgeAllPluginDisplays();
    }

    public Collection<NpcRuntime> runtimes() {
        return runtimes.values();
    }

    public Optional<NpcRuntime> findRuntime(String id) {
        return Optional.ofNullable(runtimes.get(NpcIdValidator.canonicalKey(id)));
    }

    public Optional<NpcRuntime> findByEntityId(int entityId) {
        String id = entityIndex.get(entityId);
        if (id == null) {
            id = hologramEntityIndex.get(entityId);
        }
        if (id == null) {
            return Optional.empty();
        }
        return findRuntime(id);
    }

    public void registerHologramEntity(int entityId, String npcId) {
        hologramEntityIndex.put(entityId, NpcIdValidator.canonicalKey(npcId));
    }

    public void registerEntityAliases(NpcRuntime runtime) {
        NpcFileData data = runtime.data();
        String npcId = NpcIdValidator.canonicalKey(data.id);
        entityIndex.put(data.entityId, npcId);
        var seat = runtime.playerSeat();
        if (seat != null) {
            entityIndex.put(seat.seatEntityId(), npcId);
        }
    }

    public void unregisterHologramEntity(int entityId) {
        hologramEntityIndex.remove(entityId);
    }

    public void bootstrapLoadedNpcs() {
        registerAllEnabled();
    }

    public void reloadAll() {
        viewerService.despawnAll(runtimes.values());
        textLabels.removeAll(runtimes.values());
        textLabels.purgeAllPluginDisplays();
        runtimes.clear();
        entityIndex.clear();
        hologramEntityIndex.clear();
        repository.reload(this::registerAllEnabled);
    }

    private void registerAllEnabled() {
        for (NpcFileData data : repository.findAll()) {
            if (!data.enabled) {
                continue;
            }
            register(data);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            viewerService.tickForPlayer(player, runtimes.values());
        }
    }

    public void register(NpcFileData data) {
        register(data, null, null);
    }

    public void register(NpcFileData data, Runnable onProfileReady, Consumer<Throwable> onProfileError) {
        data.appearance.normalizePresentation();
        data.entityId = NpcEntityIds.resolve(data.id, data.entityId);
        NpcRuntime runtime = new NpcRuntime(data);
        runtimes.put(NpcIdValidator.canonicalKey(data.id), runtime);
        entityIndex.put(data.entityId, NpcIdValidator.canonicalKey(data.id));
        textLabels.spawn(runtime);
        viewerService.prepareProfile(runtime, () -> {
            onRuntimeProfileReady(runtime);
            if (onProfileReady != null) {
                onProfileReady.run();
            }
        }, onProfileError);
    }

    private void onRuntimeProfileReady(NpcRuntime runtime) {
        if (runtime.packetMob() != null) {
            runtime.packetMob().updateRotation(runtime.data().yaw, runtime.data().pitch);
            animationService.onMobSpawned(runtime);
        } else if (runtime.packetNpc() != null) {
            viewerService.syncPlayerPose(runtime);
            registerEntityAliases(runtime);
            animationService.onMobSpawned(runtime);
        }
    }

    public void createRuntime(NpcFileData data) {
        createRuntime(data, null, null);
    }

    public void createRuntime(NpcFileData data, Runnable onProfileReady, Consumer<Throwable> onProfileError) {
        register(data, onProfileReady, onProfileError);
    }

    public void removeRuntime(String id) {
        String key = NpcIdValidator.canonicalKey(id);
        NpcRuntime runtime = runtimes.remove(key);
        if (runtime != null) {
            entityIndex.remove(runtime.data().entityId);
            animationService.resetRuntime(runtime);
            textLabels.remove(runtime);
            if (runtime.packetMob() != null) {
                runtime.packetMob().despawnAll();
            }
            if (runtime.packetNpc() != null) {
                if (runtime.playerSeat() != null) {
                    runtime.playerSeat().despawnAll();
                }
                runtime.packetNpc().despawnAll();
            }
            runtime.clearViewers();
        }
    }

    public boolean respawn(String id) {
        return respawn(id, null, null);
    }

    public boolean respawn(String id, Runnable onProfileReady, java.util.function.Consumer<Throwable> onProfileError) {
        Optional<NpcRuntime> optional = findRuntime(id);
        if (optional.isPresent()) {
            NpcRuntime runtime = optional.get();
            if (runtime.packetMob() != null) {
                runtime.packetMob().despawnAll();
            }
            if (runtime.packetNpc() != null) {
                if (runtime.playerSeat() != null) {
                    runtime.playerSeat().despawnAll();
                }
                runtime.packetNpc().despawnAll();
            }
            runtime.clearViewers();
            runtime.setPacketNpc(null);
            runtime.setPacketMob(null);
            runtime.resetAnimationState();
            textLabels.spawn(runtime);
            viewerService.prepareProfile(runtime, () -> {
                onRuntimeProfileReady(runtime);
                if (onProfileReady != null) {
                    onProfileReady.run();
                }
            }, onProfileError);
            return true;
        }
        Optional<NpcFileData> data = repository.findById(id);
        if (data.isEmpty()) {
            return false;
        }
        createRuntime(data.get());
        return findRuntime(id).map(NpcRuntime::isProfileReady).orElse(false);
    }

    public void onPlayerQuit(Player player) {
        for (NpcRuntime runtime : runtimes.values()) {
            textLabels.hideFromPlayer(player, runtime);
            viewerService.hideFrom(player, runtime);
        }
    }

    public void onPlayerJoin(Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                viewerService.tickForPlayer(player, runtimes.values());
            }
        }, 2L);
    }

    public void refreshHolograms(String id) {
        findRuntime(id).ifPresent(runtime -> {
            textLabels.spawn(runtime);
            viewerService.showToNearbyPlayers(runtime);
        });
    }

    public void refreshEquipment(String id) {
        findRuntime(id).ifPresent(viewerService::refreshEquipment);
    }

    public void refreshGlow(String id) {
        findRuntime(id).ifPresent(viewerService::refreshGlow);
    }

    public void relocate(String id) {
        findRuntime(id).ifPresent(runtime -> {
            NpcFileData data = runtime.data();
            if (runtime.packetMob() != null) {
                runtime.packetMob().updateRotation(data.yaw, data.pitch);
            } else if (runtime.packetNpc() != null) {
                var packetLocation = bm.b0b0b0.SoulNPC.packet.PacketNpcFactory.toPacketLocation(data);
                runtime.packetNpc().setLocation(packetLocation);
                runtime.packetNpc().teleport(packetLocation);
            }
            textLabels.spawn(runtime);
            viewerService.showToNearbyPlayers(runtime);
        });
    }

    public void burstGroundItems(String id) {
        findRuntime(id).ifPresent(groundItemEffectService::burst);
    }

    public void refreshPose(String id) {
        findRuntime(id).ifPresent(runtime -> {
            animationService.resetRuntime(runtime);
            if (runtime.packetMob() != null) {
                runtime.packetMob().refreshPose();
                runtime.packetMob().updateRotation(runtime.data().yaw, runtime.data().pitch);
            } else if (runtime.packetNpc() != null) {
                viewerService.syncPlayerPose(runtime);
                registerEntityAliases(runtime);
            }
            viewerService.showToNearbyPlayers(runtime);
        });
    }
}
