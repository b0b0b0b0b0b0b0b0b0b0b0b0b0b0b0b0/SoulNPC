package bm.b0b0b0.SoulNPC.api;

import bm.b0b0b0.SoulNPC.model.NpcFileData;

import java.util.Collection;
import java.util.Optional;

public interface NpcRegistry {

    Optional<NpcFileData> getById(String id);

    Collection<NpcFileData> getAll();

    boolean create(NpcFileData data);

    boolean delete(String id);
}
