package bm.b0b0b0.SoulNPC.util;

public final class NpcEntityIds {

    private static final int VIRTUAL_BASE = 2_000_000_000;
    private static final int VIRTUAL_SPAN = 100_000_000;

    private NpcEntityIds() {
    }

    public static int resolve(String npcId, int stored) {
        if (stored >= VIRTUAL_BASE) {
            return stored;
        }
        return allocate(npcId, "SoulNPC:eid:");
    }

    public static int seatFor(String npcId, int playerEntityId) {
        int seat = allocate(npcId, "SoulNPC:seat:");
        while (seat == playerEntityId) {
            seat++;
        }
        return seat;
    }

    private static int allocate(String npcId, String salt) {
        int hash = Math.abs((salt + npcId).hashCode());
        return VIRTUAL_BASE + (hash % VIRTUAL_SPAN);
    }
}
