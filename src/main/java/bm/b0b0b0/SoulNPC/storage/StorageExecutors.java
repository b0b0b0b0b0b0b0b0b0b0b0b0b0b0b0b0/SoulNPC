package bm.b0b0b0.SoulNPC.storage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class StorageExecutors {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private StorageExecutors() {
    }

    public static ExecutorService create(String name) {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "SoulNPC-" + name + "-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadExecutor(factory);
    }
}
