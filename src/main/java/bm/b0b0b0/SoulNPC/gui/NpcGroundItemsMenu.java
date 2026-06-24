package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcGroundItemEffectData;
import bm.b0b0b0.SoulNPC.model.NpcGroundItemEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NpcGroundItemsMenu extends AbstractNpcEditMenu {

    public NpcGroundItemsMenu(NpcEditGuiDependencies deps, String npcId) {
        super(
                deps,
                npcId,
                deps.pluginConfig().guiNpcEdit().groundItemsMenu.size,
                deps.messageService().guiTitle("gui.ground-items-title", Placeholder.parsed("npc", npcId.toLowerCase(Locale.ROOT)))
        );
        render(null);
    }

    @Override
    public void handleInventoryClick(Player player, GuiClickContext click) {
        GuiNpcEditSettings.GroundItemsLayout layout = pluginConfig().guiNpcEdit().groundItemsMenu;
        NpcFileData data = requireNpc(player);
        if (data == null) {
            return;
        }

        int slot = click.slot();
        if (slot == layout.backSlot) {
            menus().openEdit(player, npcId);
            return;
        }

        NpcGroundItemEffectData effect = data.groundItems;
        if (effect == null) {
            effect = new NpcGroundItemEffectData();
            data.groundItems = effect;
        }
        effect.ensureItems();
        float step = GuiMenuItems.step(click.shiftClick());
        boolean changed = false;

        if (slot == layout.toggleSlot) {
            effect.enabled = !effect.enabled;
            changed = true;
        } else if (slot == layout.offsetSlot) {
            if (!click.leftClick() && !click.rightClick()) {
                return;
            }
            if (click.leftClick()) {
                effect.spawnYOffset = GuiMenuItems.round(effect.spawnYOffset + step);
            } else {
                effect.spawnYOffset = GuiMenuItems.round(Math.max(-0.5F, effect.spawnYOffset - step));
            }
            changed = true;
        } else if (slot == layout.intervalSlot) {
            if (!click.leftClick() && !click.rightClick()) {
                return;
            }
            int intervalStep = click.shiftClick() ? 8 : 4;
            if (click.leftClick()) {
                effect.intervalTicks = Math.max(4, effect.intervalTicks - intervalStep);
            } else {
                effect.intervalTicks = Math.min(200, effect.intervalTicks + intervalStep);
            }
            changed = true;
        } else if (slot == layout.burstSlot) {
            npcService().burstGroundItems(npcId);
            return;
        }

        if (changed) {
            npcService().saveAndRefresh(npcId);
            render(player);
        }
    }

    @Override
    protected void render(Player player) {
        inventory.clear();
        GuiNpcEditSettings.GroundItemsLayout layout = pluginConfig().guiNpcEdit().groundItemsMenu;
        NpcFileData data = findNpc();
        if (data == null) {
            return;
        }
        NpcGroundItemEffectData effect = data.groundItems;
        if (effect == null) {
            effect = new NpcGroundItemEffectData();
            data.groundItems = effect;
        }
        effect.ensureItems();

        fillPane(layout.size, GuiMenuItems.pane());

        inventory.setItem(layout.toggleSlot, groundItemsToggle(player, layout, effect));
        inventory.setItem(layout.offsetSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.offsetMaterial,
                "gui.ground-items.offset-name",
                "gui.ground-items.offset-lore",
                effect.spawnYOffset
        ));
        inventory.setItem(layout.burstSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.burstMaterial,
                "gui.ground-items.burst-name",
                "gui.ground-items.burst-lore",
                null
        ));
        inventory.setItem(layout.intervalSlot, groundItemInterval(
                player,
                layout.intervalMaterial,
                "gui.ground-items.interval-name",
                "gui.ground-items.interval-lore",
                effect.intervalTicks
        ));
        inventory.setItem(layout.backSlot, GuiMenuItems.back(messageService(), player, layout.backMaterial));
    }

    private ItemStack groundItemsToggle(
            Player player,
            GuiNpcEditSettings.GroundItemsLayout layout,
            NpcGroundItemEffectData effect
    ) {
        ItemStack item = new ItemStack(layout.toggleMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(
                player,
                "gui.ground-items.toggle-name",
                Placeholder.component(
                        "state",
                        messageService().message(player, effect.enabled ? "gui.state-on" : "gui.state-off")
                )
        ));
        List<Component> lore = new ArrayList<>();
        GuiMenuItems.appendToggleLore(messageService(), player, lore, "gui.ground-items.toggle-lore", effect.enabled);
        lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                "<gray>▸ Предметы: <white>" + formatGroundItemList(effect) + "</white></gray>"
        )));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack groundItemInterval(
            Player player,
            Material material,
            String namePath,
            String lorePath,
            int intervalTicks
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(player, namePath));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, lorePath)) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                    line.replace("{value}", String.valueOf(intervalTicks))
            )));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String formatGroundItemList(NpcGroundItemEffectData effect) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < effect.items.size(); index++) {
            NpcGroundItemEntry entry = effect.items.get(index);
            entry.ensureMaterials();
            if (index > 0) {
                builder.append(" | ");
            }
            builder.append('[');
            for (int materialIndex = 0; materialIndex < entry.materials.size(); materialIndex++) {
                if (materialIndex > 0) {
                    builder.append(", ");
                }
                builder.append(entry.materials.get(materialIndex).name());
            }
            builder.append("] x").append(Math.max(1, entry.amount));
        }
        return builder.toString();
    }
}
