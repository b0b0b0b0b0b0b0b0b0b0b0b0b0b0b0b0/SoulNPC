package bm.b0b0b0.SoulNPC.gui;

import org.bukkit.entity.Player;

public interface ClickableSoulNpcGui extends SoulNpcGui {

    void handleInventoryClick(Player player, GuiClickContext click);
}
