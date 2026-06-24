package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class NpcLineDeleteConfirmMenu extends AbstractNpcEditMenu {

    private final int lineIndex;

    public NpcLineDeleteConfirmMenu(NpcEditGuiDependencies deps, String npcId, int lineIndex) {
        super(
                deps,
                npcId,
                deps.pluginConfig().guiNpcEdit().lineDeleteConfirmMenu.size,
                deps.messageService().guiTitle(
                        "gui.line-delete-title",
                        Placeholder.parsed("npc", npcId.toLowerCase(Locale.ROOT)),
                        Placeholder.parsed("line", String.valueOf(lineIndex + 1))
                )
        );
        this.lineIndex = lineIndex;
        render(null);
    }

    @Override
    public void handleInventoryClick(Player player, GuiClickContext click) {
        GuiNpcEditSettings.LineDeleteConfirmLayout layout = pluginConfig().guiNpcEdit().lineDeleteConfirmMenu;
        NpcFileData data = requireNpc(player);
        if (data == null) {
            menus().openAdmin(player);
            return;
        }
        int slot = click.slot();
        if (slot == layout.backSlot || slot == layout.noSlot) {
            menus().openLines(player, npcId);
            return;
        }
        if (slot == layout.yesSlot) {
            NpcHologramLines.clearLine(data.appearance, lineIndex);
            data.appearance.normalizePresentation();
            saveAndRefresh(data);
            player.sendMessage(messageService().message(
                    player,
                    "gui.line-delete.deleted",
                    Placeholder.parsed("line", String.valueOf(lineIndex + 1))
            ));
            menus().openLines(player, npcId);
        }
    }

    @Override
    protected void render(Player player) {
        inventory.clear();
        GuiNpcEditSettings.LineDeleteConfirmLayout layout = pluginConfig().guiNpcEdit().lineDeleteConfirmMenu;
        fillPane(layout.size, GuiMenuItems.pane());
        inventory.setItem(layout.infoSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.infoMaterial,
                "gui.line-delete.info-name",
                "gui.line-delete.info-lore",
                null,
                Placeholder.parsed("line", String.valueOf(lineIndex + 1))
        ));
        inventory.setItem(layout.yesSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.yesMaterial,
                "gui.line-delete.yes-name",
                "gui.line-delete.yes-lore",
                null
        ));
        inventory.setItem(layout.noSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.noMaterial,
                "gui.line-delete.no-name",
                "gui.line-delete.no-lore",
                null
        ));
        inventory.setItem(layout.backSlot, GuiMenuItems.back(messageService(), player, layout.backMaterial));
    }
}
