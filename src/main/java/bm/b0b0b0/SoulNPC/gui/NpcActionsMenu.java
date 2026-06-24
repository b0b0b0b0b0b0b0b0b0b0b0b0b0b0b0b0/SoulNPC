package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.model.NpcActionType;
import bm.b0b0b0.SoulNPC.model.NpcClickBinding;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcInteractionAction;
import bm.b0b0b0.SoulNPC.model.NpcInteractionActionParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NpcActionsMenu extends AbstractNpcEditMenu {

    private final GuiChatInputService chatInputService;
    private final Map<Integer, Integer> actionSlotIndexes = new HashMap<>();

    public NpcActionsMenu(NpcEditGuiDependencies deps, GuiChatInputService chatInputService, String npcId) {
        super(
                deps,
                npcId,
                deps.pluginConfig().guiNpcEdit().actionsMenu.size,
                deps.messageService().guiTitle("gui.actions-title", Placeholder.parsed("npc", npcId.toLowerCase(Locale.ROOT)))
        );
        this.chatInputService = chatInputService;
        render(null);
    }

    @Override
    public void handleInventoryClick(Player player, GuiClickContext click) {
        GuiNpcEditSettings.ActionsLayout layout = pluginConfig().guiNpcEdit().actionsMenu;
        NpcFileData data = requireNpc(player);
        if (data == null) {
            return;
        }
        data.interaction.ensureActionsMigrated();
        int slot = click.slot();
        if (slot == layout.backSlot) {
            menus().openEdit(player, npcId);
            return;
        }
        if (slot == layout.addSlot) {
            NpcInteractionAction action = new NpcInteractionAction();
            action.click = NpcClickBinding.RIGHT;
            action.type = NpcActionType.MESSAGE;
            action.value = messageService().plain(player, "gui.actions.default-value");
            data.interaction.actions.add(action);
            saveAndRefresh(data);
            render(player);
            return;
        }
        Integer index = actionSlotIndexes.get(slot);
        if (index == null) {
            return;
        }
        if (index >= data.interaction.actions.size()) {
            return;
        }
        NpcInteractionAction action = data.interaction.actions.get(index);
        if (click.shiftClick() && click.rightClick()) {
            data.interaction.actions.remove((int) index);
            saveAndRefresh(data);
            render(player);
            return;
        }
        if (click.leftClick()) {
            action.click = nextBinding(action.click, click.shiftClick());
            saveAndRefresh(data);
            render(player);
            return;
        }
        if (click.rightClick()) {
            action.type = nextType(action.type, click.shiftClick());
            saveAndRefresh(data);
            render(player);
            return;
        }
        if (click.middleClick()) {
            chatInputService.beginActionEdit(player, npcId, index);
        }
    }

    @Override
    protected void render(Player player) {
        inventory.clear();
        actionSlotIndexes.clear();
        GuiNpcEditSettings.ActionsLayout layout = pluginConfig().guiNpcEdit().actionsMenu;
        fillPane(layout.size, GuiMenuItems.pane());
        NpcFileData data = findNpc();
        if (data != null) {
            data.interaction.ensureActionsMigrated();
            int slot = 0;
            for (int index = 0; index < data.interaction.actions.size() && slot < layout.backSlot; index++) {
                if (slot == layout.backSlot || slot == layout.addSlot) {
                    slot++;
                }
                NpcInteractionAction action = data.interaction.actions.get(index);
                actionSlotIndexes.put(slot, index);
                inventory.setItem(slot, actionItem(player, action, index));
                slot++;
            }
        }
        inventory.setItem(layout.backSlot, GuiMenuItems.back(messageService(), player, layout.backMaterial));
        inventory.setItem(layout.addSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.addMaterial,
                "gui.actions.add",
                "gui.actions.add-lore",
                null
        ));
    }

    private ItemStack actionItem(Player player, NpcInteractionAction action, int index) {
        GuiNpcEditSettings.ActionsLayout layout = pluginConfig().guiNpcEdit().actionsMenu;
        ItemStack item = new ItemStack(layout.actionMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messageService().message(
                player,
                "gui.actions.entry-name",
                Placeholder.parsed("index", String.valueOf(index + 1))
        ));
        List<Component> lore = new ArrayList<>();
        lore.add(messageService().message(player, "gui.actions.entry-click", Placeholder.parsed("click", action.click.name())));
        lore.add(messageService().message(player, "gui.actions.entry-type", Placeholder.parsed("type", action.type.name())));
        lore.add(messageService().message(player, "gui.actions.entry-value", Placeholder.parsed("value", action.value)));
        lore.add(messageService().message(player, "gui.actions.entry-delay", Placeholder.parsed("ticks", String.valueOf(action.delayTicks))));
        lore.add(Component.empty());
        for (String line : messageService().plainList(player, "gui.actions.entry-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(line)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static NpcClickBinding nextBinding(NpcClickBinding current, boolean reverse) {
        NpcClickBinding[] values = NpcClickBinding.values();
        int index = current.ordinal();
        index = reverse ? (index - 1 + values.length) % values.length : (index + 1) % values.length;
        return values[index];
    }

    private static NpcActionType nextType(NpcActionType current, boolean reverse) {
        NpcActionType[] values = NpcActionType.values();
        int index = current.ordinal();
        index = reverse ? (index - 1 + values.length) % values.length : (index + 1) % values.length;
        return values[index];
    }
}
