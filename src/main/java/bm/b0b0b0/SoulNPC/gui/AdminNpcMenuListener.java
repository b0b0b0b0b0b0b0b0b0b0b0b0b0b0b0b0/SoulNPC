package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcService;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import bm.b0b0b0.SoulNPC.util.SoulNpcPermissionChecks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public final class AdminNpcMenuListener implements Listener {

    private final NpcEditGuiDependencies editGuiDeps;
    private GuiChatInputService chatInputService;

    public AdminNpcMenuListener(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            MessageService messageService,
            NpcRepository repository,
            NpcService npcService,
            ItemStackFactory itemStackFactory,
            SoulNpcKeys soulNpcKeys
    ) {
        this.editGuiDeps = new NpcEditGuiDependencies(
                plugin,
                pluginConfig,
                messageService,
                repository,
                npcService,
                this,
                itemStackFactory,
                soulNpcKeys
        );
    }

    public void setChatInputService(GuiChatInputService chatInputService) {
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder(false) instanceof NpcDressMenu dressMenu) {
            Player player = GuiInventorySupport.guardEditPlayer(
                    event,
                    editGuiDeps.pluginConfig(),
                    editGuiDeps.messageService()
            );
            if (player == null) {
                return;
            }
            handleDressClick(event, dressMenu, player);
            return;
        }
        if (event.getInventory().getHolder(false) instanceof ClickableSoulNpcGui gui) {
            GuiInventorySupport.dispatchEditClick(
                    event,
                    gui,
                    editGuiDeps.pluginConfig(),
                    editGuiDeps.messageService(),
                    gui::handleInventoryClick
            );
        }
    }

    private void handleDressClick(InventoryClickEvent event, NpcDressMenu dressMenu, Player player) {
        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < topSize) {
            if (dressMenu.isEquipmentSlot(rawSlot)) {
                dressMenu.handleEquipmentClick(player, event);
                return;
            }
            event.setCancelled(true);
            if (GuiInventorySupport.isClickedHolder(event, dressMenu)) {
                dressMenu.handleInventoryClick(player, GuiClickContext.from(event));
            }
            return;
        }
        if (event.isShiftClick() && event.getClickedInventory() != null
                && event.getClickedInventory().getHolder(false) != dressMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder(false) instanceof NpcDressMenu dressMenu && !dressMenu.shouldSkipCloseSave()) {
            if (!SoulNpcPermissionChecks.hasEditGui(player, editGuiDeps.pluginConfig())) {
                return;
            }
            dressMenu.saveFromInventory();
            player.sendMessage(editGuiDeps.messageService().message(player, "gui.dress.saved"));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof NpcDressMenu dressMenu) {
            if (!(event.getWhoClicked() instanceof Player player)) {
                event.setCancelled(true);
                return;
            }
            if (!SoulNpcPermissionChecks.hasEditGui(player, editGuiDeps.pluginConfig())) {
                event.setCancelled(true);
                return;
            }
            int topSize = event.getView().getTopInventory().getSize();
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot >= 0 && rawSlot < topSize && !dressMenu.isEquipmentSlot(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        if (event.getInventory().getHolder(false) instanceof SoulNpcGui) {
            event.setCancelled(true);
        }
    }

    public void openAdmin(Player player) {
        openWithEditPermission(player, p -> new AdminNpcMenu(editGuiDeps).open(p));
    }

    public void openEdit(Player player, String npcId) {
        openWithEditPermission(player, p -> new NpcEditMenu(editGuiDeps, chatInputService, npcId).open(p));
    }

    public void openGroundItems(Player player, String npcId) {
        openWithEditPermission(player, p -> new NpcGroundItemsMenu(editGuiDeps, npcId).open(p));
    }

    public void openDress(Player player, String npcId) {
        openWithEditPermission(player, p -> new NpcDressMenu(editGuiDeps, npcId).open(p));
    }

    public void openGlow(Player player, String npcId) {
        openWithEditPermission(player, p -> new NpcGlowMenu(editGuiDeps, npcId).open(p));
    }

    public void openActions(Player player, String npcId) {
        openWithEditPermission(player, p -> new NpcActionsMenu(editGuiDeps, chatInputService, npcId).open(p));
    }

    public void openLines(Player player, String npcId) {
        openWithEditPermission(player, p -> new NpcLinesMenu(editGuiDeps, chatInputService, npcId).open(p));
    }

    public void openLineDeleteConfirm(Player player, String npcId, int lineIndex) {
        openWithEditPermission(player, p -> new NpcLineDeleteConfirmMenu(editGuiDeps, npcId, lineIndex).open(p));
    }

    public void openDeleteConfirm(Player player, String npcId) {
        if (!SoulNpcPermissionChecks.requireDelete(player, editGuiDeps.pluginConfig(), editGuiDeps.messageService())) {
            return;
        }
        new NpcDeleteConfirmMenu(editGuiDeps, npcId).open(player);
    }

    private void openWithEditPermission(Player player, Consumer<Player> opener) {
        if (!SoulNpcPermissionChecks.requireEditGui(player, editGuiDeps.pluginConfig(), editGuiDeps.messageService())) {
            return;
        }
        opener.accept(player);
    }
}
