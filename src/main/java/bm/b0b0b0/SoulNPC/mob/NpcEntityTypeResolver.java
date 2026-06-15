package bm.b0b0b0.SoulNPC.mob;

import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcDisplayType;
import bm.b0b0b0.SoulNPC.model.NpcFoxVariant;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class NpcEntityTypeResolver {

    private static final Set<String> FOX_ALIASES = Set.of("fox", "fox_red", "fox_snow", "snow_fox");

    private NpcEntityTypeResolver() {
    }

    public static boolean isPacketMob(NpcAppearanceData appearance) {
        if (appearance == null || appearance.type.isPlayerModel()) {
            return false;
        }
        return resolveEntityId(appearance) != null && resolvePacketType(appearance) != null;
    }

    public static String resolveEntityId(NpcAppearanceData appearance) {
        if (appearance == null || appearance.type.isPlayerModel()) {
            return null;
        }
        if (appearance.entityType != null && !appearance.entityType.isBlank()) {
            return canonicalMobId(appearance.entityType);
        }
        return switch (appearance.type.name()) {
            case "FOX" -> "fox";
            case "ARMOR_STAND" -> "armor_stand";
            default -> null;
        };
    }

    public static NpcFoxVariant resolveFoxVariant(NpcAppearanceData appearance) {
        if (appearance == null) {
            return NpcFoxVariant.RED;
        }
        if (appearance.entityType != null && !appearance.entityType.isBlank()) {
            String normalized = normalizeMobId(appearance.entityType);
            if ("fox".equals(normalized) && appearance.legacyFoxVariant() == NpcFoxVariant.SNOW) {
                return NpcFoxVariant.SNOW;
            }
            return foxVariantFor(appearance.entityType);
        }
        return appearance.legacyFoxVariant();
    }

    public static com.github.retrooper.packetevents.protocol.entity.type.EntityType resolvePacketType(
            NpcAppearanceData appearance
    ) {
        String entityId = resolveEntityId(appearance);
        if (entityId == null) {
            return null;
        }
        return resolvePacketType(entityId);
    }

    public static com.github.retrooper.packetevents.protocol.entity.type.EntityType resolvePacketType(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        String normalized = canonicalMobId(entityId);
        com.github.retrooper.packetevents.protocol.entity.type.EntityType type = EntityTypes.getByName(
                "minecraft:" + normalized
        );
        if (type == null) {
            type = EntityTypes.getByName(normalized);
        }
        if (type == null || type == EntityTypes.PLAYER) {
            return null;
        }
        return type;
    }

    public static boolean isValidMobId(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = normalizeMobId(raw);
        if ("player".equals(normalized)) {
            return false;
        }
        if (FOX_ALIASES.contains(normalized)) {
            return true;
        }
        return resolvePacketType(normalized) != null;
    }

    public static Optional<String> tryParseMobId(String raw) {
        if (!isValidMobId(raw)) {
            return Optional.empty();
        }
        return Optional.of(normalizeMobId(raw));
    }

    public static String normalizeMobId(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        return normalized;
    }

    public static String canonicalMobId(String raw) {
        String normalized = normalizeMobId(raw);
        return switch (normalized) {
            case "fox_red", "fox_snow", "snow_fox" -> "fox";
            default -> normalized;
        };
    }

    public static NpcFoxVariant foxVariantFor(String raw) {
        return switch (normalizeMobId(raw)) {
            case "fox_snow", "snow_fox" -> NpcFoxVariant.SNOW;
            default -> NpcFoxVariant.RED;
        };
    }

    public static void applyMob(NpcAppearanceData appearance, String entityId) {
        appearance.type = NpcDisplayType.MOB;
        appearance.entityType = normalizeMobId(entityId);
    }

    public static String[] mobTabChoices() {
        List<String> choices = new ArrayList<>();
        choices.add("player");
        choices.add("fox");
        choices.add("fox_snow");
        choices.add("armor_stand");
        Arrays.stream(EntityType.values())
                .filter(type -> type.isAlive() && type.isSpawnable() && type != EntityType.PLAYER)
                .map(type -> type.name().toLowerCase(Locale.ROOT))
                .filter(name -> !choices.contains(name))
                .filter(name -> !"fox".equals(name))
                .sorted(Comparator.naturalOrder())
                .forEach(choices::add);
        return choices.toArray(String[]::new);
    }
}
