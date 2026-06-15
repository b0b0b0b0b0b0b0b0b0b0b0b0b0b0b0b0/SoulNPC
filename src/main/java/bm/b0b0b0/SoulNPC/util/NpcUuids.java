package bm.b0b0b0.SoulNPC.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

public final class NpcUuids {

    private NpcUuids() {
    }

    public static UUID forNpc(String npcId) {
        return UUID.nameUUIDFromBytes(("SoulNPC:" + npcId.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
    }

    public static UUID forNpcSeat(String npcId) {
        return UUID.nameUUIDFromBytes(("SoulNPC:seat:" + npcId.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
    }

    public static String profileName(String npcId) {
        String name = npcId.toLowerCase(Locale.ROOT);
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        return name;
    }
}
