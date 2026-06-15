package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcGroundItemEffectData;
import bm.b0b0b0.SoulNPC.model.NpcGroundItemEntry;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NpcEditMenu implements InventoryHolder {

    private final PluginConfig pluginConfig;
    private final MessageService messageService;
    private final NpcRepository repository;
    private final NpcService npcService;
    private final AdminNpcMenuListener adminMenuListener;
    private final GuiChatInputService chatInputService;
    private final String npcId;
    private final Inventory inventory;
    private final Map<Integer, HologramLineTarget> lineSlotTargets = new HashMap<>();

    public NpcEditMenu(
            PluginConfig pluginConfig,
            MessageService messageService,
            NpcRepository repository,
            NpcService npcService,
            AdminNpcMenuListener adminMenuListener,
            GuiChatInputService chatInputService,
            String npcId
    ) {
        this.pluginConfig = pluginConfig;
        this.messageService = messageService;
        this.repository = repository;
        this.npcService = npcService;
        this.adminMenuListener = adminMenuListener;
        this.chatInputService = chatInputService;
        this.npcId = npcId.toLowerCase(Locale.ROOT);
        GuiNpcEditSettings.Layout layout = pluginConfig.guiNpcEdit().layout;
        this.inventory = Bukkit.createInventory(
                this,
                layout.size,
                messageService.guiTitle("gui.edit-title", Placeholder.parsed("npc", this.npcId))
        );
        render(null);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        render(player);
        player.openInventory(inventory);
    }

    public void handleClick(Player player, int slot, boolean leftClick, boolean shiftClick) {
        GuiNpcEditSettings.Layout layout = pluginConfig.guiNpcEdit().layout;
        NpcFileData data = repository.findById(npcId).orElse(null);
        if (data == null) {
            player.closeInventory();
            player.sendMessage(messageService.message(player, "command.delete-missing", Placeholder.parsed("npc", npcId)));
            return;
        }

        HologramLineTarget lineTarget = lineSlotTargets.get(slot);
        if (lineTarget != null && leftClick) {
            chatInputService.begin(player, npcId, lineTarget);
            return;
        }

        if (slot == layout.backSlot) {
            adminMenuListener.openAdmin(player);
            return;
        }
        if (slot == layout.deleteSlot) {
            if (npcService.delete(npcId)) {
                player.sendMessage(messageService.message(player, "command.delete-success", Placeholder.parsed("npc", npcId)));
            }
            adminMenuListener.openAdmin(player);
            return;
        }
        if (slot == layout.respawnSlot) {
            npcService.saveAndRefresh(npcId);
            npcService.respawn(npcId);
            player.sendMessage(messageService.message(player, "command.respawn-success", Placeholder.parsed("npc", npcId)));
            render(player);
            return;
        }
        if (slot == layout.teleportToMeSlot) {
            if (npcService.teleportToPlayer(npcId, player)) {
                player.sendMessage(messageService.message(player, "gui.edit.teleport-to-me-done", Placeholder.parsed("npc", npcId)));
            }
            render(player);
            return;
        }
        if (slot == layout.lookAtPlayersSlot) {
            if (npcService.toggleLookAtPlayers(npcId)) {
                render(player);
            }
            return;
        }
        if (slot == layout.entityPoseSlot && data.appearance.type.isPlayerModel()) {
            if (npcService.cyclePlayerEntityPose(npcId, shiftClick)) {
                render(player);
            }
            return;
        }

        if (handleGroundItemSlot(slot, layout, data, shiftClick)) {
            if (slot != layout.groundItemBurstSlot) {
                npcService.saveAndRefresh(npcId);
            }
            render(player);
            return;
        }

        if (handleAppearanceSlot(slot, layout, data.appearance, shiftClick)) {
            npcService.saveAndRefresh(npcId);
            render(player);
        }
    }

    private boolean handleGroundItemSlot(
            int slot,
            GuiNpcEditSettings.Layout layout,
            NpcFileData data,
            boolean shiftClick
    ) {
        NpcGroundItemEffectData effect = data.groundItems;
        if (effect == null) {
            effect = new NpcGroundItemEffectData();
            data.groundItems = effect;
        }
        effect.ensureItems();
        float step = step(shiftClick);

        if (slot == layout.groundItemsSlot) {
            effect.enabled = !effect.enabled;
            return true;
        }
        if (slot == layout.groundItemOffsetUpSlot) {
            effect.spawnYOffset = round(effect.spawnYOffset + step);
            return true;
        }
        if (slot == layout.groundItemOffsetDownSlot) {
            effect.spawnYOffset = round(Math.max(-0.5F, effect.spawnYOffset - step));
            return true;
        }
        if (slot == layout.groundItemIntervalFasterSlot) {
            effect.intervalTicks = Math.max(4, effect.intervalTicks - (shiftClick ? 8 : 4));
            return true;
        }
        if (slot == layout.groundItemIntervalSlowerSlot) {
            effect.intervalTicks = Math.min(200, effect.intervalTicks + (shiftClick ? 8 : 4));
            return true;
        }
        if (slot == layout.groundItemBurstSlot) {
            npcService.burstGroundItems(npcId);
            return true;
        }
        return false;
    }

    private boolean handleAppearanceSlot(
            int slot,
            GuiNpcEditSettings.Layout layout,
            NpcAppearanceData appearance,
            boolean shiftClick
    ) {
        float step = step(shiftClick);
        if (slot == layout.offsetUpSlot) {
            appearance.hologramBaseOffset = round(appearance.hologramBaseOffset + step);
            return true;
        }
        if (slot == layout.offsetDownSlot) {
            appearance.hologramBaseOffset = round(Math.max(1.5F, appearance.hologramBaseOffset - step));
            return true;
        }
        if (slot == layout.spacingUpSlot) {
            appearance.hologramLineSpacing = round(Math.max(0.05F, appearance.hologramLineSpacing + step * 0.5F));
            return true;
        }
        if (slot == layout.spacingDownSlot) {
            appearance.hologramLineSpacing = round(Math.max(0.05F, appearance.hologramLineSpacing - step * 0.5F));
            return true;
        }
        if (slot == layout.scaleUpSlot) {
            appearance.nameDisplayScale = round(Math.max(0.3F, appearance.nameDisplayScale + step * 0.5F));
            appearance.descriptionDisplayScale = round(Math.max(0.3F, appearance.descriptionDisplayScale + step * 0.5F));
            return true;
        }
        if (slot == layout.scaleDownSlot) {
            appearance.nameDisplayScale = round(Math.max(0.3F, appearance.nameDisplayScale - step * 0.5F));
            appearance.descriptionDisplayScale = round(Math.max(0.3F, appearance.descriptionDisplayScale - step * 0.5F));
            return true;
        }
        if (slot == layout.seeThroughSlot) {
            appearance.hologramSeeThrough = !appearance.hologramSeeThrough;
            return true;
        }
        if (slot == layout.shadowSlot) {
            appearance.hologramShadowed = !appearance.hologramShadowed;
            return true;
        }
        if (slot == layout.backgroundSlot) {
            appearance.hologramBackground = !appearance.hologramBackground;
            return true;
        }
        if (slot == layout.textDisplaySlot) {
            appearance.useTextDisplay = !appearance.useTextDisplay;
            npcService.respawn(npcId);
            return true;
        }
        return false;
    }

    private void render(Player player) {
        inventory.clear();
        lineSlotTargets.clear();
        GuiNpcEditSettings.Layout layout = pluginConfig.guiNpcEdit().layout;
        NpcFileData data = repository.findById(npcId).orElse(null);
        if (data == null) {
            return;
        }
        NpcAppearanceData appearance = data.appearance;
        NpcGroundItemEffectData groundItems = data.groundItems;
        if (groundItems == null) {
            groundItems = new NpcGroundItemEffectData();
            data.groundItems = groundItems;
        }
        groundItems.ensureItems();

        ItemStack filler = pane(layout.fillerMaterial);
        for (int slot = 0; slot < layout.size; slot++) {
            inventory.setItem(slot, filler);
        }

        renderLineColumn(player, layout, appearance);

        inventory.setItem(layout.offsetUpSlot, action(
                player, layout.offsetUpMaterial, "gui.edit.offset-up-name", "gui.edit.offset-up-lore",
                appearance.hologramBaseOffset
        ));
        inventory.setItem(layout.offsetDownSlot, action(
                player, layout.offsetDownMaterial, "gui.edit.offset-down-name", "gui.edit.offset-down-lore",
                appearance.hologramBaseOffset
        ));
        inventory.setItem(layout.spacingUpSlot, action(
                player, layout.spacingUpMaterial, "gui.edit.spacing-up-name", "gui.edit.spacing-up-lore",
                appearance.hologramLineSpacing
        ));
        inventory.setItem(layout.spacingDownSlot, action(
                player, layout.spacingDownMaterial, "gui.edit.spacing-down-name", "gui.edit.spacing-down-lore",
                appearance.hologramLineSpacing
        ));
        inventory.setItem(layout.scaleUpSlot, action(
                player, layout.scaleUpMaterial, "gui.edit.scale-up-name", "gui.edit.scale-up-lore",
                appearance.nameDisplayScale
        ));
        inventory.setItem(layout.scaleDownSlot, action(
                player, layout.scaleDownMaterial, "gui.edit.scale-down-name", "gui.edit.scale-down-lore",
                appearance.nameDisplayScale
        ));
        inventory.setItem(layout.groundItemsSlot, groundItemsToggle(player, layout, groundItems));
        inventory.setItem(layout.groundItemOffsetUpSlot, action(
                player,
                layout.groundItemOffsetUpMaterial,
                "gui.edit.ground-items-offset-up-name",
                "gui.edit.ground-items-offset-up-lore",
                groundItems.spawnYOffset
        ));
        inventory.setItem(layout.groundItemOffsetDownSlot, action(
                player,
                layout.groundItemOffsetDownMaterial,
                "gui.edit.ground-items-offset-down-name",
                "gui.edit.ground-items-offset-down-lore",
                groundItems.spawnYOffset
        ));
        inventory.setItem(layout.groundItemBurstSlot, action(
                player,
                layout.groundItemBurstMaterial,
                "gui.edit.ground-items-burst-name",
                "gui.edit.ground-items-burst-lore",
                null
        ));
        inventory.setItem(layout.groundItemIntervalFasterSlot, groundItemInterval(
                player,
                layout.groundItemIntervalFasterMaterial,
                "gui.edit.ground-items-interval-faster-name",
                "gui.edit.ground-items-interval-faster-lore",
                groundItems.intervalTicks
        ));
        inventory.setItem(layout.groundItemIntervalSlowerSlot, groundItemInterval(
                player,
                layout.groundItemIntervalSlowerMaterial,
                "gui.edit.ground-items-interval-slower-name",
                "gui.edit.ground-items-interval-slower-lore",
                groundItems.intervalTicks
        ));
        inventory.setItem(layout.seeThroughSlot, toggle(
                player, layout.seeThroughMaterial, "gui.edit.see-through-name", "gui.edit.see-through-lore",
                appearance.hologramSeeThrough
        ));
        inventory.setItem(layout.shadowSlot, toggle(
                player, layout.shadowMaterial, "gui.edit.shadow-name", "gui.edit.shadow-lore",
                appearance.hologramShadowed
        ));
        inventory.setItem(layout.backgroundSlot, toggle(
                player, layout.backgroundMaterial, "gui.edit.background-name", "gui.edit.background-lore",
                appearance.hologramBackground
        ));
        inventory.setItem(layout.textDisplaySlot, toggle(
                player, layout.textDisplayMaterial, "gui.edit.text-display-name", "gui.edit.text-display-lore",
                appearance.useTextDisplay
        ));
        inventory.setItem(layout.teleportToMeSlot, action(
                player, layout.teleportToMeMaterial, "gui.edit.teleport-to-me-name", "gui.edit.teleport-to-me-lore", null
        ));
        NpcFileData fileData = data;
        inventory.setItem(layout.lookAtPlayersSlot, toggle(
                player, layout.lookAtPlayersMaterial, "gui.edit.look-at-name", "gui.edit.look-at-lore",
                fileData.lookAtPlayers
        ));
        if (appearance.type.isPlayerModel()) {
            inventory.setItem(layout.entityPoseSlot, entityPoseItem(player, layout, appearance.entityPose));
        }
        inventory.setItem(layout.respawnSlot, action(
                player, layout.respawnMaterial, "gui.edit.respawn-name", "gui.edit.respawn-lore", null
        ));
        inventory.setItem(layout.deleteSlot, action(
                player, layout.deleteMaterial, "gui.edit.delete-name", "gui.edit.delete-lore", null
        ));
        inventory.setItem(layout.backSlot, action(
                player, layout.backMaterial, "gui.edit.back-name", "gui.edit.back-lore", null
        ));
    }

    private void renderLineColumn(Player player, GuiNpcEditSettings.Layout layout, NpcAppearanceData appearance) {
        putLineSlot(player, layout.lineNameSlot, layout.lineNameMaterial, "gui.edit.line-slot-name",
                appearance.name, new HologramLineTarget.Name(), 1);
        putLineSlot(player, layout.lineDescriptionSlot, layout.lineDescriptionMaterial, "gui.edit.line-slot-description",
                appearance.description, new HologramLineTarget.Description(), 2);

        int[] extraSlots = layout.lineExtraSlots();
        for (int index = 0; index < extraSlots.length; index++) {
            String text = index < appearance.extraLines.size() ? appearance.extraLines.get(index) : "";
            HologramLineTarget target = new HologramLineTarget.Extra(index);
            putLineSlot(player, extraSlots[index], layout.lineExtraMaterial, "gui.edit.line-slot-extra",
                    text, target, index + 3);
        }

        lineSlotTargets.put(layout.lineAddSlot, new HologramLineTarget.AddExtra());
        inventory.setItem(layout.lineAddSlot, lineItem(
                player,
                layout.lineAddMaterial,
                "gui.edit.line-slot-add-name",
                "gui.edit.line-slot-add-lore",
                null,
                0
        ));
    }

    private void putLineSlot(
            Player player,
            int slot,
            Material material,
            String titlePath,
            String text,
            HologramLineTarget target,
            int lineNumber
    ) {
        lineSlotTargets.put(slot, target);
        inventory.setItem(slot, lineItem(player, material, titlePath, "gui.edit.line-slot-lore", text, lineNumber));
    }

    private ItemStack lineItem(
            Player player,
            Material material,
            String titlePath,
            String lorePath,
            String text,
            int lineNumber
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService.message(
                player,
                titlePath,
                Placeholder.parsed("line", String.valueOf(lineNumber))
        ));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService.plainList(player, lorePath)) {
            String resolved = line.replace("{text}", preview(text));
            lore.add(messageService.raw(bracesToMiniMessage(resolved)));
        }
        if (text != null && !text.isBlank()) {
            lore.add(messageService.raw(bracesToMiniMessage("<gray>" + preview(text) + "</gray>")));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack entityPoseItem(Player player, GuiNpcEditSettings.Layout layout, NpcEntityPose pose) {
        NpcEntityPose current = pose == null ? NpcEntityPose.STANDING : pose;
        ItemStack item = new ItemStack(layout.entityPoseMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService.message(
                player,
                "gui.edit.entity-pose-name",
                Placeholder.parsed("pose", poseLabel(player, current))
        ));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService.plainList(player, "gui.edit.entity-pose-lore")) {
            lore.add(messageService.raw(bracesToMiniMessage(
                    line.replace("{pose}", poseLabel(player, current))
            )));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String poseLabel(Player player, NpcEntityPose pose) {
        String key = "gui.edit.pose." + pose.name().toLowerCase(Locale.ROOT);
        String label = messageService.plain(player, key);
        if (label.equals(key)) {
            return pose.name();
        }
        return label;
    }

    private ItemStack groundItemsToggle(Player player, GuiNpcEditSettings.Layout layout, NpcGroundItemEffectData effect) {
        ItemStack item = new ItemStack(layout.groundItemsMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService.message(
                player,
                "gui.edit.ground-items-name",
                Placeholder.component(
                        "state",
                        messageService.message(player, effect.enabled ? "gui.state-on" : "gui.state-off")
                )
        ));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService.plainList(player, "gui.edit.ground-items-lore")) {
            lore.add(messageService.raw(bracesToMiniMessage(line)));
        }
        lore.add(messageService.raw(bracesToMiniMessage("<gray>" + formatGroundItemList(effect) + "</gray>")));
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
        meta.displayName(messageService.message(player, namePath));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService.plainList(player, lorePath)) {
            lore.add(messageService.raw(bracesToMiniMessage(
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

    private ItemStack action(Player player, Material material, String namePath, String lorePath, Float value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService.message(player, namePath));
        if (lorePath != null) {
            List<Component> lore = new ArrayList<>();
            for (String line : messageService.plainList(player, lorePath)) {
                String resolved = line;
                if (value != null) {
                    resolved = resolved.replace("{value}", String.format(Locale.ROOT, "%.2f", value));
                }
                lore.add(messageService.raw(bracesToMiniMessage(resolved)));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack toggle(Player player, Material material, String namePath, String lorePath, boolean enabled) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService.message(
                player,
                namePath,
                Placeholder.component("state", messageService.message(player, enabled ? "gui.state-on" : "gui.state-off"))
        ));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService.plainList(player, lorePath)) {
            lore.add(messageService.raw(bracesToMiniMessage(line)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String preview(String text) {
        if (text == null || text.isBlank()) {
            return "—";
        }
        String stripped = text.replaceAll("<[^>]+>", "");
        if (stripped.length() > 48) {
            return stripped.substring(0, 45) + "...";
        }
        return stripped;
    }

    private static float step(boolean shiftClick) {
        return shiftClick ? 0.25F : 0.1F;
    }

    private static float round(float value) {
        return Math.round(value * 100.0F) / 100.0F;
    }

    private static String bracesToMiniMessage(String raw) {
        return raw.replaceAll("\\{([a-zA-Z0-9_-]+)\\}", "<$1>");
    }
}
