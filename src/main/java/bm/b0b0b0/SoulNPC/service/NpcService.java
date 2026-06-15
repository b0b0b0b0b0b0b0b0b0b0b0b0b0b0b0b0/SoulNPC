package bm.b0b0b0.SoulNPC.service;

import bm.b0b0b0.SoulNPC.model.NpcDisplayType;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import bm.b0b0b0.SoulNPC.model.NpcPoseData;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.util.NpcLocationUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class NpcService {

    private final NpcRepository repository;
    private final NpcSpawnService spawnService;
    private final NpcDefaultsFactory defaultsFactory;
    private NpcPoseData poseBuffer;

    public NpcService(NpcRepository repository, NpcSpawnService spawnService, NpcDefaultsFactory defaultsFactory) {
        this.repository = repository;
        this.spawnService = spawnService;
        this.defaultsFactory = defaultsFactory;
    }

    public Optional<NpcRuntime> findRuntime(String id) {
        return spawnService.findRuntime(id);
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
        if (repository.findById(id).isPresent()) {
            return false;
        }
        NpcFileData data = defaultsFactory.createFromPlayer(player, id, type, entityType, mobDisplayPose);
        repository.save(data);
        spawnService.createRuntime(data);
        return true;
    }

    public boolean respawn(String id) {
        return spawnService.respawn(id);
    }

    public boolean delete(String id) {
        if (repository.findById(id).isEmpty()) {
            return false;
        }
        spawnService.removeRuntime(id);
        repository.delete(id);
        return true;
    }

    public void reload() {
        spawnService.reloadAll();
    }

    public void saveAndRefresh(String id) {
        repository.findById(id).ifPresent(data -> {
            repository.save(data);
            spawnService.refreshHolograms(id);
        });
    }

    public boolean teleportToPlayer(String id, Player player) {
        Optional<NpcFileData> optional = repository.findById(id);
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
        repository.save(data);
        spawnService.relocate(id);
        return true;
    }

    public void burstGroundItems(String id) {
        spawnService.burstGroundItems(id);
    }

    public boolean toggleLookAtPlayers(String id) {
        Optional<NpcFileData> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        NpcFileData data = optional.get();
        data.lookAtPlayers = !data.lookAtPlayers;
        repository.save(data);
        return true;
    }

    public boolean cyclePlayerEntityPose(String id, boolean reverse) {
        Optional<NpcFileData> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        NpcFileData data = optional.get();
        if (!data.appearance.type.isPlayerModel()) {
            return false;
        }
        NpcEntityPose next = NpcEntityPose.nextPlayerGuiPose(data.appearance.entityPose, reverse);
        data.appearance.entityPose = next;
        repository.save(data);
        spawnService.refreshPose(id);
        return true;
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
        Optional<NpcFileData> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        NpcFileData data = optional.get();
        data.pose = poseBuffer.copy();
        repository.save(data);
        spawnService.refreshPose(id);
        return true;
    }

    public boolean setSkin(String id, String profile) {
        Optional<NpcFileData> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        NpcFileData data = optional.get();
        if (!data.appearance.type.isPlayerModel()) {
            return false;
        }
        data.appearance.profile = profile == null ? "" : profile.trim();
        repository.save(data);
        return spawnService.respawn(id);
    }

    public NpcPoseData poseBuffer() {
        return poseBuffer;
    }
}
