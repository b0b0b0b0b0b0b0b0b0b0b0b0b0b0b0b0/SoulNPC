package bm.b0b0b0.SoulNPC.api;

import org.jetbrains.annotations.NotNull;

public final class SoulNpcApi {

    private static SoulNpcApi instance;

    private final NpcRegistry registry;

    public SoulNpcApi(NpcRegistry registry) {
        this.registry = registry;
    }

    public static @NotNull SoulNpcApi get() {
        if (instance == null) {
            throw new IllegalStateException("SoulNPC is not enabled");
        }
        return instance;
    }

    public static void register(SoulNpcApi api) {
        instance = api;
    }

    public static void unregister() {
        instance = null;
    }

    public @NotNull NpcRegistry getRegistry() {
        return registry;
    }
}
