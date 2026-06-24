package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.appearance.NpcGlowColors;
import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NpcGlowMenu extends AbstractNpcEditMenu {

    private final Map<Integer, String> colorSlotIds = new HashMap<>();

    public NpcGlowMenu(NpcEditGuiDependencies deps, String npcId) {
        super(
                deps,
                npcId,
                deps.pluginConfig().guiNpcEdit().glowMenu.size,
                deps.messageService().guiTitle("gui.glow-title", Placeholder.parsed("npc", npcId.toLowerCase(Locale.ROOT)))
        );
        render(null);
    }

    @Override
    public void handleInventoryClick(Player player, GuiClickContext click) {
        GuiNpcEditSettings.GlowLayout layout = pluginConfig().guiNpcEdit().glowMenu;
        NpcFileData data = requireNpc(player);
        if (data == null) {
            return;
        }
        int slot = click.slot();
        if (slot == layout.backSlot) {
            menus().openEdit(player, npcId);
            return;
        }

        NpcAppearanceData appearance = data.appearance;
        boolean changed = false;

        if (slot == layout.toggleSlot) {
            appearance.glow = !appearance.glow;
            changed = true;
        } else if (slot == layout.disableSlot) {
            appearance.glow = false;
            changed = true;
        } else if (colorSlotIds.containsKey(slot)) {
            appearance.glowColor = colorSlotIds.get(slot);
            appearance.glow = true;
            changed = true;
        }

        if (changed) {
            appearance.normalizePresentation();
            repository().save(data);
            npcService().refreshGlow(npcId);
            render(player);
        }
    }

    @Override
    protected void render(Player player) {
        inventory.clear();
        colorSlotIds.clear();
        GuiNpcEditSettings.GlowLayout layout = pluginConfig().guiNpcEdit().glowMenu;
        NpcFileData data = findNpc();
        if (data == null) {
            return;
        }
        NpcAppearanceData appearance = data.appearance;
        appearance.normalizePresentation();

        fillPane(layout.size, GuiMenuItems.pane());

        inventory.setItem(layout.toggleSlot, toggleItem(player, appearance));
        inventory.setItem(layout.disableSlot, disableItem(player, appearance));
        inventory.setItem(layout.backSlot, GuiMenuItems.back(messageService(), player, layout.backMaterial));

        List<NpcGlowColors.Option> options = NpcGlowColors.options();
        int[] slots = layout.colorSlots();
        for (int index = 0; index < options.size() && index < slots.length; index++) {
            NpcGlowColors.Option option = options.get(index);
            int slot = slots[index];
            colorSlotIds.put(slot, option.id());
            inventory.setItem(slot, colorItem(player, option, appearance));
        }
    }

    private ItemStack toggleItem(Player player, NpcAppearanceData appearance) {
        GuiNpcEditSettings.GlowLayout layout = pluginConfig().guiNpcEdit().glowMenu;
        ItemStack item = new ItemStack(layout.toggleMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(
                player,
                "gui.glow.toggle-name",
                Placeholder.component(
                        "state",
                        messageService().message(player, appearance.glow ? "gui.state-on" : "gui.state-off")
                )
        ));
        List<Component> lore = new ArrayList<>();
        GuiMenuItems.appendToggleLore(messageService(), player, lore, "gui.glow.toggle-lore", appearance.glow);
        lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                messageService().plain(player, "gui.glow.current-color-line")
                        .replace("{color}", colorLabel(player, appearance.glowColor))
        )));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack disableItem(Player player, NpcAppearanceData appearance) {
        GuiNpcEditSettings.GlowLayout layout = pluginConfig().guiNpcEdit().glowMenu;
        boolean selected = !appearance.glow;
        ItemStack item = new ItemStack(layout.disableMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(player, "gui.glow.disable-name"));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(
                player,
                selected ? "gui.glow.disable-lore-selected" : "gui.glow.disable-lore"
        )) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(line)));
        }
        if (selected) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack colorItem(Player player, NpcGlowColors.Option option, NpcAppearanceData appearance) {
        ItemStack item = new ItemStack(option.icon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        boolean selected = option.id().equals(appearance.glowColor);
        meta.displayName(colorDisplayName(player, option));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, selected ? "gui.glow.color-lore-selected" : "gui.glow.color-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(line)));
        }
        if (selected) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Component colorDisplayName(Player player, NpcGlowColors.Option option) {
        String label = colorLabel(player, option.id());
        String tag = option.id();
        return messageService().raw("<" + tag + ">▸ " + label + "</" + tag + ">");
    }

    private String colorLabel(Player player, String colorId) {
        String key = "gui.glow.color-id." + NpcGlowColors.normalizeId(colorId);
        String label = messageService().plain(player, key);
        if (label.equals(key)) {
            return colorId;
        }
        return label;
    }
}
