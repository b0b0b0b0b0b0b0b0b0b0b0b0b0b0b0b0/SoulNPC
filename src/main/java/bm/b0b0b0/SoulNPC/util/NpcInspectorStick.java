package bm.b0b0b0.SoulNPC.util;

import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class NpcInspectorStick {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final String LORE_MARKER = "soulnpc:inspector-stick";

    private NpcInspectorStick() {
    }

    public static boolean isInspectorStick(ItemStack item, SoulNpcKeys keys) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(keys.inspectorStick, PersistentDataType.STRING)) {
            return "1".equals(pdc.get(keys.inspectorStick, PersistentDataType.STRING));
        }
        if (pdc.has(keys.inspectorStick, PersistentDataType.BYTE)) {
            Byte legacy = pdc.get(keys.inspectorStick, PersistentDataType.BYTE);
            return legacy != null && legacy == (byte) 1;
        }
        if (meta.lore() == null) {
            return false;
        }
        for (Component line : meta.lore()) {
            if (PLAIN.serialize(line).contains(LORE_MARKER)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHoldingInspectorStick(Player player, SoulNpcKeys keys) {
        return isInspectorStick(player.getInventory().getItemInMainHand(), keys);
    }

    public static void purgeFrom(Inventory inventory, SoulNpcKeys keys) {
        if (inventory == null) {
            return;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isInspectorStick(stack, keys)) {
                inventory.setItem(slot, null);
            }
        }
    }

    public static void purgeUnallowedLocations(Inventory inventory, SoulNpcKeys keys) {
        if (inventory == null || isAllowedStorage(inventory)) {
            return;
        }
        purgeFrom(inventory, keys);
    }

    public static boolean isAllowedStorage(Inventory inventory) {
        if (!(inventory.getHolder() instanceof Player)) {
            return false;
        }
        return switch (inventory.getType()) {
            case PLAYER, CREATIVE -> true;
            default -> false;
        };
    }

    public static void purgeFromPlayer(Player player, SoulNpcKeys keys) {
        purgeFrom(player.getInventory(), keys);
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isInspectorStick(offHand, keys)) {
            player.getInventory().setItemInOffHand(null);
        }
        ItemStack cursor = player.getItemOnCursor();
        if (isInspectorStick(cursor, keys)) {
            player.setItemOnCursor(null);
        }
    }

    public static void removeGroundItem(Item entity, SoulNpcKeys keys) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (isInspectorStick(entity.getItemStack(), keys)) {
            entity.remove();
        }
    }

    public static ItemStack create(MessageService messageService, Player player, SoulNpcKeys keys) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messageService.message(player, "command.stick-item-name"));
        List<Component> lore = new ArrayList<>(messageService.messageList(player, "command.stick-item-lore"));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(keys.inspectorStick, PersistentDataType.STRING, "1");
        item.setItemMeta(meta);
        return item;
    }

    public static void showNpcInfo(Player player, NpcRuntime runtime, MessageService messageService) {
        NpcFileData data = runtime.data();
        String type = data.appearance.type == null ? "PLAYER" : data.appearance.type.name();
        if (data.appearance.isPacketMob()) {
            type = type + " (" + data.appearance.resolvedEntityType() + ")";
        }
        player.sendMessage(messageService.message(
                player,
                "command.inspector-hit",
                Placeholder.parsed("npc_id", data.id),
                Placeholder.parsed("npc_type", type)
        ));
    }
}
