package bm.b0b0b0.SoulNPC.api.event;

import bm.b0b0b0.SoulNPC.model.NpcClickType;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public final class SoulNpcClickEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final NpcFileData npc;
    private final NpcClickType clickType;
    private boolean cancelled;

    public SoulNpcClickEvent(@NotNull Player player, @NotNull NpcFileData npc, @NotNull NpcClickType clickType) {
        super(player, true);
        this.npc = npc;
        this.clickType = clickType;
    }

    public @NotNull NpcFileData getNpc() {
        return npc;
    }

    public @NotNull String getNpcId() {
        return npc.id;
    }

    public @NotNull NpcClickType getClickType() {
        return clickType;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
