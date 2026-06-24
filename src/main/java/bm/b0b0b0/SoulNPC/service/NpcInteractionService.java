package bm.b0b0b0.SoulNPC.service;

import bm.b0b0b0.SoulNPC.api.event.SoulNpcClickEvent;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.model.NpcClickBinding;
import bm.b0b0b0.SoulNPC.model.NpcClickType;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcInteractionAction;
import bm.b0b0b0.SoulNPC.model.NpcInteractionActionParser;
import bm.b0b0b0.SoulNPC.model.NpcInteractionData;
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
    private final ProxyTransferService proxyTransferService;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, Long> clickDedupe = new ConcurrentHashMap<>();

    public NpcInteractionService(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            MessageService messageService,
            ProxyTransferService proxyTransferService
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.messageService = messageService;
        this.proxyTransferService = proxyTransferService;
    }

    public void handleClick(Player player, NpcRuntime runtime, NpcClickType clickType) {
        NpcFileData data = runtime.data();
        data.interaction.ensureActionsMigrated();
        if (!markClick(player.getUniqueId(), data.id, clickType)) {
            return;
        }
        if (!data.enabled || !data.interaction.enabled) {
            player.sendMessage(messageService.message(player, "interaction.disabled"));
            playSound(player, data.interaction.denySound);
            return;
        }
        String usePermission = pluginConfig.settings().permissions.use;
        if (!player.hasPermission(usePermission)) {
            player.sendMessage(messageService.message(player, "interaction.no-permission"));
            playSound(player, data.interaction.denySound);
            return;
        }
        if (data.interaction.permission != null && !data.interaction.permission.isBlank()
                && !player.hasPermission(data.interaction.permission)) {
            player.sendMessage(messageService.message(player, "interaction.no-permission"));
            playSound(player, data.interaction.denySound);
            return;
        }
        if (!player.hasPermission(pluginConfig.settings().permissions.bypassCooldown) && isOnCooldown(player, data)) {
            player.sendMessage(messageService.message(player, "interaction.cooldown"));
            playSound(player, data.interaction.denySound);
            return;
        }
        SoulNpcClickEvent clickEvent = new SoulNpcClickEvent(player, data, clickType);
        Bukkit.getPluginManager().callEvent(clickEvent);
        if (clickEvent.isCancelled()) {
            return;
        }
        List<NpcInteractionAction> actions = data.interaction.actionsFor(clickType);
        if (actions.isEmpty() && NpcInteractionData.hasActionableCommands(commandsForLegacy(data, clickType))) {
            setCooldown(player, data);
            playSound(player, data.interaction.clickSound);
            Bukkit.getScheduler().runTask(plugin, () -> dispatchLegacy(player, data.id, clickType, commandsForLegacy(data, clickType)));
            return;
        }
        if (actions.isEmpty()) {
            return;
        }
        setCooldown(player, data);
        playSound(player, data.interaction.clickSound);
        dispatchActions(player, data.id, actions);
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

    private static List<String> commandsForLegacy(NpcFileData data, NpcClickType clickType) {
        return switch (clickType) {
            case LEFT -> data.interaction.leftClickCommands;
            case RIGHT -> data.interaction.rightClickCommands;
            case MIDDLE -> data.interaction.middleClickCommands;
            case SHIFT_LEFT -> data.interaction.shiftLeftClickCommands;
            case SHIFT_RIGHT -> data.interaction.shiftRightClickCommands;
        };
    }

    private void dispatchActions(Player player, String npcId, List<NpcInteractionAction> actions) {
        int accumulatedDelay = 0;
        for (NpcInteractionAction action : actions) {
            accumulatedDelay += Math.max(0, action.delayTicks);
            int runDelay = accumulatedDelay;
            Bukkit.getScheduler().runTaskLater(plugin, () -> executeAction(player, npcId, action), runDelay);
        }
    }

    private void executeAction(Player player, String npcId, NpcInteractionAction action) {
        if (action.cooldownSeconds > 0) {
            String key = player.getUniqueId() + ":" + npcId + ":action:" + System.identityHashCode(action);
            Long last = cooldowns.get(key);
            long cooldownMs = action.cooldownSeconds * 1000L;
            if (last != null && System.currentTimeMillis() - last < cooldownMs) {
                return;
            }
            cooldowns.put(key, System.currentTimeMillis());
        }
        String value = applyPlaceholders(action.value, player, npcId);
        switch (action.type) {
            case MESSAGE -> sendMessage(player, value);
            case SWITCH_SERVER -> proxyTransferService.transfer(player, value);
            case CONSOLE_CMD -> executeCommand(player, value, CommandTarget.CONSOLE);
            case OP_CMD -> executeCommand(player, value, CommandTarget.OP);
            case PLAYER_CMD -> executeCommand(player, value, CommandTarget.PLAYER);
        }
    }

    private void dispatchLegacy(Player player, String npcId, NpcClickType clickType, List<String> commands) {
        NpcClickBinding binding = NpcClickBinding.fromClickType(clickType);
        for (String raw : commands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.startsWith("#")) {
                continue;
            }
            NpcInteractionAction action = NpcInteractionActionParser.fromLegacyLine(trimmed, binding);
            executeAction(player, npcId, action);
        }
    }

    private void sendMessage(Player player, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String resolved = PlaceholderService.apply(player, text);
        player.sendMessage(messageService.raw(resolved));
    }

    private void executeCommand(Player player, String command, CommandTarget target) {
        if (command == null || command.isBlank()) {
            return;
        }
        String resolved = PlaceholderService.apply(player, applyPlaceholders(command, player, ""));
        if (resolved.startsWith("/")) {
            resolved = resolved.substring(1);
        }
        execute(player, resolved, target);
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
