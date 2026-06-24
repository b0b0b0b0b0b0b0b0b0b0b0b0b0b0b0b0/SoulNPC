package bm.b0b0b0.SoulNPC.repository;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.storage.NpcStorageBackend;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CachedNpcRepository implements NpcRepository {

    private final JavaPlugin plugin;
    private final NpcStorageBackend backend;
    private final Map<String, NpcFileData> cache = new LinkedHashMap<>();
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicBoolean loading = new AtomicBoolean(false);

    public CachedNpcRepository(JavaPlugin plugin, NpcStorageBackend backend) {
        this.plugin = plugin;
        this.backend = backend;
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    @Override
    public boolean isLoading() {
        return loading.get();
    }

    @Override
    public Collection<NpcFileData> findAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public Optional<NpcFileData> findById(String id) {
        return Optional.ofNullable(cache.get(normalizeId(id)));
    }

    @Override
    public String nextAutoId() {
        int candidate = 1;
        while (cache.containsKey(String.valueOf(candidate))) {
            candidate++;
        }
        return String.valueOf(candidate);
    }

    @Override
    public void save(NpcFileData data) {
        String id = normalizeId(data.id);
        data.id = id;
        data.prepareForYamlSave();
        cache.put(id, data);
        backend.save(id, data).exceptionally(error -> {
            plugin.getLogger().warning("Failed to persist NPC " + id + ": " + error.getMessage());
            return null;
        });
    }

    @Override
    public void delete(String id) {
        String normalized = normalizeId(id);
        if (!cache.containsKey(normalized)) {
            return;
        }
        cache.remove(normalized);
        backend.delete(normalized).exceptionally(error -> {
            plugin.getLogger().warning("Failed to delete NPC " + normalized + ": " + error.getMessage());
            return null;
        });
    }

    @Override
    public void reload() {
        reload(null);
    }

    @Override
    public void reload(Runnable onComplete) {
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        backend.loadAll().whenComplete((loaded, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            loading.set(false);
            if (error != null) {
                plugin.getLogger().warning("Failed to reload NPC storage: " + error.getMessage());
            } else {
                cache.clear();
                if (loaded != null) {
                    cache.putAll(loaded);
                }
                ready.set(true);
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }));
    }

    public void initialLoad(Runnable onComplete) {
        reload(onComplete);
    }

    public NpcStorageBackend backend() {
        return backend;
    }

    private static String normalizeId(String id) {
        return id.toLowerCase();
    }
}
