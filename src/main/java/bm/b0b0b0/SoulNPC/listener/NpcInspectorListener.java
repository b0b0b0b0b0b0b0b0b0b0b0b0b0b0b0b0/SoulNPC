package bm.b0b0b0.SoulNPC.listener;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import bm.b0b0b0.SoulNPC.util.NpcInspectorStick;
import bm.b0b0b0.SoulNPC.util.NpcInteractionRaycast;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import bm.b0b0b0.SoulNPC.util.SoulNpcPermissionChecks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public final class NpcInspectorListener implements Listener {

    private record Deps(
            NpcSpawnService spawnService,
            PluginConfig pluginConfig,
            MessageService messageService,
            SoulNpcKeys keys
    ) {
    }

    private final Deps deps;

    public NpcInspectorListener(
            NpcSpawnService spawnService,
            PluginConfig pluginConfig,
            MessageService messageService,
            SoulNpcKeys keys
    ) {
        this.deps = new Deps(spawnService, pluginConfig, messageService, keys);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        if (!NpcInspectorStick.isHoldingInspectorStick(player, deps.keys())) {
            return;
        }
        if (!SoulNpcPermissionChecks.hasAdmin(player, deps.pluginConfig())) {
            return;
        }
        NpcInteractionRaycast.findTargeted(player, deps.spawnService().runtimes()).ifPresent(runtime ->
                NpcInspectorStick.showNpcInfo(player, runtime, deps.messageService())
        );
    }
}
