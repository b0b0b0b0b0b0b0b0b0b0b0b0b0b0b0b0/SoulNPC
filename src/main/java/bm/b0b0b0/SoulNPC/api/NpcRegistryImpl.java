package bm.b0b0b0.SoulNPC.api;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcService;

public final class NpcRegistryImpl implements NpcRegistry {

    private final NpcRepository repository;
    private final NpcService npcService;

    public NpcRegistryImpl(NpcRepository repository, NpcService npcService) {
        this.repository = repository;
        this.npcService = npcService;
    }

    @Override
    public java.util.Optional<NpcFileData> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public java.util.Collection<NpcFileData> getAll() {
        return repository.findAll();
    }

    @Override
    public boolean create(NpcFileData data) {
        return npcService.create(data);
    }

    @Override
    public boolean delete(String id) {
        return npcService.delete(id);
    }
}
