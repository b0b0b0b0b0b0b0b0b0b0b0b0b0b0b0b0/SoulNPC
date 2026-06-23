package bm.b0b0b0.SoulNPC.util;

import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class NpcInspectorStick {

    private NpcInspectorStick() {
    }

    public static boolean isInspectorStick(ItemStack item, SoulNpcKeys keys) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(keys.inspectorStick, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public static boolean isHoldingInspectorStick(Player player, SoulNpcKeys keys) {
        return isInspectorStick(player.getInventory().getItemInMainHand(), keys);
    }

    public static ItemStack create(MessageService messageService, Player player, SoulNpcKeys keys) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messageService.message(player, "command.stick-item-name"));
        List<Component> lore = messageService.messageList(player, "command.stick-item-lore");
        meta.lore(lore);
        meta.getPersistentDataContainer().set(keys.inspectorStick, PersistentDataType.BYTE, (byte) 1);
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
