package bm.b0b0b0.SoulNPC.config.serializer;

import net.elytrium.serializer.custom.ClassSerializer;
import org.bukkit.Material;

import java.util.Locale;

public final class MaterialYamlSerializer extends ClassSerializer<Material, String> {

    @SuppressWarnings("unchecked")
    public MaterialYamlSerializer() {
        super(Material.class, (Class<String>) (Class<?>) String.class);
    }

    @Override
    public String serialize(Material from) {
        if (from == null || from.isAir()) {
            return "AIR";
        }
        return from.name();
    }

    @Override
    public Material deserialize(String from) {
        if (from == null || from.isBlank()) {
            return Material.AIR;
        }
        Material material = Material.matchMaterial(from.trim().toUpperCase(Locale.ROOT));
        return material == null ? Material.AIR : material;
    }
}
