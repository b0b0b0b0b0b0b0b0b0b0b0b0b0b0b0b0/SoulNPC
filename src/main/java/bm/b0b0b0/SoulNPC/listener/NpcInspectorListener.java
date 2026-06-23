package bm.b0b0b0.SoulNPC.listener;

import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import bm.b0b0b0.SoulNPC.util.NpcInspectorStick;
import bm.b0b0b0.SoulNPC.util.NpcInteractionRaycast;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public final class NpcInspectorListener implements Listener {

    private final NpcSpawnService spawnService;
    private final MessageService messageService;
    private final SoulNpcKeys keys;

    public NpcInspectorListener(NpcSpawnService spawnService, MessageService messageService, SoulNpcKeys keys) {
        this.spawnService = spawnService;
        this.messageService = messageService;
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        if (!NpcInspectorStick.isHoldingInspectorStick(player, keys)) {
            return;
        }
        NpcInteractionRaycast.findTargeted(player, spawnService.runtimes()).ifPresent(runtime ->
                NpcInspectorStick.showNpcInfo(player, runtime, messageService)
        );
    }
}
