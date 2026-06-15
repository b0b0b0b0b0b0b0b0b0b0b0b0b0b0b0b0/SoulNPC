package bm.b0b0b0.SoulNPC.effect;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcGroundItemEffectData;
import bm.b0b0b0.SoulNPC.model.NpcGroundItemEntry;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class NpcGroundItemEffectService {

    private static final int MIN_SCATTER_TICKS = 6;
    private static final int MAX_SCATTER_TICKS = 48;

    private final JavaPlugin plugin;
    private final SoulNpcKeys keys;
    private BukkitTask task;

    public NpcGroundItemEffectService(JavaPlugin plugin, SoulNpcKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    public void start(Collection<NpcRuntime> runtimes) {
        stopTask();
        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> tick(runtimes),
                1L,
                1L
        );
    }

    public void stop() {
        stopTask();
        purgeAll();
    }

    public void burst(NpcRuntime runtime) {
        if (runtime == null) {
            return;
        }
        NpcGroundItemEffectData effect = runtime.data().groundItems;
        effect.ensureItems();
        if (effect.items.isEmpty()) {
            return;
        }
        for (NpcGroundItemEntry entry : effect.items) {
            spawnWave(runtime, entry, effect);
        }
    }

    public boolean isPluginGroundItem(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(keys.groundItem, PersistentDataType.BYTE);
    }

    private void tick(Collection<NpcRuntime> runtimes) {
        for (NpcRuntime runtime : runtimes) {
            NpcGroundItemEffectData effect = runtime.data().groundItems;
            effect.ensureItems();
            if (!effect.enabled || effect.items.isEmpty()) {
                continue;
            }
            if (!runtime.isProfileReady() || runtime.viewers().isEmpty()) {
                runtime.resetGroundItemWaveState();
                continue;
            }
            if (!runtime.consumeGroundItemWaveDue(effect.intervalTicks)) {
                continue;
            }
            int index = runtime.nextGroundItemIndex(effect.items.size());
            spawnWave(runtime, effect.items.get(index), effect);
        }
    }

    private void spawnWave(NpcRuntime runtime, NpcGroundItemEntry entry, NpcGroundItemEffectData effect) {
        if (entry == null) {
            return;
        }
        entry.ensureMaterials();
        int count = Math.max(1, effect.countPerWave);
        for (int waveIndex = 0; waveIndex < count; waveIndex++) {
            Material material = entry.pickRandomMaterial();
            if (material != null) {
                spawnOne(runtime, entry, material, waveIndex, count);
            }
        }
    }

    private void spawnOne(
            NpcRuntime runtime,
            NpcGroundItemEntry entry,
            Material material,
            int waveIndex,
            int waveSize
    ) {
        if (entry == null || material == null || material.isAir()) {
            return;
        }
        NpcFileData data = runtime.data();
        NpcGroundItemEffectData effect = data.groundItems;
        World world = Bukkit.getWorld(data.world);
        if (world == null) {
            return;
        }
        double radius = radiusBlocks(effect);
        Location center = npcCenter(data, world);
        double waveSpread = Math.min(0.35D, radius * 0.18D);
        double angle = (waveIndex / (double) Math.max(1, waveSize)) * Math.PI * 2.0D;
        Location spawnAt = new Location(
                world,
                data.x + Math.cos(angle) * waveSpread + randomOffset(radius),
                data.y + effect.spawnYOffset,
                data.z + Math.sin(angle) * waveSpread + randomOffset(radius)
        );
        if (!world.isChunkLoaded(spawnAt.getBlockX() >> 4, spawnAt.getBlockZ() >> 4)) {
            return;
        }

        int amount = Math.max(1, Math.min(entry.amount, material.getMaxStackSize()));
        ItemStack stack = new ItemStack(material, amount);
        Item item = world.dropItem(spawnAt, stack);
        configure(item);
        applyVelocity(item, effect);
        monitorSettle(item, center, radius);
        scheduleRemove(item.getUniqueId(), effect.lifetimeTicks);
    }

    private void configure(Item item) {
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setUnlimitedLifetime(false);
        item.setPersistent(false);
        item.getPersistentDataContainer().set(keys.groundItem, PersistentDataType.BYTE, (byte) 1);
    }

    private void monitorSettle(Item item, Location center, double radius) {
        UUID itemId = item.getUniqueId();
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            Item tracked = resolveItem(itemId);
            if (tracked == null) {
                task.cancel();
                return;
            }
            int age = tracked.getTicksLived();
            if (age < MIN_SCATTER_TICKS) {
                return;
            }

            boolean onGround = tracked.isOnGround();
            Vector velocity = tracked.getVelocity();
            boolean moving = velocity.lengthSquared() > 0.003D || Math.abs(velocity.getY()) > 0.05D;
            if (!onGround && moving && age < MAX_SCATTER_TICKS) {
                return;
            }

            freezeOnGround(tracked, center, radius);
            task.cancel();
        }, 2L, 1L);
    }

    private void freezeOnGround(Item item, Location center, double radius) {
        Location current = item.getLocation();
        Location clamped = clampToRadius(center, current, radius);
        if (!item.isOnGround() || isInsideBlock(clamped)) {
            clamped = snapToGround(clamped);
        }
        if (clamped.distanceSquared(current) > 0.0001D) {
            item.teleport(clamped);
        }
        item.setVelocity(new Vector(0, 0, 0));
        item.setGravity(false);
    }

    private static boolean isInsideBlock(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        Block feet = world.getBlockAt(location);
        Block below = feet.getRelative(0, -1, 0);
        return feet.getType().isSolid() || (below.getType().isSolid() && location.getY() < below.getY() + 1.0D);
    }

    private Item resolveItem(UUID itemId) {
        Entity entity = Bukkit.getEntity(itemId);
        if (entity instanceof Item item && item.isValid()) {
            return item;
        }
        return null;
    }

    private void applyVelocity(Item item, NpcGroundItemEffectData effect) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double radius = radiusBlocks(effect);
        double angle = random.nextDouble(Math.PI * 2.0D);
        double horizontal = radius * 0.09D * (0.65D + random.nextDouble() * 0.5D);
        double maxVertical = radius * 0.085D;
        double vertical = Math.min(effect.throwUp, maxVertical) * (0.88D + random.nextDouble() * 0.28D);
        item.setVelocity(new Vector(
                Math.cos(angle) * horizontal,
                vertical,
                Math.sin(angle) * horizontal
        ));
    }

    private static double radiusBlocks(NpcGroundItemEffectData effect) {
        return 0.5D + Math.max(0.0D, effect.spread) * 12.0D;
    }

    private static Location npcCenter(NpcFileData data, World world) {
        return new Location(world, data.x, data.y, data.z);
    }

    private static Location clampToRadius(Location center, Location point, double radius) {
        if (center.getWorld() == null || point.getWorld() == null
                || !center.getWorld().equals(point.getWorld())) {
            return point;
        }
        double dx = point.getX() - center.getX();
        double dz = point.getZ() - center.getZ();
        double maxRadius = Math.max(0.35D, radius);
        double distSq = dx * dx + dz * dz;
        if (distSq <= maxRadius * maxRadius) {
            return point;
        }
        double scale = maxRadius / Math.sqrt(distSq);
        return new Location(
                point.getWorld(),
                center.getX() + dx * scale,
                point.getY(),
                center.getZ() + dz * scale,
                point.getYaw(),
                point.getPitch()
        );
    }

    private static double randomOffset(double radius) {
        double span = Math.min(0.2D, radius * 0.08D);
        return (ThreadLocalRandom.current().nextDouble() - 0.5D) * span;
    }

    private static Location snapToGround(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return location;
        }
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int startY = Math.min(location.getBlockY() + 1, world.getMaxHeight() - 1);
        for (int y = startY; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Block above = block.getRelative(0, 1, 0);
            if (!block.getType().isAir() && block.isSolid() && above.getType().isAir()) {
                return new Location(world, location.getX(), y + 1.0625D, location.getZ(), location.getYaw(), location.getPitch());
            }
        }
        return location;
    }

    private void scheduleRemove(UUID entityId, int lifetimeTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }, Math.max(1, lifetimeTicks));
    }

    private void purgeAll() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isPluginGroundItem(entity)) {
                    entity.remove();
                }
            }
        }
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
