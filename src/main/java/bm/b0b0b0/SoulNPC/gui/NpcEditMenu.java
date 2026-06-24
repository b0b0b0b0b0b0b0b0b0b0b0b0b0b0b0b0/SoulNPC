package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.appearance.NpcGlowColors;
import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcGroundItemEffectData;
import bm.b0b0b0.SoulNPC.util.SoulNpcPermissionChecks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NpcEditMenu extends AbstractNpcEditMenu {

    private final GuiChatInputService chatInputService;

    public NpcEditMenu(NpcEditGuiDependencies deps, GuiChatInputService chatInputService, String npcId) {
        super(
                deps,
                npcId,
                deps.pluginConfig().guiNpcEdit().layout.size,
                deps.messageService().guiTitle("gui.edit-title", Placeholder.parsed("npc", npcId.toLowerCase(Locale.ROOT)))
        );
        this.chatInputService = chatInputService;
        render(null);
    }

    @Override
    public void handleInventoryClick(Player player, GuiClickContext click) {
        GuiNpcEditSettings.Layout layout = pluginConfig().guiNpcEdit().layout;
        NpcFileData data = requireNpc(player);
        if (data == null) {
            return;
        }

        int slot = click.slot();
        if (slot == layout.backSlot) {
            menus().openAdmin(player);
            return;
        }
        if (slot == layout.deleteSlot) {
            if (!SoulNpcPermissionChecks.requireDelete(player, pluginConfig(), messageService())) {
                return;
            }
            menus().openDeleteConfirm(player, npcId);
            return;
        }
        if (slot == layout.respawnSlot) {
            if (!SoulNpcPermissionChecks.requireAdmin(player, pluginConfig(), messageService())) {
                return;
            }
            npcService().saveAndRefresh(npcId);
            if (!npcService().respawn(npcId)) {
                player.sendMessage(messageService().message(player, "command.delete-missing", Placeholder.parsed("npc", npcId)));
                return;
            }
            player.sendMessage(messageService().message(player, "command.respawn-success", Placeholder.parsed("npc", npcId)));
            render(player);
            return;
        }
        if (slot == layout.teleportToMeSlot) {
            if (npcService().teleportToPlayer(npcId, player)) {
                player.sendMessage(messageService().message(player, "gui.edit.teleport-to-me-done", Placeholder.parsed("npc", npcId)));
            }
            render(player);
            return;
        }
        if (slot == layout.lookAtPlayersSlot) {
            npcService().toggleLookAtPlayers(npcId);
            render(player);
            return;
        }
        if (slot == layout.entityPoseSlot && data.appearance.type.isPlayerModel()) {
            npcService().cyclePlayerEntityPose(npcId, click.shiftClick());
            render(player);
            return;
        }
        if (slot == layout.groundItemsOpenSlot) {
            menus().openGroundItems(player, npcId);
            return;
        }
        if (slot == layout.dressOpenSlot && data.appearance.type.isPlayerModel()) {
            menus().openDress(player, npcId);
            return;
        }
        if (slot == layout.glowOpenSlot) {
            menus().openGlow(player, npcId);
            return;
        }
        if (slot == layout.linesOpenSlot) {
            menus().openLines(player, npcId);
            return;
        }
        if (slot == layout.actionsOpenSlot) {
            menus().openActions(player, npcId);
            return;
        }
        if (slot == layout.visibilityPermissionSlot) {
            if (click.shiftClick()) {
                data.visibilityPermission = "";
                npcService().saveAndRefresh(npcId);
                render(player);
            } else if (click.leftClick()) {
                chatInputService.beginVisibilityPermission(player, npcId);
            }
            return;
        }
        if (slot == layout.packetViewDistanceSlot) {
            if (!click.leftClick() && !click.rightClick()) {
                return;
            }
            int step = click.shiftClick() ? 16 : 8;
            if (click.leftClick()) {
                data.packetViewDistance = data.packetViewDistance <= 0 ? step : data.packetViewDistance + step;
            } else {
                data.packetViewDistance = Math.max(0, data.packetViewDistance - step);
            }
            npcService().saveAndRefresh(npcId);
            render(player);
            return;
        }
        if (slot == layout.hologramViewDistanceSlot) {
            if (!click.leftClick() && !click.rightClick()) {
                return;
            }
            int step = click.shiftClick() ? 16 : 8;
            if (click.leftClick()) {
                data.hologramViewDistance = data.hologramViewDistance <= 0 ? step : data.hologramViewDistance + step;
            } else {
                data.hologramViewDistance = Math.max(0, data.hologramViewDistance - step);
            }
            npcService().saveAndRefresh(npcId);
            render(player);
            return;
        }

        if (handleAppearanceSlot(slot, layout, data.appearance, click)) {
            npcService().saveAndRefresh(npcId);
            render(player);
        }
    }

    private boolean handleAppearanceSlot(
            int slot,
            GuiNpcEditSettings.Layout layout,
            NpcAppearanceData appearance,
            GuiClickContext click
    ) {
        float step = GuiMenuItems.step(click.shiftClick());
        if (slot == layout.hologramOffsetSlot) {
            if (!click.leftClick() && !click.rightClick()) {
                return false;
            }
            if (click.leftClick()) {
                appearance.hologramBaseOffset = GuiMenuItems.round(appearance.hologramBaseOffset + step);
            } else {
                appearance.hologramBaseOffset = GuiMenuItems.round(Math.max(1.5F, appearance.hologramBaseOffset - step));
            }
            return true;
        }
        if (slot == layout.hologramSpacingSlot) {
            if (!click.leftClick() && !click.rightClick()) {
                return false;
            }
            float spacingStep = step * 0.5F;
            if (click.leftClick()) {
                appearance.hologramLineSpacing = GuiMenuItems.round(Math.max(0.05F, appearance.hologramLineSpacing + spacingStep));
            } else {
                appearance.hologramLineSpacing = GuiMenuItems.round(Math.max(0.05F, appearance.hologramLineSpacing - spacingStep));
            }
            return true;
        }
        if (slot == layout.hologramScaleSlot) {
            if (!click.leftClick() && !click.rightClick()) {
                return false;
            }
            float scaleStep = step * 0.5F;
            if (click.leftClick()) {
                appearance.nameDisplayScale = GuiMenuItems.round(Math.max(0.3F, appearance.nameDisplayScale + scaleStep));
                appearance.descriptionDisplayScale = GuiMenuItems.round(Math.max(0.3F, appearance.descriptionDisplayScale + scaleStep));
            } else {
                appearance.nameDisplayScale = GuiMenuItems.round(Math.max(0.3F, appearance.nameDisplayScale - scaleStep));
                appearance.descriptionDisplayScale = GuiMenuItems.round(Math.max(0.3F, appearance.descriptionDisplayScale - scaleStep));
            }
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
        if (slot == layout.collidableSlot) {
            appearance.collidable = !appearance.collidable;
            return true;
        }
        return false;
    }

    @Override
    protected void render(Player player) {
        inventory.clear();
        GuiNpcEditSettings.Layout layout = pluginConfig().guiNpcEdit().layout;
        NpcFileData data = findNpc();
        if (data == null) {
            return;
        }
        NpcAppearanceData appearance = data.appearance;
        NpcGroundItemEffectData groundItems = data.groundItems;
        if (groundItems == null) {
            groundItems = new NpcGroundItemEffectData();
            data.groundItems = groundItems;
        }

        ItemStack filler = GuiMenuItems.pane();
        fillPane(layout.size, filler);

        inventory.setItem(layout.hologramOffsetSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.hologramOffsetMaterial,
                "gui.edit.hologram-offset-name",
                "gui.edit.hologram-offset-lore",
                appearance.hologramBaseOffset
        ));
        inventory.setItem(layout.hologramSpacingSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.hologramSpacingMaterial,
                "gui.edit.hologram-spacing-name",
                "gui.edit.hologram-spacing-lore",
                appearance.hologramLineSpacing
        ));
        inventory.setItem(layout.hologramScaleSlot, GuiMenuItems.action(
                messageService(),
                player,
                layout.hologramScaleMaterial,
                "gui.edit.hologram-scale-name",
                "gui.edit.hologram-scale-lore",
                appearance.nameDisplayScale
        ));
        inventory.setItem(layout.packetViewDistanceSlot, viewDistanceItem(
                player,
                layout.packetViewDistanceMaterial,
                "gui.edit.packet-view-distance-name",
                "gui.edit.packet-view-distance-lore",
                data.packetViewDistance
        ));
        inventory.setItem(layout.hologramViewDistanceSlot, viewDistanceItem(
                player,
                layout.hologramViewDistanceMaterial,
                "gui.edit.hologram-view-distance-name",
                "gui.edit.hologram-view-distance-lore",
                data.hologramViewDistance
        ));
        inventory.setItem(layout.visibilityPermissionSlot, visibilityPermissionItem(player, layout, data));
        inventory.setItem(layout.groundItemsOpenSlot, groundItemsOpenButton(player, layout, groundItems));
        if (appearance.type.isPlayerModel()) {
            inventory.setItem(layout.dressOpenSlot, dressOpenButton(player, layout));
        }
        inventory.setItem(layout.glowOpenSlot, glowOpenButton(player, layout, appearance));
        inventory.setItem(layout.linesOpenSlot, linesOpenButton(player, layout, appearance));
        inventory.setItem(layout.actionsOpenSlot, actionsOpenButton(player, layout, data));
        inventory.setItem(layout.seeThroughSlot, GuiMenuItems.toggle(
                messageService(),
                player, layout.seeThroughMaterial, "gui.edit.see-through-name", "gui.edit.see-through-lore",
                appearance.hologramSeeThrough
        ));
        inventory.setItem(layout.shadowSlot, GuiMenuItems.toggle(
                messageService(),
                player, layout.shadowMaterial, "gui.edit.shadow-name", "gui.edit.shadow-lore",
                appearance.hologramShadowed
        ));
        inventory.setItem(layout.backgroundSlot, GuiMenuItems.toggle(
                messageService(),
                player, layout.backgroundMaterial, "gui.edit.background-name", "gui.edit.background-lore",
                appearance.hologramBackground
        ));
        inventory.setItem(layout.collidableSlot, GuiMenuItems.toggle(
                messageService(),
                player, layout.collidableMaterial, "gui.edit.collidable-name", "gui.edit.collidable-lore",
                appearance.collidable
        ));
        inventory.setItem(layout.teleportToMeSlot, GuiMenuItems.action(
                messageService(),
                player, layout.teleportToMeMaterial, "gui.edit.teleport-to-me-name", "gui.edit.teleport-to-me-lore", null
        ));
        NpcFileData fileData = data;
        inventory.setItem(layout.lookAtPlayersSlot, GuiMenuItems.toggle(
                messageService(),
                player, layout.lookAtPlayersMaterial, "gui.edit.look-at-name", "gui.edit.look-at-lore",
                fileData.lookAtPlayers
        ));
        if (appearance.type.isPlayerModel()) {
            inventory.setItem(layout.entityPoseSlot, entityPoseItem(player, layout, appearance.entityPose));
        }
        inventory.setItem(layout.respawnSlot, GuiMenuItems.action(
                messageService(),
                player, layout.respawnMaterial, "gui.edit.respawn-name", "gui.edit.respawn-lore", null
        ));
        inventory.setItem(layout.deleteSlot, GuiMenuItems.action(
                messageService(),
                player, layout.deleteMaterial, "gui.edit.delete-name", "gui.edit.delete-lore", null
        ));
        inventory.setItem(layout.backSlot, GuiMenuItems.back(messageService(), player, layout.backMaterial));
    }

    private ItemStack viewDistanceItem(
            Player player,
            org.bukkit.Material material,
            String nameKey,
            String loreKey,
            int value
    ) {
        String displayValue = value <= 0
                ? messageService().plain(player, "gui.edit.view-distance-global")
                : String.valueOf(value);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(player, nameKey));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, loreKey)) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                    line.replace("{value}", displayValue)
            )));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack visibilityPermissionItem(Player player, GuiNpcEditSettings.Layout layout, NpcFileData data) {
        String permission = data.visibilityPermission == null || data.visibilityPermission.isBlank()
                ? messageService().plain(player, "gui.edit.visibility-permission-none")
                : data.visibilityPermission;
        ItemStack item = new ItemStack(layout.visibilityPermissionMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(player, "gui.edit.visibility-permission-name"));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, "gui.edit.visibility-permission-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                    line.replace("{permission}", permission)
            )));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack groundItemsOpenButton(
            Player player,
            GuiNpcEditSettings.Layout layout,
            NpcGroundItemEffectData effect
    ) {
        String summaryKey = effect.enabled ? "gui.edit.ground-items-open-summary-on" : "gui.edit.ground-items-open-summary-off";
        String summary = messageService().plain(player, summaryKey);
        ItemStack item = new ItemStack(layout.groundItemsOpenMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(player, "gui.edit.ground-items-open-name"));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, "gui.edit.ground-items-open-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                    line.replace("{state_summary}", summary)
            )));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack dressOpenButton(Player player, GuiNpcEditSettings.Layout layout) {
        return GuiMenuItems.action(
                messageService(),
                player,
                layout.dressOpenMaterial,
                "gui.edit.dress-open-name",
                "gui.edit.dress-open-lore",
                null
        );
    }

    private ItemStack glowOpenButton(Player player, GuiNpcEditSettings.Layout layout, NpcAppearanceData appearance) {
        String colorKey = "gui.glow.color-id." + NpcGlowColors.normalizeId(appearance.glowColor);
        String colorLabel = messageService().plain(player, colorKey);
        if (colorLabel.equals(colorKey)) {
            colorLabel = appearance.glowColor;
        }
        String stateSummary = appearance.glow
                ? messageService().plain(player, "gui.edit.glow-open-summary-on")
                        .replace("{color}", colorLabel)
                : messageService().plain(player, "gui.edit.glow-open-summary-off");
        ItemStack item = new ItemStack(layout.glowOpenMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(player, "gui.edit.glow-open-name"));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, "gui.edit.glow-open-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                    line.replace("{state_summary}", stateSummary)
            )));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack linesOpenButton(Player player, GuiNpcEditSettings.Layout layout, NpcAppearanceData appearance) {
        int active = NpcHologramLines.countActiveLines(appearance);
        ItemStack item = new ItemStack(layout.linesOpenMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(player, "gui.edit.lines-open-name"));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, "gui.edit.lines-open-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                    line.replace("{active}", String.valueOf(active))
            )));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack actionsOpenButton(Player player, GuiNpcEditSettings.Layout layout, NpcFileData data) {
        data.interaction.ensureActionsMigrated();
        int count = data.interaction.actionableActionCount();
        ItemStack item = new ItemStack(layout.actionsOpenMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messageService().message(player, "gui.edit.actions-open-name"));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, "gui.edit.actions-open-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                    line.replace("{count}", String.valueOf(count))
            )));
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
        meta.displayName(messageService().message(
                player,
                "gui.edit.entity-pose-name",
                Placeholder.parsed("pose", poseLabel(player, current))
        ));
        List<Component> lore = new ArrayList<>();
        for (String line : messageService().plainList(player, "gui.edit.entity-pose-lore")) {
            lore.add(messageService().raw(GuiMenuItems.bracesToMiniMessage(
                    line.replace("{pose}", poseLabel(player, current))
            )));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String poseLabel(Player player, NpcEntityPose pose) {
        String key = "gui.edit.pose." + pose.name().toLowerCase(Locale.ROOT);
        String label = messageService().plain(player, key);
        if (label.equals(key)) {
            return pose.name();
        }
        return label;
    }
}
