package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.appearance.NpcEquipmentSlotCodec;
import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcEquipmentSlotData;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NpcDressMenu extends AbstractNpcEditMenu {

    private final Set<Integer> equipmentSlots = new HashSet<>();
    private boolean skipCloseSave;

    public NpcDressMenu(NpcEditGuiDependencies deps, String npcId) {
        super(
                deps,
                npcId,
                deps.pluginConfig().guiNpcEdit().dressMenu.size,
                deps.messageService().guiTitle("gui.dress-title", Placeholder.parsed("npc", npcId.toLowerCase(Locale.ROOT)))
        );
        GuiNpcEditSettings.DressLayout layout = pluginConfig().guiNpcEdit().dressMenu;
        for (int slot : layout.equipmentSlots()) {
            equipmentSlots.add(slot);
        }
        render(null);
    }

    public boolean isEquipmentSlot(int slot) {
        return equipmentSlots.contains(slot);
    }

    public boolean shouldSkipCloseSave() {
        return skipCloseSave;
    }

    public void open(Player player) {
        skipCloseSave = false;
        super.open(player);
    }

    @Override
    public void handleInventoryClick(Player player, GuiClickContext click) {
        handleBackClick(player, click.slot());
    }

    private void handleBackClick(Player player, int slot) {
        GuiNpcEditSettings.DressLayout layout = pluginConfig().guiNpcEdit().dressMenu;
        if (repository().findById(npcId).isEmpty()) {
            player.closeInventory();
            player.sendMessage(messageService().message(player, "command.delete-missing", Placeholder.parsed("npc", npcId)));
            return;
        }
        if (slot == layout.backSlot) {
            skipCloseSave = true;
            saveFromInventory();
            menus().openEdit(player, npcId);
        }
    }

    public void handleEquipmentClick(Player player, InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder(false) != this) {
            return;
        }
        int slot = event.getSlot();
        if (!isEquipmentSlot(slot)) {
            return;
        }
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY) {
            return;
        }
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack current = inventory.getItem(slot);
        boolean cursorEmpty = cursor == null || cursor.getType().isAir();
        boolean currentVirtual = isVirtualSlotItem(current);

        if (currentVirtual && cursorEmpty) {
            return;
        }
        if (currentVirtual) {
            inventory.setItem(slot, cursor.clone());
            event.getView().setCursor(null);
            return;
        }
        if (cursorEmpty) {
            event.getView().setCursor(current.clone());
            inventory.setItem(slot, hintForEquipmentSlot(player, slot));
            return;
        }
        event.getView().setCursor(current.clone());
        inventory.setItem(slot, cursor.clone());
    }

    private boolean isVirtualSlotItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return true;
        }
        return NpcEquipmentSlotCodec.isHint(stack, soulNpcKeys().dressHint);
    }

    private ItemStack hintForEquipmentSlot(Player player, int slot) {
        GuiNpcEditSettings.DressLayout layout = pluginConfig().guiNpcEdit().dressMenu;
        if (slot == layout.helmetSlot) {
            return hintItem(player, layout.helmetHintMaterial, "gui.dress.slot-helmet-name", "gui.dress.slot-helmet-lore");
        }
        if (slot == layout.chestSlot) {
            return hintItem(player, layout.chestHintMaterial, "gui.dress.slot-chest-name", "gui.dress.slot-chest-lore");
        }
        if (slot == layout.leggingsSlot) {
            return hintItem(player, layout.leggingsHintMaterial, "gui.dress.slot-leggings-name", "gui.dress.slot-leggings-lore");
        }
        if (slot == layout.bootsSlot) {
            return hintItem(player, layout.bootsHintMaterial, "gui.dress.slot-boots-name", "gui.dress.slot-boots-lore");
        }
        if (slot == layout.offHandSlot) {
            return hintItem(player, layout.offHandHintMaterial, "gui.dress.slot-offhand-name", "gui.dress.slot-offhand-lore");
        }
        if (slot == layout.mainHandSlot) {
            return hintItem(player, layout.mainHandHintMaterial, "gui.dress.slot-mainhand-name", "gui.dress.slot-mainhand-lore");
        }
        return new ItemStack(Material.AIR);
    }

    public void saveFromInventory() {
        NpcFileData data = findNpc();
        if (data == null || !data.appearance.type.isPlayerModel()) {
            return;
        }
        GuiNpcEditSettings.DressLayout layout = pluginConfig().guiNpcEdit().dressMenu;
        NpcAppearanceData appearance = data.appearance;
        appearance.ensureEquipmentSlots();

        appearance.helmet = readSlot(layout.helmetSlot);
        appearance.chestplate = readSlot(layout.chestSlot);
        appearance.leggings = readSlot(layout.leggingsSlot);
        appearance.boots = readSlot(layout.bootsSlot);
        appearance.offHand = readSlot(layout.offHandSlot);
        appearance.mainHand = readSlot(layout.mainHandSlot);

        repository().save(data);
        npcService().refreshEquipment(npcId);
    }

    private NpcEquipmentSlotData readSlot(int slot) {
        ItemStack stack = inventory.getItem(slot);
        return NpcEquipmentSlotCodec.fromItemStack(stack, soulNpcKeys().dressHint, pluginConfig());
    }

    @Override
    protected void render(Player player) {
        inventory.clear();
        GuiNpcEditSettings.DressLayout layout = pluginConfig().guiNpcEdit().dressMenu;
        NpcFileData data = findNpc();
        if (data == null) {
            return;
        }
        NpcAppearanceData appearance = data.appearance;
        appearance.ensureEquipmentSlots();

        ItemStack filler = fillerPane(layout.fillerMaterial);
        for (int slot = 0; slot < layout.size; slot++) {
            inventory.setItem(slot, filler);
        }

        putEquipmentSlot(player, layout.helmetSlot, appearance.helmet, layout.helmetHintMaterial,
                "gui.dress.slot-helmet-name", "gui.dress.slot-helmet-lore");
        putEquipmentSlot(player, layout.chestSlot, appearance.chestplate, layout.chestHintMaterial,
                "gui.dress.slot-chest-name", "gui.dress.slot-chest-lore");
        putEquipmentSlot(player, layout.leggingsSlot, appearance.leggings, layout.leggingsHintMaterial,
                "gui.dress.slot-leggings-name", "gui.dress.slot-leggings-lore");
        putEquipmentSlot(player, layout.bootsSlot, appearance.boots, layout.bootsHintMaterial,
                "gui.dress.slot-boots-name", "gui.dress.slot-boots-lore");
        putEquipmentSlot(player, layout.offHandSlot, appearance.offHand, layout.offHandHintMaterial,
                "gui.dress.slot-offhand-name", "gui.dress.slot-offhand-lore");
        putEquipmentSlot(player, layout.mainHandSlot, appearance.mainHand, layout.mainHandHintMaterial,
                "gui.dress.slot-mainhand-name", "gui.dress.slot-mainhand-lore");

        inventory.setItem(layout.backSlot, GuiMenuItems.back(messageService(), player, layout.backMaterial));
    }

    private void putEquipmentSlot(
            Player player,
            int slot,
            NpcEquipmentSlotData equipment,
            Material hintMaterial,
            String namePath,
            String lorePath
    ) {
        ItemStack item = itemStackFactory().create(equipment);
        if (item.isEmpty()) {
            item = hintItem(player, hintMaterial, namePath, lorePath);
        } else {
            item = item.clone();
        }
        inventory.setItem(slot, item);
    }

    private ItemStack hintItem(Player player, Material material, String namePath, String lorePath) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService().message(player, namePath));
            List<Component> lore = new ArrayList<>();
            for (String line : messageService().plainList(player, lorePath)) {
                lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(line)));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        NpcEquipmentSlotCodec.markHint(item, soulNpcKeys().dressHint);
        return item;
    }

    private static ItemStack fillerPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }
}
