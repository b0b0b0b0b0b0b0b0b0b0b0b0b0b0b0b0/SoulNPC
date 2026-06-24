package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.lang.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class GuiMenuItems {

    private GuiMenuItems() {
    }

    static ItemStack pane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack back(MessageService messageService, Player player, Material material) {
        return action(messageService, player, material, "gui.back-name", "gui.back-lore", null);
    }

    static ItemStack action(
            MessageService messageService,
            Player player,
            Material material,
            String namePath,
            String lorePath,
            Float value,
            TagResolver... resolvers
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService.message(player, namePath, resolvers));
        if (lorePath != null) {
            List<Component> lore = new ArrayList<>();
            for (String line : messageService.plainList(player, lorePath)) {
                String resolved = line;
                if (value != null) {
                    resolved = resolved.replace("{value}", String.format(Locale.ROOT, "%.2f", value));
                }
                lore.add(messageService.raw(bracesToMiniMessage(resolved), resolvers));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack toggle(
            MessageService messageService,
            Player player,
            Material material,
            String namePath,
            String lorePath,
            boolean enabled
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService.message(
                player,
                namePath,
                Placeholder.component("state", messageService.message(player, enabled ? "gui.state-on" : "gui.state-off"))
        ));
        List<Component> lore = new ArrayList<>();
        appendToggleLore(messageService, player, lore, lorePath, enabled);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    static void appendToggleLore(
            MessageService messageService,
            Player player,
            List<Component> lore,
            String lorePath,
            boolean enabled
    ) {
        String stateDescPath = lorePath.replace("-lore", enabled ? "-state-on" : "-state-off");
        String stateDesc = messageService.plain(player, stateDescPath);
        for (String line : messageService.plainList(player, lorePath)) {
            if (line.isBlank()) {
                lore.add(Component.empty());
                continue;
            }
            lore.add(messageService.raw(bracesToMiniMessage(line.replace("{state_desc}", stateDesc))));
        }
    }

    static float step(boolean shiftClick) {
        return shiftClick ? 0.25F : 0.1F;
    }

    static float round(float value) {
        return Math.round(value * 100.0F) / 100.0F;
    }

    static String bracesToMiniMessage(String raw) {
        return raw.replaceAll("\\{([a-zA-Z0-9_-]+)\\}", "<$1>");
    }
}
