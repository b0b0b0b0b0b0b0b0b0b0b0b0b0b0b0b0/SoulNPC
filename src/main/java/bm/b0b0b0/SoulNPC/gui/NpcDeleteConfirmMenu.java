package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.util.SoulNpcPermissionChecks;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class NpcDeleteConfirmMenu extends AbstractNpcEditMenu {

    public NpcDeleteConfirmMenu(NpcEditGuiDependencies deps, String npcId) {
        super(
                deps,
                npcId,
                deps.pluginConfig().guiNpcEdit().deleteConfirmMenu.size,
                deps.messageService().guiTitle("gui.delete-confirm-title", Placeholder.parsed("npc", npcId.toLowerCase(Locale.ROOT)))
        );
        render(null);
    }

    @Override
    public void handleInventoryClick(Player player, GuiClickContext click) {
        GuiNpcEditSettings.DeleteConfirmLayout layout = pluginConfig().guiNpcEdit().deleteConfirmMenu;
        if (repository().findById(npcId).isEmpty()) {
            player.closeInventory();
            player.sendMessage(messageService().message(player, "command.delete-missing", Placeholder.parsed("npc", npcId)));
            menus().openAdmin(player);
            return;
        }
        int slot = click.slot();
        if (slot == layout.backSlot || slot == layout.noSlot) {
            menus().openEdit(player, npcId);
            return;
        }
        if (slot == layout.yesSlot) {
            if (!SoulNpcPermissionChecks.requireDelete(player, pluginConfig(), messageService())) {
                return;
            }
            if (npcService().delete(npcId)) {
                player.sendMessage(messageService().message(player, "command.delete-success", Placeholder.parsed("npc", npcId)));
            }
            menus().openAdmin(player);
        }
    }

    @Override
    protected void render(Player player) {
        inventory.clear();
        GuiNpcEditSettings.DeleteConfirmLayout layout = pluginConfig().guiNpcEdit().deleteConfirmMenu;
        fillPane(layout.size, GuiMenuItems.pane());
        inventory.setItem(layout.infoSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.infoMaterial,
                "gui.delete-confirm.info-name",
                "gui.delete-confirm.info-lore",
                null,
                Placeholder.parsed("npc", npcId)
        ));
        inventory.setItem(layout.yesSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.yesMaterial,
                "gui.delete-confirm.yes-name",
                "gui.delete-confirm.yes-lore",
                null
        ));
        inventory.setItem(layout.noSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.noMaterial,
                "gui.delete-confirm.no-name",
                "gui.delete-confirm.no-lore",
                null
        ));
        inventory.setItem(layout.backSlot, GuiMenuItems.back(messageService(), player, layout.backMaterial));
    }
}
