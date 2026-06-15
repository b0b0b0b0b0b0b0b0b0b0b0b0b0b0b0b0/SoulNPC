package bm.b0b0b0.SoulNPC.listener;

import bm.b0b0b0.SoulNPC.effect.NpcGroundItemEffectService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

public final class NpcGroundItemListener implements Listener {

    private final NpcGroundItemEffectService groundItemEffectService;

    public NpcGroundItemListener(NpcGroundItemEffectService groundItemEffectService) {
        this.groundItemEffectService = groundItemEffectService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (isDecorItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPickup(PlayerAttemptPickupItemEvent event) {
        if (isDecorItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    private boolean isDecorItem(Item item) {
        Entity entity = item;
        return groundItemEffectService.isPluginGroundItem(entity);
    }
}
