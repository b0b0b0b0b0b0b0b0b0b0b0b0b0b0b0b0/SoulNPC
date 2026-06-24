package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.settings.GuiAdminSettings;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.util.NpcSkullProfileUtil;
import bm.b0b0b0.SoulNPC.util.SoulNpcPermissionChecks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AdminNpcMenu implements ClickableSoulNpcGui {

    private final NpcEditGuiDependencies deps;
    private final int page;
    private final Inventory inventory;
    private final List<String> npcIds = new ArrayList<>();
    private int totalPages = 1;
    private int currentPage;

    public AdminNpcMenu(NpcEditGuiDependencies deps) {
        this(deps, 0);
    }

    public AdminNpcMenu(NpcEditGuiDependencies deps, int page) {
        this.deps = deps;
        this.page = Math.max(0, page);
        GuiAdminSettings.Layout layout = deps.pluginConfig().guiAdmin().layout;
        this.inventory = Bukkit.createInventory(this, layout.size, deps.messageService().guiTitle("gui.admin-title"));
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

    @Override
    public void handleInventoryClick(Player player, GuiClickContext click) {
        GuiAdminSettings.Layout layout = deps.pluginConfig().guiAdmin().layout;
        int slot = click.slot();
        if (slot == layout.reloadSlot) {
            if (!SoulNpcPermissionChecks.requireAdmin(player, deps.pluginConfig(), deps.messageService())) {
                return;
            }
            player.sendMessage(deps.messageService().message(player, "gui.reload-started"));
            Bukkit.getScheduler().runTask(deps.plugin(), () -> {
                deps.npcService().reload();
                reopen(player, currentPage);
                player.sendMessage(deps.messageService().message(player, "gui.reload-done"));
            });
            return;
        }
        if (slot == layout.createSlot && currentPage > 0) {
            reopen(player, currentPage - 1);
            return;
        }
        if (slot == layout.closeSlot && currentPage < totalPages - 1) {
            reopen(player, currentPage + 1);
            return;
        }
        if (slot >= layout.npcListStart && slot <= layout.npcListEnd) {
            int index = slot - layout.npcListStart;
            if (index < 0 || index >= npcIds.size()) {
                return;
            }
            String npcId = npcIds.get(index);
            if (!click.rightClick()) {
                deps.menus().openEdit(player, npcId);
                return;
            }
            if (!SoulNpcPermissionChecks.requireAdmin(player, deps.pluginConfig(), deps.messageService())) {
                return;
            }
            deps.npcService().findRuntime(npcId).ifPresentOrElse(runtime -> {
                Location target = new Location(
                        Bukkit.getWorld(runtime.data().world),
                        runtime.data().x,
                        runtime.data().y,
                        runtime.data().z,
                        runtime.data().yaw,
                        runtime.data().pitch
                );
                player.closeInventory();
                player.teleportAsync(target).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(deps.messageService().message(player, "command.tp-success", Placeholder.parsed("npc", npcId)));
                    }
                });
            }, () -> player.sendMessage(deps.messageService().message(player, "command.delete-missing", Placeholder.parsed("npc", npcId))));
        }
    }

    private void reopen(Player player, int newPage) {
        new AdminNpcMenu(deps, newPage).open(player);
    }

    private void render(Player player) {
        inventory.clear();
        npcIds.clear();
        GuiAdminSettings.Layout layout = deps.pluginConfig().guiAdmin().layout;
        ItemStack filler = pane(layout.fillerMaterial);
        for (int slot = 0; slot < layout.size; slot++) {
            inventory.setItem(slot, filler);
        }

        List<NpcFileData> all = new ArrayList<>(deps.repository().findAll());
        all.sort(Comparator.comparing(data -> data.id.toLowerCase()));

        int pageSize = pageSize(layout);
        totalPages = Math.max(1, (all.size() + pageSize - 1) / pageSize);
        currentPage = Math.min(page, totalPages - 1);
        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, all.size());

        int slot = layout.npcListStart;
        for (int i = fromIndex; i < toIndex; i++) {
            NpcFileData data = all.get(i);
            npcIds.add(data.id);
            inventory.setItem(slot++, createNpcItem(player, data));
        }

        inventory.setItem(layout.reloadSlot, actionItem(player, layout.reloadMaterial, "gui.reload-name", "gui.reload-lore"));
        if (currentPage > 0) {
            inventory.setItem(
                    layout.createSlot,
                    pageItem(player, layout.createMaterial, "gui.page-prev-name", "gui.page-prev-lore", currentPage + 1, totalPages)
            );
        }
        if (currentPage < totalPages - 1) {
            inventory.setItem(
                    layout.closeSlot,
                    pageItem(player, layout.closeMaterial, "gui.page-next-name", "gui.page-next-lore", currentPage + 1, totalPages)
            );
        }
    }

    private static int pageSize(GuiAdminSettings.Layout layout) {
        return layout.npcListEnd - layout.npcListStart + 1;
    }

    private ItemStack createNpcItem(Player player, NpcFileData data) {
        GuiAdminSettings.Layout layout = deps.pluginConfig().guiAdmin().layout;
        Material material = NpcMenuIconUtil.materialFor(data, layout.npcMaterial);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(deps.messageService().message(player, "gui.npc-entry-name", Placeholder.parsed("npc", data.id)));
        meta.lore(deps.messageService().messageList(
                player,
                "gui.npc-entry-lore",
                Placeholder.parsed("npc", data.id),
                Placeholder.parsed("world", data.world),
                Placeholder.parsed("x", formatCoord(data.x)),
                Placeholder.parsed("y", formatCoord(data.y)),
                Placeholder.parsed("z", formatCoord(data.z)),
                Placeholder.parsed("type", formatDisplayType(data))
        ));
        if (NpcMenuIconUtil.usesPlayerHead(data) && meta instanceof SkullMeta skullMeta) {
            String profile = data.appearance.profile.isBlank() ? data.id : data.appearance.profile;
            NpcSkullProfileUtil.applySkullProfile(skullMeta, profile, data.id);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static String formatDisplayType(NpcFileData data) {
        if (!data.appearance.isPacketMob()) {
            return "player";
        }
        String entity = data.appearance.entityType;
        if (entity == null || entity.isBlank()) {
            entity = data.appearance.resolvedEntityType();
        }
        return entity == null || entity.isBlank() ? "mob" : entity;
    }

    private static String formatCoord(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05D) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private ItemStack pageItem(
            Player player,
            Material material,
            String namePath,
            String lorePath,
            int pageNumber,
            int pages
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(deps.messageService().message(
                player,
                namePath,
                Placeholder.parsed("page", String.valueOf(pageNumber)),
                Placeholder.parsed("pages", String.valueOf(pages))
        ));
        meta.lore(deps.messageService().messageList(
                player,
                lorePath,
                Placeholder.parsed("page", String.valueOf(pageNumber)),
                Placeholder.parsed("pages", String.valueOf(pages))
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack actionItem(Player player, Material material, String namePath, String lorePath) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(deps.messageService().message(player, namePath));
        if (lorePath != null) {
            List<Component> lore = new ArrayList<>();
            for (String line : deps.messageService().plainList(player, lorePath)) {
                lore.add(deps.messageService().raw(line));
            }
            meta.lore(lore);
        }
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
}
