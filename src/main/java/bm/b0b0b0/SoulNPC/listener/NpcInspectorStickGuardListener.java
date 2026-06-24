package bm.b0b0b0.SoulNPC.listener;

import bm.b0b0b0.SoulNPC.util.NpcInspectorStick;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class NpcInspectorStickGuardListener implements Listener {

    private final JavaPlugin plugin;
    private final SoulNpcKeys keys;

    public NpcInspectorStickGuardListener(JavaPlugin plugin, SoulNpcKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (!NpcInspectorStick.isInspectorStick(dropped, keys)) {
            return;
        }
        event.setCancelled(true);
        event.getItemDrop().remove();
        plugin.getServer().getScheduler().runTask(plugin, () -> NpcInspectorStick.purgeFromPlayer(event.getPlayer(), keys));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!(event.getEntity() instanceof Item item)) {
            return;
        }
        removeGroundLater(item);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(stack -> NpcInspectorStick.isInspectorStick(stack, keys));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack stack = event.getItem().getItemStack();
            if (NpcInspectorStick.isInspectorStick(stack, keys)) {
                event.setCancelled(true);
                removeGroundLater(event.getItem());
                plugin.getServer().getScheduler().runTask(plugin, () -> NpcInspectorStick.purgeFromPlayer(player, keys));
            }
            return;
        }
        removeGroundLater(event.getItem());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        schedulePurge(event.getView());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        schedulePurge(event.getView());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        schedulePurge(event.getView());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            NpcInspectorStick.purgeUnallowedLocations(event.getSource(), keys);
            NpcInspectorStick.purgeUnallowedLocations(event.getDestination(), keys);
        });
    }

    private void removeGroundLater(Item item) {
        NpcInspectorStick.removeGroundItem(item, keys);
        plugin.getServer().getScheduler().runTask(plugin, () -> NpcInspectorStick.removeGroundItem(item, keys));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> NpcInspectorStick.removeGroundItem(item, keys), 1L);
    }

    private void schedulePurge(InventoryView view) {
        plugin.getServer().getScheduler().runTask(plugin, () -> purgeView(view));
    }

    private void purgeView(InventoryView view) {
        if (view == null) {
            return;
        }
        purgeIfUnallowed(view.getTopInventory());
        if (view.getType() != InventoryType.CRAFTING) {
            purgeIfUnallowed(view.getBottomInventory());
        }
    }

    private void purgeIfUnallowed(Inventory inventory) {
        NpcInspectorStick.purgeUnallowedLocations(inventory, keys);
    }
}
