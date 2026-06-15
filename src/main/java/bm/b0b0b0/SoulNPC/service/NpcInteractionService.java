package bm.b0b0b0.SoulNPC.service;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.model.NpcClickType;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcInteractionService {

    private static final long CLICK_DEDUPE_MS = 120L;

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final MessageService messageService;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, Long> clickDedupe = new ConcurrentHashMap<>();

    public NpcInteractionService(JavaPlugin plugin, PluginConfig pluginConfig, MessageService messageService) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.messageService = messageService;
    }

    public boolean handleClick(Player player, NpcRuntime runtime, NpcClickType clickType) {
        NpcFileData data = runtime.data();
        if (!markClick(player.getUniqueId(), data.id, clickType)) {
            return false;
        }
        if (!data.enabled || !data.interaction.enabled) {
            player.sendMessage(messageService.message(player, "interaction.disabled"));
            playSound(player, data.interaction.denySound);
            return false;
        }
        String usePermission = pluginConfig.settings().permissions.use;
        if (!player.hasPermission(usePermission)) {
            player.sendMessage(messageService.message(player, "interaction.no-permission"));
            playSound(player, data.interaction.denySound);
            return false;
        }
        if (data.interaction.permission != null && !data.interaction.permission.isBlank() && !player.hasPermission(data.interaction.permission)) {
            player.sendMessage(messageService.message(player, "interaction.no-permission"));
            playSound(player, data.interaction.denySound);
            return false;
        }
        if (!player.hasPermission(pluginConfig.settings().permissions.bypassCooldown) && isOnCooldown(player, data)) {
            player.sendMessage(messageService.message(player, "interaction.cooldown"));
            playSound(player, data.interaction.denySound);
            return false;
        }
        List<String> commands = commandsFor(data, clickType);
        if (commands == null || commands.isEmpty()) {
            return false;
        }
        setCooldown(player, data);
        playSound(player, data.interaction.clickSound);
        Bukkit.getScheduler().runTask(plugin, () -> dispatchActions(player, data.id, commands));
        return true;
    }

    private boolean markClick(UUID playerId, String npcId, NpcClickType clickType) {
        String key = playerId + ":" + npcId + ":" + clickType.name();
        long now = System.currentTimeMillis();
        Long last = clickDedupe.get(key);
        if (last != null && now - last < CLICK_DEDUPE_MS) {
            return false;
        }
        clickDedupe.put(key, now);
        return true;
    }

    private static List<String> commandsFor(NpcFileData data, NpcClickType clickType) {
        return switch (clickType) {
            case LEFT -> data.interaction.leftClickCommands;
            case RIGHT -> data.interaction.rightClickCommands;
            case MIDDLE -> data.interaction.middleClickCommands;
            case SHIFT_LEFT -> data.interaction.shiftLeftClickCommands;
            case SHIFT_RIGHT -> data.interaction.shiftRightClickCommands;
        };
    }

    private void dispatchActions(Player player, String npcId, List<String> commands) {
        for (String raw : commands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.regionMatches(true, 0, "[message]", 0, 9)) {
                sendMessage(player, npcId, trimmed.substring(9).trim());
                continue;
            }
            CommandTarget target = CommandTarget.PLAYER;
            String command = trimmed;
            if (trimmed.startsWith("[console]")) {
                target = CommandTarget.CONSOLE;
                command = trimmed.substring(9).trim();
            } else if (trimmed.startsWith("[op]")) {
                target = CommandTarget.OP;
                command = trimmed.substring(4).trim();
            } else if (trimmed.startsWith("[player]")) {
                target = CommandTarget.PLAYER;
                command = trimmed.substring(8).trim();
            }
            command = applyPlaceholders(command, player, npcId);
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            execute(player, command, target);
        }
    }

    private void sendMessage(Player player, String npcId, String text) {
        if (text.isBlank()) {
            return;
        }
        String resolved = applyPlaceholders(text, player, npcId);
        player.sendMessage(messageService.raw(resolved));
    }

    private static String applyPlaceholders(String input, Player player, String npcId) {
        return input
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{npc}", npcId)
                .replace("{world}", player.getWorld().getName());
    }

    private void execute(Player player, String command, CommandTarget target) {
        switch (target) {
            case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            case OP -> {
                boolean wasOp = player.isOp();
                try {
                    player.setOp(true);
                    player.performCommand(command);
                } finally {
                    player.setOp(wasOp);
                }
            }
            case PLAYER -> player.performCommand(command);
        }
    }

    private boolean isOnCooldown(Player player, NpcFileData data) {
        long cooldown = data.interaction.cooldownMs > 0
                ? data.interaction.cooldownMs
                : pluginConfig.settings().performance.defaultInteractionCooldownMs;
        String key = player.getUniqueId() + ":" + data.id;
        Long last = cooldowns.get(key);
        if (last == null) {
            return false;
        }
        return System.currentTimeMillis() - last < cooldown;
    }

    private void setCooldown(Player player, NpcFileData data) {
        long cooldown = data.interaction.cooldownMs > 0
                ? data.interaction.cooldownMs
                : pluginConfig.settings().performance.defaultInteractionCooldownMs;
        if (cooldown <= 0) {
            return;
        }
        cooldowns.put(player.getUniqueId() + ":" + data.id, System.currentTimeMillis());
    }

    private void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isBlank()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.trim().toUpperCase());
            player.playSound(player.getLocation(), sound, 1F, 1F);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private enum CommandTarget {
        CONSOLE,
        OP,
        PLAYER
    }
}
