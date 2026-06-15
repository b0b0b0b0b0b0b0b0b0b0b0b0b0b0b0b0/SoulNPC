package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdminNpcMenuListener implements Listener {

    private final PluginConfig pluginConfig;
    private final MessageService messageService;
    private final NpcRepository repository;
    private final NpcService npcService;
    private final JavaPlugin plugin;
    private GuiChatInputService chatInputService;

    public AdminNpcMenuListener(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            MessageService messageService,
            NpcRepository repository,
            NpcService npcService
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.messageService = messageService;
        this.repository = repository;
        this.npcService = npcService;
    }

    public void setChatInputService(GuiChatInputService chatInputService) {
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder(false) instanceof NpcEditMenu editMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            if (event.getClickedInventory() == null || event.getClickedInventory().getHolder(false) != editMenu) {
                return;
            }
            editMenu.handleClick(player, event.getSlot(), event.isLeftClick(), event.isShiftClick());
            return;
        }
        if (!(event.getInventory().getHolder(false) instanceof AdminNpcMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder(false) != menu) {
            return;
        }
        menu.handleClick(player, event.getSlot(), event.isRightClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof AdminNpcMenu
                || event.getInventory().getHolder(false) instanceof NpcEditMenu) {
            event.setCancelled(true);
        }
    }

    public void openAdmin(Player player) {
        new AdminNpcMenu(plugin, pluginConfig, messageService, repository, npcService, this).open(player);
    }

    public void openEdit(Player player, String npcId) {
        new NpcEditMenu(pluginConfig, messageService, repository, npcService, this, chatInputService, npcId).open(player);
    }
}
