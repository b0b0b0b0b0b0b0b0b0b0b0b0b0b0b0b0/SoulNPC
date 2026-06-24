package bm.b0b0b0.SoulNPC.repository;

import bm.b0b0b0.SoulNPC.model.NpcFileData;

import java.util.Collection;
import java.util.Optional;

public interface NpcRepository {

    default boolean isReady() {
        return true;
    }

    default boolean isLoading() {
        return false;
    }

    Collection<NpcFileData> findAll();

    Optional<NpcFileData> findById(String id);

    String nextAutoId();

    void save(NpcFileData data);

    void delete(String id);

    void reload();

    default void reload(Runnable onComplete) {
        reload();
        if (onComplete != null) {
            onComplete.run();
        }
    }
}
