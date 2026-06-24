package bm.b0b0b0.SoulNPC.service;

import bm.b0b0b0.SoulNPC.api.event.SoulNpcCreateEvent;
import bm.b0b0b0.SoulNPC.api.event.SoulNpcDeleteEvent;
import bm.b0b0b0.SoulNPC.model.NpcDisplayType;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import bm.b0b0b0.SoulNPC.model.NpcPoseData;
import bm.b0b0b0.SoulNPC.model.NpcSkinSource;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.util.NpcLocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.function.Consumer;

public final class NpcService {

    private record Deps(
            JavaPlugin plugin,
            NpcRepository repository,
            NpcSpawnService spawnService,
            NpcDefaultsFactory defaultsFactory
    ) {
    }

    private final Deps deps;
    private NpcPoseData poseBuffer;

    public NpcService(
            JavaPlugin plugin,
            NpcRepository repository,
            NpcSpawnService spawnService,
            NpcDefaultsFactory defaultsFactory
    ) {
        this.deps = new Deps(plugin, repository, spawnService, defaultsFactory);
    }

    public Optional<NpcRuntime> findRuntime(String id) {
        return deps.spawnService().findRuntime(id);
    }

    public boolean createAt(Player player, String id) {
        return createAt(player, id, NpcDisplayType.PLAYER);
    }

    public boolean createAt(Player player, String id, NpcDisplayType type) {
        return createAt(player, id, type, null, NpcMobDisplayPose.STANDING);
    }

    public boolean createAt(
            Player player,
            String id,
            NpcDisplayType type,
            String entityType
    ) {
        return createAt(player, id, type, entityType, NpcMobDisplayPose.STANDING);
    }

    public boolean createAt(
            Player player,
            String id,
            NpcDisplayType type,
            String entityType,
            NpcMobDisplayPose mobDisplayPose
    ) {
        return createAt(player, id, type, entityType, mobDisplayPose, null);
    }

    public boolean createAt(
            Player player,
            String id,
            NpcDisplayType type,
            String entityType,
            NpcMobDisplayPose mobDisplayPose,
            String skinProfile
    ) {
        return createAt(player, id, type, entityType, mobDisplayPose, skinProfile, null, null);
    }

    public boolean createAt(
            Player player,
            String id,
            NpcDisplayType type,
            String entityType,
            NpcMobDisplayPose mobDisplayPose,
            String skinProfile,
            Runnable onProfileReady,
            Consumer<Throwable> onProfileError
    ) {
        if (deps.repository().findById(id).isPresent()) {
            return false;
        }
        NpcFileData data = deps.defaultsFactory().createFromPlayer(
                player,
                id,
                type,
                entityType,
                mobDisplayPose,
                skinProfile
        );
        return create(data, onProfileReady, onProfileError);
    }

    public boolean create(NpcFileData data) {
        return create(data, null, null);
    }

    public boolean create(NpcFileData data, Runnable onProfileReady, Consumer<Throwable> onProfileError) {
        if (data == null || data.id == null || data.id.isBlank()) {
            return false;
        }
        if (deps.repository().findById(data.id).isPresent()) {
            return false;
        }
        data.prepareForYamlSave();
        SoulNpcCreateEvent event = new SoulNpcCreateEvent(data);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        deps.repository().save(data);
        deps.spawnService().createRuntime(data, onProfileReady, onProfileError);
        return true;
    }

    public boolean respawn(String id) {
        return deps.spawnService().respawn(id);
    }

    public boolean delete(String id) {
        Optional<NpcFileData> optional = deps.repository().findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        NpcFileData data = optional.get();
        SoulNpcDeleteEvent event = new SoulNpcDeleteEvent(data);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        deps.spawnService().removeRuntime(id);
        deps.repository().delete(id);
        return true;
    }

    public void reload() {
        deps.spawnService().reloadAll();
    }

    public void saveAndRefresh(String id) {
        deps.repository().findById(id).ifPresent(data -> {
            deps.repository().save(data);
            deps.spawnService().refreshHolograms(id);
        });
    }

    public void refreshEquipment(String id) {
        deps.spawnService().refreshEquipment(id);
    }

    public void refreshGlow(String id) {
        deps.spawnService().refreshGlow(id);
    }

    public boolean teleportToPlayer(String id, Player player) {
        Optional<NpcFileData> optional = deps.repository().findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        NpcFileData data = optional.get();
        Location location = NpcLocationUtil.createAtPlayer(player);
        data.world = location.getWorld().getName();
        data.x = location.getX();
        data.y = location.getY();
        data.z = location.getZ();
        data.yaw = location.getYaw();
        data.pitch = location.getPitch();
        deps.repository().save(data);
        deps.spawnService().relocate(id);
        return true;
    }

    public void burstGroundItems(String id) {
        deps.spawnService().burstGroundItems(id);
    }

    public void toggleLookAtPlayers(String id) {
        Optional<NpcFileData> optional = deps.repository().findById(id);
        if (optional.isEmpty()) {
            return;
        }
        NpcFileData data = optional.get();
        data.lookAtPlayers = !data.lookAtPlayers;
        deps.repository().save(data);
    }

    public void cyclePlayerEntityPose(String id, boolean reverse) {
        Optional<NpcFileData> optional = deps.repository().findById(id);
        if (optional.isEmpty()) {
            return;
        }
        NpcFileData data = optional.get();
        if (!data.appearance.type.isPlayerModel()) {
            return;
        }
        NpcEntityPose next = NpcEntityPose.nextPlayerGuiPose(data.appearance.entityPose, reverse);
        data.appearance.entityPose = next;
        deps.repository().save(data);
        deps.spawnService().refreshPose(id);
    }

    public void copyPoseFrom(Player player) {
        NpcPoseData pose = new NpcPoseData();
        pose.head = new bm.b0b0b0.SoulNPC.model.EulerAngleData(
                player.getLocation().getPitch(),
                player.getLocation().getYaw(),
                0F
        );
        poseBuffer = pose;
    }

    public boolean applyBufferedPose(String id) {
        if (poseBuffer == null) {
            return false;
        }
        Optional<NpcFileData> optional = deps.repository().findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        NpcFileData data = optional.get();
        data.pose = poseBuffer.copy();
        deps.repository().save(data);
        deps.spawnService().refreshPose(id);
        return true;
    }

    public boolean setSkin(String id, String profile) {
        return setSkin(id, profile, null, null);
    }

    public boolean setSkin(String id, String profile, Runnable onReady, Consumer<Throwable> onError) {
        return setSkin(id, NpcSkinSource.NICK, profile, null, null, onReady, onError);
    }

    public boolean setSkin(
            String id,
            NpcSkinSource source,
            String profile,
            String skinUrl,
            String skinFile,
            Runnable onReady,
            Consumer<Throwable> onError
    ) {
        Optional<NpcFileData> optional = deps.repository().findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        NpcFileData data = optional.get();
        if (!data.appearance.type.isPlayerModel()) {
            return false;
        }
        data.appearance.skinSource = source == null ? NpcSkinSource.NICK : source;
        data.appearance.profile = profile == null ? "" : profile.trim();
        data.appearance.skinUrl = skinUrl == null ? "" : skinUrl.trim();
        data.appearance.skinFile = skinFile == null ? "" : skinFile.trim();
        deps.repository().save(data);
        return deps.spawnService().respawn(id, onReady, onError);
    }

    public NpcPoseData poseBuffer() {
        return poseBuffer;
    }
}
