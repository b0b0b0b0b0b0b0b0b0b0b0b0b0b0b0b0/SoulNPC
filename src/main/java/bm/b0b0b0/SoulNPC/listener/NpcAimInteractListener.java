package bm.b0b0b0.SoulNPC.listener;

import bm.b0b0b0.SoulNPC.hologram.NpcTextLabels;
import bm.b0b0b0.SoulNPC.model.NpcClickType;
import bm.b0b0b0.SoulNPC.service.NpcInteractionService;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import bm.b0b0b0.SoulNPC.util.NpcInteractionRaycast;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class NpcAimInteractListener implements Listener {

    private final NpcSpawnService spawnService;
    private final NpcInteractionService interactionService;
    private final NpcTextLabels textLabels;
    private final SoulNpcKeys keys;

    public NpcAimInteractListener(
            NpcSpawnService spawnService,
            NpcInteractionService interactionService,
            NpcTextLabels textLabels,
            SoulNpcKeys keys
    ) {
        this.spawnService = spawnService;
        this.interactionService = interactionService;
        this.textLabels = textLabels;
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        NpcInteractionRaycast.findTargeted(player, spawnService.runtimes()).ifPresent(runtime ->
                interactionService.handleClick(
                        player,
                        runtime,
                        player.isSneaking() ? NpcClickType.SHIFT_LEFT : NpcClickType.LEFT
                )
        );
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        resolveRuntime(event.getEntity()).ifPresent(runtime -> {
            event.setCancelled(true);
            interactionService.handleClick(
                    player,
                    runtime,
                    player.isSneaking() ? NpcClickType.SHIFT_LEFT : NpcClickType.LEFT
            );
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        NpcInteractionRaycast.findTargeted(player, spawnService.runtimes()).ifPresent(runtime -> {
            if (!runtime.data().appearance.isPacketMob()) {
                return;
            }
            event.setCancelled(true);
            interactionService.handleClick(
                    player,
                    runtime,
                    player.isSneaking() ? NpcClickType.SHIFT_RIGHT : NpcClickType.RIGHT
            );
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!textLabels.isPluginDisplay(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        resolveRuntime(event.getRightClicked()).ifPresent(runtime -> {
            if (runtime.data().appearance.isPacketMob()
                    && NpcInteractionRaycast.findTargeted(player, java.util.List.of(runtime)).isEmpty()) {
                return;
            }
            interactionService.handleClick(
                    player,
                    runtime,
                    player.isSneaking() ? NpcClickType.SHIFT_RIGHT : NpcClickType.RIGHT
            );
        });
    }

    private Optional<NpcRuntime> resolveRuntime(Entity entity) {
        if (!textLabels.isPluginDisplay(entity)) {
            return Optional.empty();
        }
        String npcId = entity.getPersistentDataContainer().get(keys.npcId, PersistentDataType.STRING);
        if (npcId == null || npcId.isBlank()) {
            return Optional.empty();
        }
        return spawnService.findRuntime(npcId);
    }
}
