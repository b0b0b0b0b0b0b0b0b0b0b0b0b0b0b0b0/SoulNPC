package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.util.SoulNpcPermissionChecks;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.function.BiConsumer;

final class GuiInventorySupport {

    private GuiInventorySupport() {
    }

    static Player guardEditPlayer(InventoryClickEvent event, PluginConfig pluginConfig, MessageService messageService) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return null;
        }
        if (!SoulNpcPermissionChecks.hasEditGui(player, pluginConfig)) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(messageService.message(player, "command.no-permission"));
            return null;
        }
        return player;
    }

    static Player guardEditClick(InventoryClickEvent event, PluginConfig pluginConfig, MessageService messageService) {
        event.setCancelled(true);
        return guardEditPlayer(event, pluginConfig, messageService);
    }

    static boolean isClickedHolder(InventoryClickEvent event, InventoryHolder holder) {
        return event.getClickedInventory() != null
                && event.getClickedInventory().getHolder(false) == holder;
    }

    static void dispatchEditClick(
            InventoryClickEvent event,
            InventoryHolder holder,
            PluginConfig pluginConfig,
            MessageService messageService,
            BiConsumer<Player, GuiClickContext> handler
    ) {
        Player player = guardEditClick(event, pluginConfig, messageService);
        if (player == null || !isClickedHolder(event, holder)) {
            return;
        }
        handler.accept(player, GuiClickContext.from(event));
    }
}
