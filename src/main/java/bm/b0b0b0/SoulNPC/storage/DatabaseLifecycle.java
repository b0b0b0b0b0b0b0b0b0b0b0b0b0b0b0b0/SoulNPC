package bm.b0b0b0.SoulNPC.storage;

import java.util.ArrayList;
import java.util.List;

public final class DatabaseLifecycle {

    private final List<NpcStorageBackend> backends = new ArrayList<>();

    public void register(NpcStorageBackend backend) {
        backends.add(backend);
    }

    public void closeAll() {
        for (NpcStorageBackend backend : backends) {
            try {
                backend.close();
            } catch (Exception ignored) {
            }
        }
        backends.clear();
    }
}
