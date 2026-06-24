package bm.b0b0b0.SoulNPC.api.event;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class SoulNpcDeleteEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final NpcFileData npc;
    private boolean cancelled;

    public SoulNpcDeleteEvent(@NotNull NpcFileData npc) {
        this.npc = npc;
    }

    public @NotNull NpcFileData getNpc() {
        return npc;
    }

    public @NotNull String getNpcId() {
        return npc.id;
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
