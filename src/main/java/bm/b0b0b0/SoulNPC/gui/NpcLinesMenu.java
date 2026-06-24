package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NpcLinesMenu extends AbstractNpcEditMenu {

    private final GuiChatInputService chatInputService;
    private final Map<Integer, Integer> lineSlotIndexes = new HashMap<>();

    public NpcLinesMenu(NpcEditGuiDependencies deps, GuiChatInputService chatInputService, String npcId) {
        super(
                deps,
                npcId,
                deps.pluginConfig().guiNpcEdit().linesMenu.size,
                deps.messageService().guiTitle("gui.lines-title", Placeholder.parsed("npc", npcId.toLowerCase(Locale.ROOT)))
        );
        this.chatInputService = chatInputService;
        render(null);
    }

    @Override
    public void handleInventoryClick(Player player, GuiClickContext click) {
        GuiNpcEditSettings.LinesLayout layout = pluginConfig().guiNpcEdit().linesMenu;
        NpcFileData data = requireNpc(player);
        if (data == null) {
            return;
        }
        int slot = click.slot();
        if (slot == layout.backSlot) {
            menus().openEdit(player, npcId);
            return;
        }
        if (slot == layout.nameHideSlot) {
            data.appearance.nameHidden = !data.appearance.nameHidden;
            saveAndRefresh(data);
            render(player);
            return;
        }

        Integer lineIndex = lineSlotIndexes.get(slot);
        ClickType clickType = click.clickType();
        if (lineIndex == null || clickType == null) {
            return;
        }

        NpcAppearanceData appearance = data.appearance;
        if (clickType == ClickType.MIDDLE) {
            if (!NpcHologramLines.hasLineContent(appearance, lineIndex)) {
                return;
            }
            NpcHologramLines.toggleLineHidden(appearance, lineIndex);
            saveAndRefresh(data);
            render(player);
            return;
        }
        if (clickType == ClickType.SHIFT_LEFT && lineIndex > NpcHologramLines.NAME_LINE_INDEX) {
            NpcHologramLines.toggleLineType(appearance, lineIndex);
            saveAndRefresh(data);
            render(player);
            return;
        }
        if (clickType == ClickType.LEFT) {
            if (!NpcHologramLines.hasLineContent(appearance, lineIndex)
                    && NpcHologramLines.lineType(appearance, lineIndex) == bm.b0b0b0.SoulNPC.model.NpcHologramLineType.TEXT) {
                chatInputService.begin(player, npcId, new HologramLineTarget.Line(lineIndex), true);
            }
            return;
        }
        if (clickType == ClickType.RIGHT) {
            if (!NpcHologramLines.hasLineContent(appearance, lineIndex)) {
                return;
            }
            menus().openLineDeleteConfirm(player, npcId, lineIndex);
        }
    }

    @Override
    protected void saveAndRefresh(NpcFileData data) {
        data.appearance.normalizePresentation();
        super.saveAndRefresh(data);
    }

    @Override
    protected void render(Player player) {
        inventory.clear();
        lineSlotIndexes.clear();
        GuiNpcEditSettings.LinesLayout layout = pluginConfig().guiNpcEdit().linesMenu;
        NpcFileData data = findNpc();
        if (data == null) {
            return;
        }
        NpcAppearanceData appearance = data.appearance;
        appearance.normalizePresentation();

        fillPane(layout.size, GuiMenuItems.pane());

        inventory.setItem(layout.nameHideSlot, nameHideButton(player, appearance));
        inventory.setItem(layout.backSlot, GuiMenuItems.back(messageService(), player, layout.backMaterial));

        int[] slots = layout.lineSlots();
        for (int index = 0; index < slots.length && index < NpcHologramLines.MAX_LINES; index++) {
            int slot = slots[index];
            lineSlotIndexes.put(slot, index);
            inventory.setItem(slot, lineFlagItem(player, appearance, index));
        }
    }

    private ItemStack nameHideButton(Player player, NpcAppearanceData appearance) {
        boolean hidden = appearance.nameHidden;
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(
                player,
                "gui.lines.name-hide-name",
                Placeholder.component(
                        "state",
                        messageService().message(player, hidden ? "gui.lines.name-hidden" : "gui.lines.name-visible")
                )
        ));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, "gui.lines.name-hide-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(line)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack lineFlagItem(Player player, NpcAppearanceData appearance, int lineIndex) {
        NpcHologramLines.LineState state = NpcHologramLines.state(appearance, lineIndex);
        String text = NpcHologramLines.lineText(appearance, lineIndex);
        ItemStack item = new ItemStack(NpcHologramLines.flagMaterial(state));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        String stateKey = switch (state) {
            case EMPTY -> "gui.lines.state-empty";
            case HIDDEN -> "gui.lines.state-hidden";
            case ACTIVE -> "gui.lines.state-active";
        };
        meta.displayName(messageService().message(
                player,
                lineIndex == NpcHologramLines.NAME_LINE_INDEX ? "gui.lines.line-name-title" : "gui.lines.line-title",
                Placeholder.parsed("line", String.valueOf(lineIndex + 1)),
                Placeholder.component("state", messageService().message(player, stateKey))
        ));
        List<Component> lore = new ArrayList<>();
        String preview = preview(text, appearance, lineIndex);
        String typeLabel = messageService().plain(
                player,
                NpcHologramLines.lineType(appearance, lineIndex) == bm.b0b0b0.SoulNPC.model.NpcHologramLineType.ITEM
                        ? "gui.lines.type-item"
                        : "gui.lines.type-text"
        );
        for (String line : messageService().plainList(player, "gui.lines.line-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                    line.replace("{text}", preview)
                            .replace("{state}", messageService().plain(player, stateKey))
                            .replace("{type}", typeLabel)
            )));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String preview(String text, NpcAppearanceData appearance, int lineIndex) {
        if (NpcHologramLines.lineType(appearance, lineIndex) == bm.b0b0b0.SoulNPC.model.NpcHologramLineType.ITEM) {
            return text == null || text.isBlank() ? "ITEM" : text;
        }
        if (text == null || text.isBlank()) {
            return "—";
        }
        String singleLine = text.replace('\n', ' ').trim();
        if (singleLine.length() <= 48) {
            return singleLine;
        }
        return singleLine.substring(0, 45) + "...";
    }
}
