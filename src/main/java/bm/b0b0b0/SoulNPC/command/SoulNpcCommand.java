package bm.b0b0b0.SoulNPC.command;

import bm.b0b0b0.SoulNPC.appearance.SkinService;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.gui.AdminNpcMenuListener;
import bm.b0b0b0.SoulNPC.gui.GuiChatInputService;
import bm.b0b0b0.SoulNPC.importing.ZnpcsPlusImporter;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.mob.NpcCreateTypeParser;
import bm.b0b0b0.SoulNPC.model.NpcSkinSource;
import bm.b0b0b0.SoulNPC.model.NpcDisplayType;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcRuntime;
import bm.b0b0b0.SoulNPC.service.NpcService;
import bm.b0b0b0.SoulNPC.storage.NpcMigrationService;
import bm.b0b0b0.SoulNPC.storage.StorageType;
import bm.b0b0b0.SoulNPC.util.NpcIdValidator;
import bm.b0b0b0.SoulNPC.util.NpcInspectorStick;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import bm.b0b0b0.SoulNPC.util.SoulNpcPermissionChecks;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SoulNpcCommand implements CommandExecutor, TabCompleter {

    private record CommandContext(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            MessageService messageService,
            NpcRepository repository,
            NpcService npcService,
            SkinService skinService,
            AdminNpcMenuListener adminMenuListener,
            GuiChatInputService chatInputService,
            SoulNpcKeys soulNpcKeys,
            NpcMigrationService migrationService
    ) {
    }

    private final CommandContext ctx;

    public SoulNpcCommand(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            MessageService messageService,
            NpcRepository repository,
            NpcService npcService,
            SkinService skinService,
            AdminNpcMenuListener adminMenuListener,
            GuiChatInputService chatInputService,
            SoulNpcKeys soulNpcKeys,
            NpcMigrationService migrationService
    ) {
        this.ctx = new CommandContext(
                plugin,
                pluginConfig,
                messageService,
                repository,
                npcService,
                skinService,
                adminMenuListener,
                chatInputService,
                soulNpcKeys,
                migrationService
        );
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            if (!SoulNpcPermissionChecks.requireAnyCommandAccess(sender, ctx.pluginConfig(), ctx.messageService())) {
                return true;
            }
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "help" -> {
                if (!SoulNpcPermissionChecks.requireAnyCommandAccess(sender, ctx.pluginConfig(), ctx.messageService())) {
                    yield true;
                }
                sendHelp(sender);
                yield true;
            }
            case "reload" -> handleReload(sender);
            case "migrate" -> handleMigrate(sender, args);
            case "list" -> handleList(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "edit" -> handleEdit(sender);
            case "tp" -> handleTeleport(sender, args);
            case "info" -> handleInfo(sender, args);
            case "pose" -> handlePose(sender, args);
            case "respawn" -> handleRespawn(sender, args);
            case "skin" -> handleSkin(sender, args);
            case "import" -> handleImport(sender, args);
            case "stick" -> handleStick(sender);
            case "guicancel" -> handleGuiCancel(sender);
            default -> {
                sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.unknown-subcommand"));
                yield true;
            }
        };
    }

    private boolean handleReload(CommandSender sender) {
        if (!SoulNpcPermissionChecks.requireAdmin(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        StorageType previous = StorageType.fromConfig(ctx.pluginConfig().settings().storage.type);
        ctx.pluginConfig().reload(ctx.plugin());
        StorageType current = StorageType.fromConfig(ctx.pluginConfig().settings().storage.type);
        ctx.messageService().reload(ctx.plugin());
        ctx.npcService().reload();
        sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.reload-success"));
        if (previous != current) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.storage-type-restart"));
        }
        return true;
    }

    private boolean handleMigrate(CommandSender sender, String[] args) {
        if (!SoulNpcPermissionChecks.requireAdmin(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.migrate-usage"));
            return true;
        }
        boolean dryRun = false;
        boolean overwrite = false;
        for (int index = 3; index < args.length; index++) {
            String flag = args[index].toLowerCase(Locale.ROOT);
            if ("--dry-run".equals(flag)) {
                dryRun = true;
            } else if ("--overwrite".equals(flag)) {
                overwrite = true;
            }
        }
        StorageType from;
        StorageType to;
        try {
            from = NpcMigrationService.parseType(args[1]);
            to = NpcMigrationService.parseType(args[2]);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.migrate-invalid-type"));
            return true;
        }
        if (from == to) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.migrate-same-type"));
            return true;
        }
        sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.migrate-started"));
        StorageType finalFrom = from;
        StorageType finalTo = to;
        boolean finalDryRun = dryRun;
        boolean finalOverwrite = overwrite;
        ctx.migrationService().migrate(from, to, dryRun, overwrite).whenComplete((result, error) ->
                NpcMigrationService.runOnMain(ctx.plugin(), () -> {
                    if (error != null) {
                        sender.sendMessage(ctx.messageService().message(
                                asPlayer(sender),
                                "command.migrate-failed",
                                Placeholder.parsed("reason", error.getMessage() == null ? "?" : error.getMessage())
                        ));
                        return;
                    }
                    sender.sendMessage(ctx.messageService().message(
                            asPlayer(sender),
                            finalDryRun ? "command.migrate-dry-run-success" : "command.migrate-success",
                            Placeholder.parsed("from", finalFrom.configKey()),
                            Placeholder.parsed("to", finalTo.configKey()),
                            Placeholder.parsed("imported", String.valueOf(result.imported())),
                            Placeholder.parsed("skipped", String.valueOf(result.skipped())),
                            Placeholder.parsed("errors", String.valueOf(result.errors()))
                    ));
                    if (!finalDryRun) {
                        sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.migrate-not-active"));
                    }
                }));
        return true;
    }

    private boolean requireReady(CommandSender sender) {
        if (ctx.repository().isReady() || !ctx.repository().findAll().isEmpty()) {
            return true;
        }
        if (ctx.repository().isLoading()) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.storage-loading"));
            return false;
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!SoulNpcPermissionChecks.requireAdmin(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        if (!requireReady(sender)) {
            return true;
        }
        List<NpcFileData> npcs = new ArrayList<>(ctx.repository().findAll());
        if (npcs.isEmpty()) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.list-empty"));
            return true;
        }
        sender.sendMessage(ctx.messageService().message(
                asPlayer(sender),
                "command.list-header",
                Placeholder.parsed("count", String.valueOf(npcs.size()))
        ));
        for (NpcFileData data : npcs) {
            sender.sendMessage(ctx.messageService().message(
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
            sender.sendMessage(ctx.messageService().message(null, "command.player-only"));
            return true;
        }
        if (!SoulNpcPermissionChecks.requireCreate(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        if (!requireReady(sender)) {
            return true;
        }
        Optional<NpcCreateArgsParser.Result> parsed = NpcCreateArgsParser.parse(args, ctx.repository());
        if (parsed.isEmpty()) {
            Optional<NpcCreateArgsParser.Failure> failure = NpcCreateArgsParser.failureReason(args);
            if (failure.orElse(null) == NpcCreateArgsParser.Failure.INVALID_TYPE) {
                sender.sendMessage(ctx.messageService().message(player, "command.invalid-type"));
            } else {
                sender.sendMessage(ctx.messageService().message(player, "command.invalid-id"));
            }
            return true;
        }
        NpcCreateArgsParser.Result request = parsed.get();
        String id = request.id();
        NpcDisplayType createType = request.type();
        String createEntityType = request.entityType();
        NpcMobDisplayPose createMobDisplayPose = request.mobDisplayPose();
        String skinProfile = request.skinProfile();
        if (!ctx.npcService().createAt(
                player,
                id,
                createType,
                createEntityType,
                createMobDisplayPose,
                skinProfile,
                () -> sendCreateSuccess(player, request, id, createType, createEntityType),
                error -> {
                    if (player.isOnline()) {
                        player.sendMessage(ctx.messageService().message(
                                player,
                                "command.spawn-failed",
                                Placeholder.parsed("npc", id)
                        ));
                    }
                }
        )) {
            sender.sendMessage(ctx.messageService().message(player, "command.create-exists", Placeholder.parsed("npc", id)));
            return true;
        }
        return true;
    }

    private void sendCreateSuccess(
            Player player,
            NpcCreateArgsParser.Result request,
            String id,
            NpcDisplayType createType,
            String createEntityType
    ) {
        if (!player.isOnline()) {
            return;
        }
        if (request.autoId()) {
            player.sendMessage(ctx.messageService().message(
                    player,
                    "command.create-success-auto",
                    Placeholder.parsed("npc", id),
                    Placeholder.parsed("type", formatCreateTypeLabel(createType, createEntityType))
            ));
            return;
        }
        player.sendMessage(ctx.messageService().message(
                player,
                "command.create-success",
                Placeholder.parsed("npc", id),
                Placeholder.parsed("type", formatCreateTypeLabel(createType, createEntityType))
        ));
    }

    private boolean handleGuiCancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (!SoulNpcPermissionChecks.requireEditGui(player, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        ctx.chatInputService().cancel(player);
        return true;
    }

    private boolean handleRespawn(CommandSender sender, String[] args) {
        if (!SoulNpcPermissionChecks.requireAdmin(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.invalid-id"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (ctx.repository().findById(id).isEmpty()) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.delete-missing", Placeholder.parsed("npc", id)));
            return true;
        }
        if (!ctx.npcService().respawn(id)) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.spawn-failed", Placeholder.parsed("npc", id)));
            return true;
        }
        sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.respawn-success", Placeholder.parsed("npc", id)));
        return true;
    }

    private boolean handleSkin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ctx.messageService().message(null, "command.player-only"));
            return true;
        }
        if (!SoulNpcPermissionChecks.requireAdmin(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ctx.messageService().message(player, "command.skin-usage"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (ctx.repository().findById(id).isEmpty()) {
            sendNpcNotFound(sender, player, id);
            return true;
        }
        NpcFileData npcData = ctx.repository().findById(id).get();
        if (!npcData.appearance.type.isPlayerModel()) {
            sender.sendMessage(ctx.messageService().message(player, "command.skin-not-player", Placeholder.parsed("npc", id)));
            return true;
        }
        if (args.length >= 4 && "url".equalsIgnoreCase(args[2])) {
            String url = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
            return applySkin(player, id, NpcSkinSource.URL, "", url, "", url);
        }
        if (args.length >= 4 && "file".equalsIgnoreCase(args[2])) {
            String file = args[3].trim();
            return applySkin(player, id, NpcSkinSource.FILE, "", "", file, file);
        }
        String profile = args.length >= 3
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim()
                : ctx.skinService().resolveProfileKeyForPlayer(player);
        if (profile.isBlank()) {
            sender.sendMessage(ctx.messageService().message(player, "command.skin-usage"));
            return true;
        }
        return applySkin(player, id, NpcSkinSource.NICK, profile, "", "", profile);
    }

    private boolean applySkin(
            Player player,
            String id,
            NpcSkinSource source,
            String profile,
            String skinUrl,
            String skinFile,
            String displayLabel
    ) {
        player.sendMessage(ctx.messageService().message(
                player,
                "command.skin-loading",
                Placeholder.parsed("npc", id),
                Placeholder.parsed("profile", displayLabel)
        ));
        if (!ctx.npcService().setSkin(id, source, profile, skinUrl, skinFile, () -> player.sendMessage(ctx.messageService().message(
                player,
                "command.skin-success",
                Placeholder.parsed("npc", id),
                Placeholder.parsed("profile", displayLabel)
        )), error -> player.sendMessage(ctx.messageService().message(
                player,
                "command.skin-failed",
                Placeholder.parsed("npc", id),
                Placeholder.parsed("profile", displayLabel),
                Placeholder.parsed("reason", error.getMessage() == null ? "?" : error.getMessage())
        )))) {
            player.sendMessage(ctx.messageService().message(player, "command.skin-failed", Placeholder.parsed("npc", id),
                    Placeholder.parsed("profile", displayLabel), Placeholder.parsed("reason", "respawn")));
            return true;
        }
        return true;
    }

    private boolean handleImport(CommandSender sender, String[] args) {
        if (!SoulNpcPermissionChecks.requireAdmin(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        Player player = asPlayer(sender);
        if (args.length < 2 || !"znpcsplus".equalsIgnoreCase(args[1])) {
            sender.sendMessage(ctx.messageService().message(player, "command.import-usage"));
            return true;
        }
        Path folder = args.length >= 3
                ? Path.of(String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim())
                : ctx.plugin().getDataFolder().toPath().getParent().resolve("ZNPCsPlus").resolve("npcs");
        sender.sendMessage(ctx.messageService().message(
                player,
                "command.import-started",
                Placeholder.parsed("path", folder.toString())
        ));
        ZnpcsPlusImporter.readFolderAsync(folder).thenAccept(imported -> ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> {
            int importedCount = 0;
            int skipped = 0;
            int errors = 0;
            for (NpcFileData data : imported.values()) {
                if (ctx.repository().findById(data.id).isPresent()) {
                    skipped++;
                    continue;
                }
                if (!ctx.npcService().create(data)) {
                    errors++;
                } else {
                    importedCount++;
                }
            }
            sender.sendMessage(ctx.messageService().message(
                    player,
                    "command.import-success",
                    Placeholder.parsed("imported", String.valueOf(importedCount)),
                    Placeholder.parsed("skipped", String.valueOf(skipped)),
                    Placeholder.parsed("errors", String.valueOf(errors)),
                    Placeholder.parsed("path", folder.toString())
            ));
        }));
        return true;
    }

    private boolean handleStick(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ctx.messageService().message(null, "command.player-only"));
            return true;
        }
        if (!SoulNpcPermissionChecks.requireAdmin(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        ItemStack stick = NpcInspectorStick.create(ctx.messageService(), player, ctx.soulNpcKeys());
        player.getInventory().addItem(stick).values().forEach(overflow ->
                player.getWorld().dropItemNaturally(player.getLocation(), overflow)
        );
        sender.sendMessage(ctx.messageService().message(player, "command.stick-given"));
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!SoulNpcPermissionChecks.requireDelete(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.invalid-id"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!ctx.npcService().delete(id)) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.delete-missing", Placeholder.parsed("npc", id)));
            return true;
        }
        sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.delete-success", Placeholder.parsed("npc", id)));
        return true;
    }

    private boolean handleEdit(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ctx.messageService().message(null, "command.player-only"));
            return true;
        }
        if (!SoulNpcPermissionChecks.requireEditGui(player, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        ctx.adminMenuListener().openAdmin(player);
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ctx.messageService().message(null, "command.player-only"));
            return true;
        }
        if (!SoulNpcPermissionChecks.requireAdmin(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ctx.messageService().message(player, "command.invalid-id"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        NpcRuntime runtime = ctx.npcService().findRuntime(id).orElse(null);
        if (runtime == null) {
            sender.sendMessage(ctx.messageService().message(player, "command.delete-missing", Placeholder.parsed("npc", id)));
            return true;
        }
        NpcFileData data = runtime.data();
        Location location = new Location(Bukkit.getWorld(data.world), data.x, data.y, data.z, data.yaw, data.pitch);
        player.teleportAsync(location).thenAccept(success -> {
            if (success) {
                player.sendMessage(ctx.messageService().message(player, "command.tp-success", Placeholder.parsed("npc", id)));
            }
        });
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!SoulNpcPermissionChecks.requireAdmin(sender, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.invalid-id"));
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        NpcFileData data = ctx.repository().findById(id).orElse(null);
        if (data == null) {
            sender.sendMessage(ctx.messageService().message(asPlayer(sender), "command.delete-missing", Placeholder.parsed("npc", id)));
            return true;
        }
        Player viewer = asPlayer(sender);
        sender.sendMessage(ctx.messageService().message(viewer, "command.info-header", Placeholder.parsed("npc", id)));
        sendInfoLine(sender, viewer, "world", data.world);
        sendInfoLine(sender, viewer, "location", data.x + ", " + data.y + ", " + data.z);
        sendInfoLine(sender, viewer, "enabled", String.valueOf(data.enabled));
        sendInfoLine(sender, viewer, "display", formatInfoDisplay(data));
        sendInfoLine(sender, viewer, "animation", data.animation.enabled ? data.animation.type.name() : "NONE");
        sendInfoLine(sender, viewer, "commands", String.valueOf(data.interaction.actionableActionCount()));
        return true;
    }

    private boolean handlePose(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ctx.messageService().message(null, "command.player-only"));
            return true;
        }
        if (!SoulNpcPermissionChecks.requireEditGui(player, ctx.pluginConfig(), ctx.messageService())) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ctx.messageService().message(player, "command.invalid-id"));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if ("copy".equals(action)) {
            ctx.npcService().copyPoseFrom(player);
            sender.sendMessage(ctx.messageService().message(player, "command.pose-copied"));
            return true;
        }
        if ("apply".equals(action)) {
            if (args.length < 3 || !NpcIdValidator.isValidId(args[2])) {
                sender.sendMessage(ctx.messageService().message(player, "command.invalid-id"));
                return true;
            }
            if (!ctx.npcService().applyBufferedPose(args[2].toLowerCase(Locale.ROOT))) {
                sender.sendMessage(ctx.messageService().message(player, "command.delete-missing", Placeholder.parsed("npc", args[2])));
                return true;
            }
            sender.sendMessage(ctx.messageService().message(player, "command.pose-applied", Placeholder.parsed("npc", args[2])));
            return true;
        }
        sender.sendMessage(ctx.messageService().message(player, "command.unknown-subcommand"));
        return true;
    }

    private void sendInfoLine(CommandSender sender, Player viewer, String key, String value) {
        sender.sendMessage(ctx.messageService().message(
                viewer,
                "command.info-line",
                Placeholder.parsed("key", key),
                Placeholder.parsed("value", value)
        ));
    }

    private void sendHelp(CommandSender sender) {
        for (net.kyori.adventure.text.Component line : ctx.messageService().messageList(asPlayer(sender), "help")) {
            sender.sendMessage(line);
        }
    }

    private void sendNpcNotFound(CommandSender sender, Player player, String id) {
        sender.sendMessage(ctx.messageService().message(player, "command.delete-missing", Placeholder.parsed("npc", id)));
        sender.sendMessage(ctx.messageService().message(player, "command.npc-not-found-hint"));
    }

    private static Player asPlayer(CommandSender sender) {
        return sender instanceof Player player ? player : null;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return filter(args[0], rootSubcommandTabOptions(sender).toArray(String[]::new));
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "delete", "tp", "info", "respawn", "skin" -> filter(args[1], npcIdTabOptions().toArray(String[]::new));
                case "migrate" -> filter(args[1], "yaml", "sqlite", "mysql");
                case "import" -> filter(args[1], "znpcsplus");
                case "pose" -> filter(args[1], "copy", "apply");
                case "create" -> filter(args[1], createTabOptions(args).toArray(String[]::new));
                default -> List.of();
            };
        }
        if (args.length == 3) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "migrate" -> filter(args[2], "yaml", "sqlite", "mysql");
                case "create" -> filter(args[2], createTabOptions(args).toArray(String[]::new));
                case "skin" -> filter(args[2], skinProfileTabOptions().toArray(String[]::new));
                default -> {
                    if ("pose".equalsIgnoreCase(args[0]) && "apply".equalsIgnoreCase(args[1])) {
                        yield filter(args[2], npcIdTabOptions().toArray(String[]::new));
                    }
                    yield List.of();
                }
            };
        }
        return List.of();
    }

    private List<String> rootSubcommandTabOptions(CommandSender sender) {
        PluginConfig config = ctx.pluginConfig();
        List<String> options = new ArrayList<>();
        if (SoulNpcPermissionChecks.hasAnyCommandAccess(sender, config)) {
            options.add("help");
        }
        if (SoulNpcPermissionChecks.hasAdmin(sender, config)) {
            options.addAll(List.of("reload", "list", "tp", "info", "respawn", "skin", "stick", "migrate", "import"));
        }
        if (SoulNpcPermissionChecks.hasCreate(sender, config)) {
            options.add("create");
        }
        if (SoulNpcPermissionChecks.hasDelete(sender, config)) {
            options.add("delete");
        }
        if (SoulNpcPermissionChecks.hasEditGui(sender, config)) {
            options.addAll(List.of("edit", "pose", "guicancel"));
        }
        return options;
    }

    private List<String> npcIdTabOptions() {
        List<String> ids = new ArrayList<>();
        for (NpcFileData data : ctx.repository().findAll()) {
            ids.add(data.id);
        }
        return ids;
    }

    private List<String> createTabOptions(String[] args) {
        List<String> options = new ArrayList<>(createTypeTabOptions());
        options.add("s-");
        options.add("-s");
        if (args.length >= 2) {
            String previous = args[args.length - 1];
            if ("s-".equalsIgnoreCase(previous) || "-s".equalsIgnoreCase(previous)) {
                return skinProfileTabOptions();
            }
        }
        return options;
    }

    private static List<String> createTypeTabOptions() {
        List<String> choices = new ArrayList<>();
        choices.add("player");
        choices.addAll(Arrays.asList(NpcCreateTypeParser.createTabChoices()));
        return choices;
    }

    private static List<String> skinProfileTabOptions() {
        List<String> choices = new ArrayList<>(List.of("url", "file"));
        for (Player online : Bukkit.getOnlinePlayers()) {
            choices.add(online.getName());
        }
        return choices;
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
