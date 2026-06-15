package bm.b0b0b0.SoulNPC.command;

import bm.b0b0b0.SoulNPC.appearance.SkinService;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.gui.AdminNpcMenuListener;
import bm.b0b0b0.SoulNPC.gui.GuiChatInputService;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.mob.NpcCreateTypeParser;
import bm.b0b0b0.SoulNPC.mob.NpcEntityTypeResolver;
import bm.b0b0b0.SoulNPC.model.NpcDisplayType;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import bm.b0b0b0.SoulNPC.service.NpcService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SoulNpcCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final MessageService messageService;
    private final NpcRepository repository;
    private final NpcService npcService;
    private final SkinService skinService;
    private final AdminNpcMenuListener adminMenuListener;
    private final GuiChatInputService chatInputService;

    public SoulNpcCommand(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            MessageService messageService,
            NpcRepository repository,
            NpcService npcService,
            SkinService skinService,
            AdminNpcMenuListener adminMenuListener,
            GuiChatInputService chatInputService
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.messageService = messageService;
        this.repository = repository;
        this.npcService = npcService;
        this.skinService = skinService;
        this.adminMenuListener = adminMenuListener;
        this.chatInputService = chatInputService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "help" -> {
                sendHelp(sender);
                yield true;
            }
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "edit" -> handleEdit(sender);
            case "tp" -> handleTeleport(sender, args);
            case "info" -> handleInfo(sender, args);
            case "pose" -> handlePose(sender, args);
            case "respawn" -> handleRespawn(sender, args);
            case "skin" -> handleSkin(sender, args);
            case "guicancel" -> handleGuiCancel(sender);
            default -> {
                sender.sendMessage(messageService.message(asPlayer(sender), "command.unknown-subcommand"));
                yield true;
            }
        };
    }

    private boolean handleReload(CommandSender sender) {
        if (!checkPermission(sender, pluginConfig.settings().permissions.admin)) {
            return true;
        }
        pluginConfig.reload(plugin);
        messageService.reload(plugin);
        npcService.reload();
        sender.sendMessage(messageService.message(asPlayer(sender), "command.reload-success"));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!checkPermission(sender, pluginConfig.settings().permissions.admin)) {
            return true;
        }
        List<NpcFileData> npcs = new ArrayList<>(repository.findAll());
        if (npcs.isEmpty()) {
            sender.sendMessage(messageService.message(asPlayer(sender), "command.list-empty"));
            return true;
        }
        sender.sendMessage(messageService.message(
                asPlayer(sender),
                "command.list-header",
                Placeholder.parsed("count", String.valueOf(npcs.size()))
        ));
        for (NpcFileData data : npcs) {
            sender.sendMessage(messageService.message(
                    asPlayer(sender),
                    "command.list-entry",
                    Placeholder.parsed("npc", data.id),
                    Placeholder.parsed("world", data.world),
                    Placeholder.parsed("x", String.valueOf((int) data.x)),
                    Placeholder.parsed("y", String.valueOf((int) data.y)),
                    Placeholder.parsed("z", String.valueOf((int) data.z))
            ));
        }
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.message(null, "command.player-only"));
            return true;
        }
        if (!checkPermission(sender, pluginConfig.settings().permissions.create)) {
            return true;
        }
        if (args.length < 2 || !isValidId(args[1])) {
            sender.sendMessage(messageService.message(player, "command.invalid-id"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        NpcDisplayType type = NpcDisplayType.PLAYER;
        String entityType = null;
        NpcMobDisplayPose mobDisplayPose = NpcMobDisplayPose.STANDING;
        if (args.length >= 3) {
            String typeArg = args[2];
            if ("player".equalsIgnoreCase(typeArg)) {
                type = NpcDisplayType.PLAYER;
            } else {
                NpcCreateTypeParser.ParsedMob parsed = NpcCreateTypeParser.parseMob(typeArg).orElse(null);
                if (parsed == null) {
                    sender.sendMessage(messageService.message(player, "command.invalid-type"));
                    return true;
                }
                type = NpcDisplayType.MOB;
                entityType = parsed.entityType();
                mobDisplayPose = parsed.mobDisplayPose();
            }
        }
        final NpcDisplayType createType = type;
        final String createEntityType = entityType;
        final NpcMobDisplayPose createMobDisplayPose = mobDisplayPose;
        if (!npcService.createAt(player, id, createType, createEntityType, createMobDisplayPose)) {
            sender.sendMessage(messageService.message(player, "command.create-exists", Placeholder.parsed("npc", id)));
            return true;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (npcService.findRuntime(id).map(runtime -> !runtime.isSpawned()).orElse(true)) {
                player.sendMessage(messageService.message(player, "command.spawn-failed", Placeholder.parsed("npc", id)));
                return;
            }
            player.sendMessage(messageService.message(
                    player,
                    "command.create-success",
                    Placeholder.parsed("npc", id),
                    Placeholder.parsed("type", formatCreateTypeLabel(createType, createEntityType))
            ));
        }, 20L);
        return true;
    }

    private boolean handleGuiCancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        chatInputService.cancel(player);
        return true;
    }

    private boolean handleRespawn(CommandSender sender, String[] args) {
        if (!checkPermission(sender, pluginConfig.settings().permissions.admin)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messageService.message(asPlayer(sender), "command.invalid-id"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (repository.findById(id).isEmpty()) {
            sender.sendMessage(messageService.message(asPlayer(sender), "command.delete-missing", Placeholder.parsed("npc", id)));
            return true;
        }
        if (!npcService.respawn(id)) {
            sender.sendMessage(messageService.message(asPlayer(sender), "command.spawn-failed", Placeholder.parsed("npc", id)));
            return true;
        }
        sender.sendMessage(messageService.message(asPlayer(sender), "command.respawn-success", Placeholder.parsed("npc", id)));
        return true;
    }

    private boolean handleSkin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.message(null, "command.player-only"));
            return true;
        }
        if (!checkPermission(sender, pluginConfig.settings().permissions.admin)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messageService.message(player, "command.skin-usage"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (repository.findById(id).isEmpty()) {
            sender.sendMessage(messageService.message(player, "command.delete-missing", Placeholder.parsed("npc", id)));
            return true;
        }
        String profile = args.length >= 3 ? args[2] : skinService.resolveProfileKeyForPlayer(player);
        if (!npcService.setSkin(id, profile)) {
            sender.sendMessage(messageService.message(player, "command.skin-not-player", Placeholder.parsed("npc", id)));
            return true;
        }
        sender.sendMessage(messageService.message(
                player,
                "command.skin-success",
                Placeholder.parsed("npc", id),
                Placeholder.parsed("profile", profile)
        ));
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!checkPermission(sender, pluginConfig.settings().permissions.delete)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messageService.message(asPlayer(sender), "command.invalid-id"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!npcService.delete(id)) {
            sender.sendMessage(messageService.message(asPlayer(sender), "command.delete-missing", Placeholder.parsed("npc", id)));
            return true;
        }
        sender.sendMessage(messageService.message(asPlayer(sender), "command.delete-success", Placeholder.parsed("npc", id)));
        return true;
    }

    private boolean handleEdit(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.message(null, "command.player-only"));
            return true;
        }
        if (!checkPermission(sender, pluginConfig.settings().permissions.edit)) {
            return true;
        }
        adminMenuListener.openAdmin(player);
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.message(null, "command.player-only"));
            return true;
        }
        if (!checkPermission(sender, pluginConfig.settings().permissions.admin)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messageService.message(player, "command.invalid-id"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        NpcRuntime runtime = npcService.findRuntime(id).orElse(null);
        if (runtime == null) {
            sender.sendMessage(messageService.message(player, "command.delete-missing", Placeholder.parsed("npc", id)));
            return true;
        }
        NpcFileData data = runtime.data();
        Location location = new Location(Bukkit.getWorld(data.world), data.x, data.y, data.z, data.yaw, data.pitch);
        player.teleportAsync(location);
        sender.sendMessage(messageService.message(player, "command.tp-success", Placeholder.parsed("npc", id)));
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!checkPermission(sender, pluginConfig.settings().permissions.admin)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messageService.message(asPlayer(sender), "command.invalid-id"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        NpcFileData data = repository.findById(id).orElse(null);
        if (data == null) {
            sender.sendMessage(messageService.message(asPlayer(sender), "command.delete-missing", Placeholder.parsed("npc", id)));
            return true;
        }
        Player viewer = asPlayer(sender);
        sender.sendMessage(messageService.message(viewer, "command.info-header", Placeholder.parsed("npc", id)));
        sendInfoLine(sender, viewer, "world", data.world);
        sendInfoLine(sender, viewer, "location", data.x + ", " + data.y + ", " + data.z);
        sendInfoLine(sender, viewer, "enabled", String.valueOf(data.enabled));
        sendInfoLine(sender, viewer, "display", formatInfoDisplay(data));
        sendInfoLine(sender, viewer, "animation", data.animation.enabled ? data.animation.type.name() : "NONE");
        sendInfoLine(sender, viewer, "commands", String.valueOf(data.interaction.rightClickCommands.size()));
        return true;
    }

    private boolean handlePose(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.message(null, "command.player-only"));
            return true;
        }
        if (!checkPermission(sender, pluginConfig.settings().permissions.edit)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messageService.message(player, "command.invalid-id"));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if ("copy".equals(action)) {
            npcService.copyPoseFrom(player);
            sender.sendMessage(messageService.message(player, "command.pose-copied"));
            return true;
        }
        if ("apply".equals(action)) {
            if (args.length < 3 || !isValidId(args[2])) {
                sender.sendMessage(messageService.message(player, "command.invalid-id"));
                return true;
            }
            if (!npcService.applyBufferedPose(args[2].toLowerCase(Locale.ROOT))) {
                sender.sendMessage(messageService.message(player, "command.delete-missing", Placeholder.parsed("npc", args[2])));
                return true;
            }
            sender.sendMessage(messageService.message(player, "command.pose-applied", Placeholder.parsed("npc", args[2])));
            return true;
        }
        sender.sendMessage(messageService.message(player, "command.unknown-subcommand"));
        return true;
    }

    private void sendInfoLine(CommandSender sender, Player viewer, String key, String value) {
        sender.sendMessage(messageService.message(
                viewer,
                "command.info-line",
                Placeholder.parsed("key", key),
                Placeholder.parsed("value", value)
        ));
    }

    private void sendHelp(CommandSender sender) {
        for (net.kyori.adventure.text.Component line : messageService.messageList(asPlayer(sender), "help")) {
            sender.sendMessage(line);
        }
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(messageService.message(asPlayer(sender), "command.no-permission"));
        return false;
    }

    private static Player asPlayer(CommandSender sender) {
        return sender instanceof Player player ? player : null;
    }

    private static boolean isValidId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        for (int index = 0; index < id.length(); index++) {
            char character = id.charAt(index);
            if (Character.isLetterOrDigit(character) || character == '_' || character == '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return filter(args[0], "help", "reload", "list", "create", "delete", "edit", "tp", "info", "pose", "respawn", "skin");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("delete".equals(sub) || "tp".equals(sub) || "info".equals(sub) || "respawn".equals(sub) || "skin".equals(sub)) {
                List<String> ids = new ArrayList<>();
                for (NpcFileData data : repository.findAll()) {
                    ids.add(data.id);
                }
                return filter(args[1], ids.toArray(String[]::new));
            }
            if ("pose".equals(sub)) {
                return filter(args[1], "copy", "apply");
            }
        }
        if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            return filter(args[2], NpcCreateTypeParser.createTabChoices());
        }
        if (args.length == 3 && "pose".equalsIgnoreCase(args[0]) && "apply".equalsIgnoreCase(args[1])) {
            List<String> ids = new ArrayList<>();
            for (NpcFileData data : repository.findAll()) {
                ids.add(data.id);
            }
            return filter(args[2], ids.toArray(String[]::new));
        }
        return List.of();
    }

    private static String formatCreateTypeLabel(NpcDisplayType type, String entityType) {
        if (type.isPlayerModel() || entityType == null) {
            return "player";
        }
        return entityType;
    }

    private static String formatInfoDisplay(NpcFileData data) {
        if (data.appearance.isPacketMob()) {
            String entity = data.appearance.entityType;
            if (entity == null || entity.isBlank()) {
                entity = data.appearance.resolvedEntityType();
            }
            return entity == null ? "mob" : entity;
        }
        return "player";
    }

    private static List<String> filter(String input, String... options) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
