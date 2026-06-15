package bm.b0b0b0.SoulNPC.repository;

import bm.b0b0b0.SoulNPC.model.NpcFileData;

import java.util.Collection;
import java.util.Optional;

public interface NpcRepository {

    Collection<NpcFileData> findAll();

    Optional<NpcFileData> findById(String id);

    void save(NpcFileData data);

    boolean delete(String id);

    void reload();
}
