package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class NpcGroundItemEntry {

    @Comment(value = {
            @CommentValue(" Материалы на выброс — каждый раз случайный из списка"),
            @CommentValue(" GOLD_INGOT, DIAMOND, EMERALD, …")
    })
    public List<Material> materials = defaultMaterials();

    @Comment(value = {
            @CommentValue(" Количество на дроп (визуально, не выдаётся игроку)")
    })
    public int amount = 1;

    @Deprecated
    public Material material;

    public static List<Material> defaultMaterials() {
        List<Material> list = new ArrayList<>(3);
        list.add(Material.GOLD_INGOT);
        list.add(Material.DIAMOND);
        list.add(Material.EMERALD);
        return list;
    }

    public void ensureMaterials() {
        migrateLegacyMaterial();
        if (materials == null) {
            materials = defaultMaterials();
            return;
        }
        materials.removeIf(material -> material == null || material.isAir());
        if (materials.isEmpty()) {
            materials = defaultMaterials();
        }
    }

    public Material pickRandomMaterial() {
        ensureMaterials();
        if (materials.isEmpty()) {
            return null;
        }
        return materials.get(ThreadLocalRandom.current().nextInt(materials.size()));
    }

    public List<Material> materialsForBurst() {
        ensureMaterials();
        return List.copyOf(materials);
    }

    private void migrateLegacyMaterial() {
        if (material == null || material.isAir()) {
            return;
        }
        if (materials == null) {
            materials = new ArrayList<>();
        }
        if (!materials.contains(material)) {
            materials.add(material);
        }
        material = null;
    }
}
