package bm.b0b0b0.SoulNPC.hologram;

import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
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

    private final SoulNpcKeys keys;
    private NpcSpawnService spawnService;

    public NpcTextLabels(SoulNpcKeys keys) {
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

        List<HologramLine> lines = collectLines(appearance);
        if (lines.isEmpty()) {
            return;
        }

        float lineHeight = Math.max(0.12F, appearance.hologramLineHeight);
        float spacing = Math.max(0.05F, appearance.hologramLineSpacing);
        float headTop = (float) data.y + PLAYER_HEAD_TOP * appearance.resolvedScale();
        float bottomLineCenter = headTop + HEAD_GAP + lineHeight * lines.get(0).scale() * 0.5F;
        float baseOffset = appearance.hologramBaseOffset;
        if (baseOffset > 0.0F) {
            bottomLineCenter = (float) data.y + baseOffset;
        }

        float currentCenterY = bottomLineCenter;
        for (int index = 0; index < lines.size(); index++) {
            HologramLine line = lines.get(index);
            if (index > 0) {
                HologramLine previous = lines.get(index - 1);
                currentCenterY += lineHeight * previous.scale() * 0.5F + spacing + lineHeight * line.scale() * 0.5F;
            }
            UUID displayId = spawnLabel(
                    data,
                    base,
                    line.role(),
                    line.text(),
                    currentCenterY,
                    line.scale(),
                    appearance
            );
            runtime.addHologramDisplay(displayId);
        }
    }

    public void remove(NpcRuntime runtime) {
        for (UUID displayId : runtime.hologramDisplayIds()) {
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

    /** Удаляет все TextDisplay с PDC плагина в загруженных чанках (сироты после краша / reload). */
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
        if (!(entity instanceof TextDisplay)) {
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

    private static List<HologramLine> collectLines(NpcAppearanceData appearance) {
        List<HologramLine> lines = new ArrayList<>();
        if (hasText(appearance.name)) {
            lines.add(new HologramLine("name", appearance.name, appearance.nameDisplayScale));
        }
        if (hasText(appearance.description)) {
            lines.add(new HologramLine("description", appearance.description, appearance.descriptionDisplayScale));
        }
        if (appearance.extraLines != null) {
            int extraIndex = 0;
            for (String extra : appearance.extraLines) {
                if (!hasText(extra)) {
                    continue;
                }
                lines.add(new HologramLine("line-" + extraIndex++, extra, appearance.descriptionDisplayScale));
            }
        }
        return lines;
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

    private void removeTaggedInChunk(World world, String npcId, Location base) {
        int chunkX = base.getBlockX() >> 4;
        int chunkZ = base.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }
        for (Entity entity : world.getChunkAt(chunkX, chunkZ).getEntities()) {
            if (!(entity instanceof TextDisplay display)) {
                continue;
            }
            String taggedId = display.getPersistentDataContainer().get(keys.npcId, PersistentDataType.STRING);
            if (npcId.equals(taggedId)) {
                display.remove();
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

    private record HologramLine(String role, String text, float scale) {
    }
}
