package bm.b0b0b0.SoulNPC.hologram;

import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcHologramLineData;
import bm.b0b0b0.SoulNPC.model.NpcHologramLineType;
import bm.b0b0b0.SoulNPC.model.NpcEquipmentSlotData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import bm.b0b0b0.SoulNPC.util.NpcViewDistance;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class NpcTextLabels {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final float PLAYER_HEAD_TOP = 1.8F;
    private static final float HEAD_GAP = 0.12F;

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final ItemStackFactory itemStackFactory;
    private final SoulNpcKeys keys;
    private NpcSpawnService spawnService;

    public NpcTextLabels(JavaPlugin plugin, PluginConfig pluginConfig, ItemStackFactory itemStackFactory, SoulNpcKeys keys) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.itemStackFactory = itemStackFactory;
        this.keys = keys;
    }

    public void bind(NpcSpawnService spawnService) {
        this.spawnService = spawnService;
    }

    public void spawn(NpcRuntime runtime) {
        NpcFileData data = runtime.data();
        if (!data.appearance.useTextDisplay) {
            remove(runtime);
            return;
        }
        World world = Bukkit.getWorld(data.world);
        if (world == null) {
            return;
        }
        Location base = new Location(world, data.x, data.y, data.z);
        if (!world.isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
            return;
        }

        remove(runtime);
        removeTaggedInChunk(world, data.id, base);

        NpcAppearanceData appearance = data.appearance;
        appearance.migrateLegacyHologramOffsets();

        List<LayoutEntry> entries = collectLayoutEntries(appearance);
        if (entries.stream().noneMatch(LayoutEntry::visible)) {
            return;
        }

        float lineHeight = Math.max(0.12F, appearance.hologramLineHeight);
        float spacing = Math.max(0.05F, appearance.hologramLineSpacing);
        float headTop = (float) data.y + PLAYER_HEAD_TOP * appearance.resolvedScale();
        float bottomLineCenter = headTop + HEAD_GAP + lineHeight * entries.get(0).scale() * 0.5F;
        float baseOffset = appearance.hologramBaseOffset;
        if (baseOffset > 0.0F) {
            bottomLineCenter = (float) data.y + baseOffset;
        }

        float currentCenterY = bottomLineCenter;
        for (int index = 0; index < entries.size(); index++) {
            LayoutEntry entry = entries.get(index);
            if (index > 0) {
                LayoutEntry previous = entries.get(index - 1);
                currentCenterY += lineHeight * previous.scale() * 0.5F + spacing + lineHeight * entry.scale() * 0.5F;
            }
            if (!entry.visible()) {
                continue;
            }
            UUID displayId = entry.itemLine()
                    ? spawnItemLabel(data, base, entry.role(), entry.lineData(), currentCenterY, entry.scale(), appearance)
                    : spawnLabel(data, base, entry.role(), entry.text(), currentCenterY, entry.scale(), appearance);
            if (displayId != null) {
                runtime.addHologramDisplay(displayId);
            }
        }
        syncVisibilityForAllPlayers(runtime);
    }

    public void updateVisibilityForPlayer(Player player, NpcRuntime runtime, int viewDistanceBlocks) {
        NpcFileData data = runtime.data();
        if (!data.appearance.useTextDisplay) {
            applyHologramVisibility(player, runtime, false);
            return;
        }
        if (runtime.hologramDisplayIds().isEmpty()) {
            runtime.removeHologramViewer(player.getUniqueId());
            return;
        }
        boolean visible = data.enabled
                && NpcViewDistance.canSee(player, data)
                && NpcViewDistance.isWithin(player, data, viewDistanceBlocks);
        applyHologramVisibility(player, runtime, visible);
    }

    public void hideFromPlayer(Player player, NpcRuntime runtime) {
        applyHologramVisibility(player, runtime, false);
    }

    public void showToPlayer(Player player, NpcRuntime runtime) {
        NpcFileData data = runtime.data();
        if (!data.appearance.useTextDisplay || runtime.hologramDisplayIds().isEmpty()) {
            return;
        }
        int viewDistance = NpcViewDistance.hologramBlocks(pluginConfig.settings().performance, data);
        if (!data.enabled || !NpcViewDistance.canSee(player, data) || !NpcViewDistance.isWithin(player, data, viewDistance)) {
            applyHologramVisibility(player, runtime, false);
            return;
        }
        applyHologramVisibility(player, runtime, true);
    }

    private void applyHologramVisibility(Player player, NpcRuntime runtime, boolean visible) {
        if (!plugin.isEnabled()) {
            return;
        }
        for (UUID displayId : runtime.hologramDisplayIds()) {
            Entity entity = Bukkit.getEntity(displayId);
            if (entity == null) {
                continue;
            }
            if (visible) {
                if (!player.canSee(entity)) {
                    player.showEntity(plugin, entity);
                }
            } else if (player.canSee(entity)) {
                player.hideEntity(plugin, entity);
            }
        }
        if (visible) {
            runtime.addHologramViewer(player.getUniqueId());
        } else {
            runtime.removeHologramViewer(player.getUniqueId());
        }
    }

    private void hideFromAllPlayers(NpcRuntime runtime) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyHologramVisibility(player, runtime, false);
        }
    }

    private void syncVisibilityForAllPlayers(NpcRuntime runtime) {
        NpcFileData data = runtime.data();
        int viewDistance = NpcViewDistance.hologramBlocks(pluginConfig.settings().performance, data);
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateVisibilityForPlayer(player, runtime, viewDistance);
        }
    }

    public void remove(NpcRuntime runtime) {
        NpcFileData data = runtime.data();
        World world = Bukkit.getWorld(data.world);
        if (world != null) {
            hideFromAllPlayers(runtime);
            Location base = new Location(world, data.x, data.y, data.z);
            removeTaggedInChunk(world, data.id, base);
        }
        for (UUID displayId : new ArrayList<>(runtime.hologramDisplayIds())) {
            Entity entity = Bukkit.getEntity(displayId);
            if (entity != null && spawnService != null) {
                spawnService.unregisterHologramEntity(entity.getEntityId());
            }
            removeDisplay(displayId);
        }
        runtime.clearHologramDisplays();
    }

    public void removeAll(Collection<NpcRuntime> runtimes) {
        for (NpcRuntime runtime : runtimes) {
            remove(runtime);
        }
    }

    public int purgeAllPluginDisplays() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            removed += purgeWorldPluginDisplays(world);
        }
        return removed;
    }

    public int purgeChunk(World world, int chunkX, int chunkZ) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return 0;
        }
        int removed = 0;
        for (Entity entity : world.getChunkAt(chunkX, chunkZ).getEntities()) {
            if (removeIfPluginDisplay(entity)) {
                removed++;
            }
        }
        return removed;
    }

    private int purgeWorldPluginDisplays(World world) {
        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (removeIfPluginDisplay(entity)) {
                removed++;
            }
        }
        return removed;
    }

    private boolean removeIfPluginDisplay(Entity entity) {
        if (!(entity instanceof TextDisplay) && !(entity instanceof ItemDisplay)) {
            return false;
        }
        if (!isPluginDisplay(entity)) {
            return false;
        }
        if (spawnService != null) {
            spawnService.unregisterHologramEntity(entity.getEntityId());
        }
        entity.remove();
        return true;
    }

    public void refreshChunk(World world, int chunkX, int chunkZ, Collection<NpcRuntime> runtimes) {
        for (NpcRuntime runtime : runtimes) {
            NpcFileData data = runtime.data();
            if (!data.enabled || !data.appearance.useTextDisplay) {
                continue;
            }
            if (!world.getName().equals(data.world)) {
                continue;
            }
            int npcChunkX = (int) Math.floor(data.x) >> 4;
            int npcChunkZ = (int) Math.floor(data.z) >> 4;
            if (npcChunkX != chunkX || npcChunkZ != chunkZ) {
                continue;
            }
            spawn(runtime);
        }
    }

    public boolean isPluginDisplay(Entity entity) {
        return entity.getPersistentDataContainer().has(keys.npcId, PersistentDataType.STRING);
    }

    private static List<LayoutEntry> collectLayoutEntries(NpcAppearanceData appearance) {
        List<LayoutEntry> entries = new ArrayList<>();
        if (hasText(appearance.name)) {
            if (appearance.nameHidden) {
                entries.add(LayoutEntry.gap("name", appearance.nameDisplayScale));
            } else {
                entries.add(LayoutEntry.visible("name", appearance.name, appearance.nameDisplayScale));
            }
        }
        if (appearance.extraLines != null) {
            for (int extraIndex = 0; extraIndex < appearance.extraLines.size(); extraIndex++) {
                NpcHologramLineData extra = appearance.extraLines.get(extraIndex);
                if (extra == null || !extra.hasContent()) {
                    continue;
                }
                String role = "line-" + extraIndex;
                float lineScale = extra.lineType == NpcHologramLineType.ITEM
                        ? extra.resolvedScale()
                        : appearance.descriptionDisplayScale;
                if (extra.hidden) {
                    entries.add(LayoutEntry.gap(role, lineScale));
                } else if (extra.lineType == NpcHologramLineType.ITEM) {
                    entries.add(LayoutEntry.visibleItem(role, extra, lineScale));
                } else {
                    entries.add(LayoutEntry.visible(role, extra.text, lineScale));
                }
            }
        }
        return entries;
    }

    private UUID spawnLabel(
            NpcFileData data,
            Location base,
            String role,
            String rawText,
            float y,
            float scale,
            NpcAppearanceData appearance
    ) {
        Location location = new Location(base.getWorld(), base.getX(), y, base.getZ());
        Component text = MINI.deserialize(rawText);
        float resolvedScale = scale <= 0.0F ? 1.0F : scale;

        TextDisplay display = base.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(appearance.hologramSeeThrough);
            entity.setShadowed(appearance.hologramShadowed);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            if (appearance.hologramBackground) {
                entity.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
            } else {
                entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            }
            entity.setLineWidth(200);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F),
                    new Vector3f(resolvedScale, resolvedScale, resolvedScale),
                    new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)
            ));
            entity.getPersistentDataContainer().set(keys.npcId, PersistentDataType.STRING, data.id);
            entity.getPersistentDataContainer().set(keys.entityRole, PersistentDataType.STRING, role);
        });
        if (spawnService != null && !data.appearance.isPacketMob()) {
            spawnService.registerHologramEntity(display.getEntityId(), data.id);
        }
        return display.getUniqueId();
    }

    private UUID spawnItemLabel(
            NpcFileData data,
            Location base,
            String role,
            NpcHologramLineData line,
            float y,
            float scale,
            NpcAppearanceData appearance
    ) {
        ItemStack item = createHologramItem(line);
        if (item == null || item.isEmpty()) {
            return null;
        }
        Location location = new Location(base.getWorld(), base.getX(), y, base.getZ());
        float resolvedScale = scale <= 0.0F ? 1.0F : scale;
        ItemDisplay display = base.getWorld().spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F),
                    new Vector3f(resolvedScale, resolvedScale, resolvedScale),
                    new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)
            ));
            entity.getPersistentDataContainer().set(keys.npcId, PersistentDataType.STRING, data.id);
            entity.getPersistentDataContainer().set(keys.entityRole, PersistentDataType.STRING, role);
        });
        if (spawnService != null && !data.appearance.isPacketMob()) {
            spawnService.registerHologramEntity(display.getEntityId(), data.id);
        }
        return display.getUniqueId();
    }

    private ItemStack createHologramItem(NpcHologramLineData line) {
        if (line == null) {
            return ItemStack.empty();
        }
        NpcEquipmentSlotData slot = new NpcEquipmentSlotData();
        slot.material = line.material;
        slot.customModelData = line.customModelData;
        slot.itemsAdderId = line.itemsAdderId;
        slot.nexoId = line.nexoId;
        return itemStackFactory.create(slot);
    }

    private void removeTaggedInChunk(World world, String npcId, Location base) {
        int chunkX = base.getBlockX() >> 4;
        int chunkZ = base.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }
        for (Entity entity : world.getChunkAt(chunkX, chunkZ).getEntities()) {
            if (!(entity instanceof TextDisplay) && !(entity instanceof ItemDisplay)) {
                continue;
            }
            String taggedId = entity.getPersistentDataContainer().get(keys.npcId, PersistentDataType.STRING);
            if (npcId.equals(taggedId)) {
                entity.remove();
            }
        }
    }

    private void removeDisplay(UUID displayId) {
        if (displayId == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(displayId);
        if (entity != null) {
            entity.remove();
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record LayoutEntry(
            String role,
            String text,
            NpcHologramLineData lineData,
            float scale,
            boolean visible,
            boolean itemLine
    ) {
        private static LayoutEntry visible(String role, String text, float scale) {
            return new LayoutEntry(role, text, null, scale, true, false);
        }

        private static LayoutEntry visibleItem(String role, NpcHologramLineData line, float scale) {
            return new LayoutEntry(role, "", line, scale, true, true);
        }

        private static LayoutEntry gap(String role, float scale) {
            return new LayoutEntry(role, "", null, scale, false, false);
        }
    }
}
