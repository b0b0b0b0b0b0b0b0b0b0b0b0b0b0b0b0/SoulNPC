package bm.b0b0b0.SoulNPC.util;

public final class NpcEntityIds {

    private NpcEntityIds() {
    }

    public static int resolve(String npcId, int stored) {
        if (stored > 0) {
            return stored;
        }
        int generated = Math.abs(npcId.hashCode() % 2_000_000_000);
        if (generated < 10_000) {
            generated += 10_000;
        }
        return generated;
    }

    public static int seatFor(String npcId, int playerEntityId) {
        int generated = Math.abs(("SoulNPC:seat:" + npcId).hashCode() % 2_000_000_000);
        if (generated < 10_000) {
            generated += 10_000;
        }
        if (generated == playerEntityId) {
            generated++;
        }
        return generated;
    }
}
