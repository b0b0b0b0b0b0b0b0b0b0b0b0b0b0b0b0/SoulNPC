package bm.b0b0b0.SoulNPC.listener;

import bm.b0b0b0.SoulNPC.hologram.NpcTextLabels;
import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public final class NpcHologramListener implements Listener {

    private final NpcSpawnService spawnService;
    private final NpcTextLabels textLabels;

    public NpcHologramListener(NpcSpawnService spawnService, NpcTextLabels textLabels) {
        this.spawnService = spawnService;
        this.textLabels = textLabels;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        textLabels.purgeChunk(event.getWorld(), chunkX, chunkZ);
        textLabels.refreshChunk(
                event.getWorld(),
                chunkX,
                chunkZ,
                spawnService.runtimes()
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (textLabels.isPluginDisplay(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (textLabels.isPluginDisplay(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
}
