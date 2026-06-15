package bm.b0b0b0.SoulNPC.listener;

import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class PlayerNpcLifecycleListener implements Listener {

    private final NpcSpawnService spawnService;

    public PlayerNpcLifecycleListener(NpcSpawnService spawnService) {
        this.spawnService = spawnService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        spawnService.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spawnService.onPlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }
        spawnService.onPlayerQuit(event.getPlayer());
    }
}
