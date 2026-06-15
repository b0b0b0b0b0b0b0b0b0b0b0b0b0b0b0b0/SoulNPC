package bm.b0b0b0.SoulNPC.mob;

import org.bukkit.entity.EntityType;

import java.util.Locale;
import java.util.Map;

final class NpcMobDimensions {

    private static final float DEFAULT_ALIVE_HEIGHT = 1.8F;
    private static final float DEFAULT_ALIVE_WIDTH = 0.6F;
    private static final float DEFAULT_OTHER_HEIGHT = 1.0F;
    private static final float DEFAULT_OTHER_WIDTH = 0.6F;

    private static final Map<String, Float> HEIGHT_OVERRIDES = Map.ofEntries(
            Map.entry("fox", 0.7F),
            Map.entry("wolf", 0.85F),
            Map.entry("cat", 0.7F),
            Map.entry("ocelot", 0.7F),
            Map.entry("rabbit", 0.5F),
            Map.entry("chicken", 0.7F),
            Map.entry("parrot", 0.9F),
            Map.entry("bat", 0.9F),
            Map.entry("bee", 0.6F),
            Map.entry("silverfish", 0.3F),
            Map.entry("endermite", 0.3F),
            Map.entry("creeper", 1.7F),
            Map.entry("pig", 0.9F),
            Map.entry("cow", 1.4F),
            Map.entry("sheep", 1.3F),
            Map.entry("goat", 1.3F),
            Map.entry("horse", 1.6F),
            Map.entry("donkey", 1.5F),
            Map.entry("mule", 1.6F),
            Map.entry("llama", 1.87F),
            Map.entry("trader_llama", 1.87F),
            Map.entry("camel", 2.375F),
            Map.entry("iron_golem", 2.7F),
            Map.entry("snow_golem", 1.9F),
            Map.entry("villager", 1.95F),
            Map.entry("wandering_trader", 1.95F),
            Map.entry("zombie", 1.95F),
            Map.entry("husk", 1.95F),
            Map.entry("drowned", 1.95F),
            Map.entry("zombie_villager", 1.95F),
            Map.entry("skeleton", 1.99F),
            Map.entry("stray", 1.99F),
            Map.entry("wither_skeleton", 2.4F),
            Map.entry("pillager", 1.95F),
            Map.entry("vindicator", 1.95F),
            Map.entry("evoker", 1.95F),
            Map.entry("witch", 1.95F),
            Map.entry("blaze", 1.8F),
            Map.entry("slime", 0.51F),
            Map.entry("magma_cube", 0.51F),
            Map.entry("guardian", 0.85F),
            Map.entry("elder_guardian", 1.9975F),
            Map.entry("creaking", 2.7F),
            Map.entry("ghast", 4.0F),
            Map.entry("dolphin", 0.6F),
            Map.entry("piglin", 1.95F),
            Map.entry("piglin_brute", 2.16F),
            Map.entry("panda", 1.25F),
            Map.entry("ender_dragon", 4.0F),
            Map.entry("wither", 3.5F),
            Map.entry("shulker", 1.0F),
            Map.entry("armor_stand", 1.975F),
            Map.entry("phantom", 0.5F),
            Map.entry("turtle", 0.4F),
            Map.entry("frog", 0.5F),
            Map.entry("allay", 0.6F),
            Map.entry("vex", 0.8F),
            Map.entry("warden", 2.9F),
            Map.entry("sniffer", 1.75F),
            Map.entry("armadillo", 0.65F)
    );

    private static final Map<String, Float> WIDTH_OVERRIDES = Map.ofEntries(
            Map.entry("fox", 0.6F),
            Map.entry("wolf", 0.6F),
            Map.entry("slime", 0.51F),
            Map.entry("magma_cube", 0.51F),
            Map.entry("armor_stand", 0.5F),
            Map.entry("phantom", 0.9F),
            Map.entry("warden", 0.9F),
            Map.entry("iron_golem", 1.4F),
            Map.entry("ghast", 4.0F),
            Map.entry("ender_dragon", 4.0F),
            Map.entry("wither", 0.9F)
    );

    private NpcMobDimensions() {
    }

    static float height(String entityId, EntityType type) {
        String key = normalize(entityId);
        Float override = HEIGHT_OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        if (type != null && type.isAlive()) {
            return DEFAULT_ALIVE_HEIGHT;
        }
        return DEFAULT_OTHER_HEIGHT;
    }

    static float width(String entityId, EntityType type) {
        String key = normalize(entityId);
        Float override = WIDTH_OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        if (type != null && type.isAlive()) {
            return DEFAULT_ALIVE_WIDTH;
        }
        return DEFAULT_OTHER_WIDTH;
    }

    private static String normalize(String entityId) {
        return entityId.trim().toLowerCase(Locale.ROOT);
    }
}
