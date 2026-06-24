package bm.b0b0b0.SoulNPC.gui;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public record GuiClickContext(
        int slot,
        boolean leftClick,
        boolean rightClick,
        boolean shiftClick,
        ClickType clickType
) {

    public static GuiClickContext from(InventoryClickEvent event) {
        return new GuiClickContext(
                event.getSlot(),
                event.isLeftClick(),
                event.isRightClick(),
                event.isShiftClick(),
                event.getClick()
        );
    }

    public boolean middleClick() {
        return clickType == ClickType.MIDDLE;
    }
}
