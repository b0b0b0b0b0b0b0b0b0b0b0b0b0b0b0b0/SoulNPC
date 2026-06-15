package bm.b0b0b0.SoulNPC.listener;

import bm.b0b0b0.SoulNPC.model.NpcClickType;
import bm.b0b0b0.SoulNPC.service.NpcInteractionService;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import bm.b0b0b0.SoulNPC.util.NpcInteractionRaycast;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItemFromEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public final class PacketNpcInteractListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final NpcSpawnService spawnService;
    private final NpcInteractionService interactionService;

    public PacketNpcInteractListener(Plugin plugin, NpcSpawnService spawnService, NpcInteractionService interactionService) {
        this.plugin = plugin;
        this.spawnService = spawnService;
        this.interactionService = interactionService;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteractEntity(event, player);
            return;
        }
        if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM_FROM_ENTITY) {
            handlePickItemFromEntity(event, player);
        }
    }

    private void handleInteractEntity(PacketReceiveEvent event, Player player) {
        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        WrapperPlayClientInteractEntity.InteractAction action = packet.getAction();
        if (action != WrapperPlayClientInteractEntity.InteractAction.ATTACK
                && action != WrapperPlayClientInteractEntity.InteractAction.INTERACT
                && action != WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
            return;
        }
        NpcClickType clickType = resolveClick(action, player.isSneaking());
        Optional<NpcRuntime> runtime = resolveRuntime(player, packet.getEntityId(), clickType);
        runtime.ifPresent(found -> dispatchClick(event, player, found, clickType));
    }

    private void handlePickItemFromEntity(PacketReceiveEvent event, Player player) {
        WrapperPlayClientPickItemFromEntity packet = new WrapperPlayClientPickItemFromEntity(event);
        Optional<NpcRuntime> runtime = resolveRuntime(player, packet.getEntityId(), NpcClickType.MIDDLE);
        runtime.ifPresent(found -> dispatchClick(event, player, found, NpcClickType.MIDDLE));
    }

    private Optional<NpcRuntime> resolveRuntime(Player player, int entityId, NpcClickType clickType) {
        Optional<NpcRuntime> runtime = spawnService.findByEntityId(entityId);
        if (runtime.isPresent()) {
            return runtime;
        }
        if (clickType == NpcClickType.LEFT || clickType == NpcClickType.SHIFT_LEFT) {
            return Optional.empty();
        }
        return NpcInteractionRaycast.findTargeted(player, spawnService.runtimes());
    }

    private void dispatchClick(PacketReceiveEvent event, Player player, NpcRuntime runtime, NpcClickType clickType) {
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () ->
                interactionService.handleClick(player, runtime, clickType)
        );
    }

    private static NpcClickType resolveClick(
            WrapperPlayClientInteractEntity.InteractAction action,
            boolean sneaking
    ) {
        return switch (action) {
            case ATTACK -> sneaking ? NpcClickType.SHIFT_LEFT : NpcClickType.LEFT;
            case INTERACT, INTERACT_AT -> sneaking ? NpcClickType.SHIFT_RIGHT : NpcClickType.RIGHT;
        };
    }
}
