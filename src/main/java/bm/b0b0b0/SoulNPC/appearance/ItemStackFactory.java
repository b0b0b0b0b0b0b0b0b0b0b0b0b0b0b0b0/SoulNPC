package bm.b0b0b0.SoulNPC.appearance;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.model.NpcEquipmentSlotData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.UUID;

public final class ItemStackFactory {

    private final PluginConfig pluginConfig;
    private final JavaPlugin plugin;
    private Boolean itemsAdderAvailable;
    private Boolean nexoAvailable;

    public ItemStackFactory(JavaPlugin plugin, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
    }

    public ItemStack create(NpcEquipmentSlotData slot) {
        if (slot == null || slot.isEmpty()) {
            return ItemStack.empty();
        }
        ItemStack custom = tryCustomPluginItem(slot);
        if (!custom.isEmpty()) {
            return custom;
        }
        Material material = slot.material == null ? Material.AIR : slot.material;
        if (material.isAir()) {
            return ItemStack.empty();
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (pluginConfig.settings().appearance.allowCustomModelData && slot.customModelData > 0) {
            meta.setCustomModelData(slot.customModelData);
        }
        if (meta instanceof SkullMeta skullMeta && slot.headTexture != null && !slot.headTexture.isBlank()) {
            applyTexture(skullMeta, slot.headTexture);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack tryCustomPluginItem(NpcEquipmentSlotData slot) {
        String itemsAdderPrefix = pluginConfig.settings().appearance.itemsAdderPrefix;
        if (slot.itemsAdderId != null && !slot.itemsAdderId.isBlank() && itemsAdderPrefix != null) {
            ItemStack item = tryPluginItem("dev.lone.itemsadder.api.CustomStack", "getInstance", slot.itemsAdderId);
            if (!item.isEmpty()) {
                return item;
            }
        }
        if (slot.nexoId != null && !slot.nexoId.isBlank()) {
            ItemStack item = tryPluginItem("com.nexomc.nexo.api.NexoItems", "itemFromId", slot.nexoId);
            if (!item.isEmpty()) {
                return item;
            }
        }
        return ItemStack.empty();
    }

    private ItemStack tryPluginItem(String className, String factoryMethod, String id) {
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(factoryMethod, String.class);
            Object result = method.invoke(null, id);
            if (result instanceof ItemStack item) {
                return item.clone();
            }
            Method build = result.getClass().getMethod("getItemStack");
            Object built = build.invoke(result);
            if (built instanceof ItemStack item) {
                return item.clone();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return ItemStack.empty();
    }

    private void applyTexture(SkullMeta meta, String texture) {
        if (texture.length() <= 16 && !texture.startsWith("ey")) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(texture));
            return;
        }
        String value = texture;
        if (!texture.startsWith("ey")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + texture + "\"}}}";
            value = Base64.getEncoder().encodeToString(json.getBytes());
        }
        UUID uuid = UUID.nameUUIDFromBytes(value.getBytes());
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
    }
}
