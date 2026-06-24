package bm.b0b0b0.SoulNPC.storage;

import bm.b0b0b0.SoulNPC.model.NpcFileData;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NpcStorageBackend {

    StorageType type();

    CompletableFuture<Map<String, NpcFileData>> loadAll();

    CompletableFuture<Void> save(String id, NpcFileData data);

    CompletableFuture<Void> delete(String id);

    CompletableFuture<String> nextAutoId(Map<String, NpcFileData> currentCache);

    void close();
}
