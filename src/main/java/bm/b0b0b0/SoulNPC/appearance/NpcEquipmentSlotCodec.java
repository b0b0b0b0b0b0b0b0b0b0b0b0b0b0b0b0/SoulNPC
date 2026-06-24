package bm.b0b0b0.SoulNPC.appearance;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.model.NpcEquipmentSlotData;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public final class NpcEquipmentSlotCodec {

    private NpcEquipmentSlotCodec() {
    }

    public static NpcEquipmentSlotData fromItemStack(
            ItemStack stack,
            NamespacedKey hintKey,
            PluginConfig pluginConfig
    ) {
        if (stack == null || stack.getType().isAir() || isHint(stack, hintKey)) {
            return empty();
        }
        NpcEquipmentSlotData slot = new NpcEquipmentSlotData();
        slot.material = stack.getType();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return slot;
        }
        if (pluginConfig.settings().appearance.allowCustomModelData && meta.hasCustomModelData()) {
            slot.customModelData = meta.getCustomModelData();
        }
        if (meta instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null) {
            String owner = skullMeta.getOwningPlayer().getName();
            if (owner != null && !owner.isBlank()) {
                slot.headTexture = owner;
            }
        }
        return slot;
    }

    public static NpcEquipmentSlotData empty() {
        return new NpcEquipmentSlotData();
    }

    public static boolean isHint(ItemStack stack, NamespacedKey hintKey) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(hintKey, PersistentDataType.BYTE);
    }

    public static void markHint(ItemStack stack, NamespacedKey hintKey) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(hintKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
    }
}
