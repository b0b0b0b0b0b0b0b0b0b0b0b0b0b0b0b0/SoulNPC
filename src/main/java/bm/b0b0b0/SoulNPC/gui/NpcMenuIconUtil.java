package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.mob.NpcEntityTypeResolver;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Locale;
import java.util.Optional;

public final class NpcMenuIconUtil {

    private NpcMenuIconUtil() {
    }

    public static Material materialFor(NpcFileData data, Material fallback) {
        if (data == null || data.appearance == null) {
            return fallback;
        }
        if (!data.appearance.isPacketMob()) {
            return Material.PLAYER_HEAD;
        }
        return spawnEggFor(data.appearance).orElse(fallback);
    }

    public static boolean usesPlayerHead(NpcFileData data) {
        return materialFor(data, Material.PLAYER_HEAD) == Material.PLAYER_HEAD;
    }

    private static Optional<Material> spawnEggFor(NpcAppearanceData appearance) {
        String entityId = appearance.entityType;
        if (entityId == null || entityId.isBlank()) {
            entityId = NpcEntityTypeResolver.resolveEntityId(appearance);
        }
        if (entityId == null || entityId.isBlank()) {
            return Optional.empty();
        }
        String canonical = NpcEntityTypeResolver.canonicalMobId(entityId);
        EntityType entityType = parseEntityType(canonical);
        if (entityType == null) {
            return Optional.empty();
        }
        try {
            Material spawnEgg = Material.valueOf(entityType.name() + "_SPAWN_EGG");
            if (spawnEgg.isItem()) {
                return Optional.of(spawnEgg);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return Optional.empty();
    }

    private static EntityType parseEntityType(String entityId) {
        try {
            return EntityType.valueOf(entityId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
