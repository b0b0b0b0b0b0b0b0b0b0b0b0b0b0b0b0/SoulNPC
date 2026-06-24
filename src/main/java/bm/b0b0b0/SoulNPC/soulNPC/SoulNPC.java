package bm.b0b0b0.SoulNPC.soulNPC;

import bm.b0b0b0.SoulNPC.api.NpcRegistryImpl;
import bm.b0b0b0.SoulNPC.api.SoulNpcApi;
import bm.b0b0b0.SoulNPC.command.SoulNpcCommand;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.appearance.SkinService;
import bm.b0b0b0.SoulNPC.effect.NpcGroundItemEffectService;
import bm.b0b0b0.SoulNPC.gui.AdminNpcMenuListener;
import bm.b0b0b0.SoulNPC.gui.GuiChatInputService;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.hologram.NpcTextLabels;
import bm.b0b0b0.SoulNPC.listener.GuiChatInputListener;
import bm.b0b0b0.SoulNPC.listener.NpcInspectorStickGuardListener;
import bm.b0b0b0.SoulNPC.listener.NpcInspectorListener;
import bm.b0b0b0.SoulNPC.listener.NpcAimInteractListener;
import bm.b0b0b0.SoulNPC.listener.NpcGroundItemListener;
import bm.b0b0b0.SoulNPC.listener.NpcHologramListener;
import bm.b0b0b0.SoulNPC.listener.PacketNpcInteractListener;
import bm.b0b0b0.SoulNPC.listener.PlayerNpcLifecycleListener;
import bm.b0b0b0.SoulNPC.packet.PacketMobPoseService;
import bm.b0b0b0.SoulNPC.packet.PacketNpcAnimator;
import bm.b0b0b0.SoulNPC.packet.PacketNpcGreetService;
import bm.b0b0b0.SoulNPC.packet.PacketNpcLookAtService;
import bm.b0b0b0.SoulNPC.packet.PacketNpcViewerService;
import bm.b0b0b0.SoulNPC.repository.CachedNpcRepository;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.storage.DatabaseLifecycle;
import bm.b0b0b0.SoulNPC.storage.NpcMigrationService;
import bm.b0b0b0.SoulNPC.storage.NpcPayloadCodec;
import bm.b0b0b0.SoulNPC.storage.NpcRepositoryFactory;
import bm.b0b0b0.SoulNPC.storage.NpcStorageBackend;
import bm.b0b0b0.SoulNPC.service.NpcAnimationService;
import bm.b0b0b0.SoulNPC.service.NpcDefaultsFactory;
import bm.b0b0b0.SoulNPC.service.NpcInteractionService;
import bm.b0b0b0.SoulNPC.service.PlaceholderService;
import bm.b0b0b0.SoulNPC.service.ProxyTransferService;
import bm.b0b0b0.SoulNPC.service.NpcService;
import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import bm.b0b0b0.SoulNPC.util.SoulNpcConsole;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import bm.b0b0b0.SoulNPC.util.upd.SoulNpcUpdateChecker;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulNPC extends JavaPlugin {

    private PluginConfig pluginConfig;
    private MessageService messageService;
    private NpcRepository npcRepository;
    private NpcSpawnService spawnService;
    private NpcService npcService;
    private DatabaseLifecycle databaseLifecycle;
    private NpcMigrationService migrationService;

    @Override
    public void onEnable() {
        String version = getPluginMeta().getVersion();
        SoulNpcConsole.banner(version, "b0b0b0");

        if (!isPacketEventsPresent()) {
            SoulNpcConsole.errorBlock(
                    "PacketEvents is required but was not found!",
                    "Download: https://github.com/retrooper/packetevents"
            );
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        SoulNpcConsole.integration("PacketEvents", true, "connected", "missing");

        getDataFolder().mkdirs();
        SoulNpcConsole.info("Loading configuration (config.yml, gui/, lang/)…");

        pluginConfig = PluginConfig.load(this);
        messageService = new MessageService(this, pluginConfig);
        SoulNpcConsole.info("Locale default: \u001B[90m" + pluginConfig.settings().general.defaultLocale + "\u001B[0m");

        SkinService skinService = new SkinService(this);
        ItemStackFactory itemStackFactory = new ItemStackFactory(this, pluginConfig);
        SoulNpcConsole.integration(
                "SkinsRestorer",
                skinService.initSkinRestorer(),
                "skin database available",
                "not found — Mojang nick skins only"
        );
        SoulNpcConsole.integration(
                "PlaceholderAPI",
                PlaceholderService.init(),
                "placeholders in actions and holograms",
                "not found — built-in placeholders only"
        );

        SoulNpcKeys soulNpcKeys = new SoulNpcKeys(this);
        NpcTextLabels textLabels = new NpcTextLabels(this, pluginConfig, itemStackFactory, soulNpcKeys);
        PacketNpcViewerService viewerService = new PacketNpcViewerService(
                this,
                pluginConfig,
                skinService,
                itemStackFactory,
                textLabels
        );
        PacketNpcAnimator packetNpcAnimator = new PacketNpcAnimator();
        PacketNpcLookAtService lookAtService = new PacketNpcLookAtService();
        PacketNpcGreetService greetService = new PacketNpcGreetService(lookAtService);
        PacketMobPoseService mobPoseService = new PacketMobPoseService();
        NpcAnimationService animationService = new NpcAnimationService(
                this,
                pluginConfig,
                packetNpcAnimator,
                lookAtService,
                greetService,
                mobPoseService
        );
        NpcGroundItemEffectService groundItemEffectService = new NpcGroundItemEffectService(this, soulNpcKeys);

        ProxyTransferService proxyTransferService = new ProxyTransferService(this);
        SoulNpcConsole.integration(
                "BungeeCord",
                proxyTransferService.init(),
                "switchserver channel registered",
                "not found — console send fallback for SWITCH_SERVER"
        );
        logOptionalIntegration("ViaVersion", "ViaVersion", "ViaBackwards", "ViaRewind");
        SoulNpcConsole.integration(
                "Geyser",
                isPluginPresent("Geyser-Spigot"),
                "Bedrock clients supported",
                "not found"
        );

        databaseLifecycle = new DatabaseLifecycle();
        NpcPayloadCodec.setLogger(getLogger());
        NpcStorageBackend storageBackend = NpcRepositoryFactory.createActiveBackend(this, pluginConfig, databaseLifecycle);
        npcRepository = new CachedNpcRepository(this, storageBackend);
        migrationService = new NpcMigrationService(this, pluginConfig, databaseLifecycle);
        SoulNpcConsole.info("Storage backend: \u001B[90m" + storageBackend.type().configKey() + "\u001B[0m");

        spawnService = new NpcSpawnService(
                this,
                pluginConfig,
                npcRepository,
                viewerService,
                textLabels,
                animationService,
                lookAtService,
                groundItemEffectService
        );
        textLabels.bind(spawnService);
        NpcInteractionService interactionService = new NpcInteractionService(
                this,
                pluginConfig,
                messageService,
                proxyTransferService
        );
        npcService = new NpcService(this, npcRepository, spawnService, new NpcDefaultsFactory(pluginConfig));
        SoulNpcApi.register(new SoulNpcApi(new NpcRegistryImpl(npcRepository, npcService)));
        SoulNpcConsole.info("Public API registered (load: STARTUP)");

        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketNpcInteractListener(spawnService, interactionService)
        );

        AdminNpcMenuListener adminMenuListener = new AdminNpcMenuListener(
                this,
                pluginConfig,
                messageService,
                npcRepository,
                npcService,
                itemStackFactory,
                soulNpcKeys
        );
        GuiChatInputService chatInputService = new GuiChatInputService(
                pluginConfig,
                messageService,
                npcRepository,
                npcService,
                adminMenuListener
        );
        adminMenuListener.setChatInputService(chatInputService);
        SoulNpcCommand command = new SoulNpcCommand(
                this,
                pluginConfig,
                messageService,
                npcRepository,
                npcService,
                skinService,
                adminMenuListener,
                chatInputService,
                soulNpcKeys,
                migrationService
        );

        SoulNpcConsole.info("Loading NPC data asynchronously…");
        ((CachedNpcRepository) npcRepository).initialLoad(() -> {
            int total = npcRepository.findAll().size();
            int enabled = (int) npcRepository.findAll().stream().filter(data -> data.enabled).count();
            spawnService.bootstrapLoadedNpcs();
            spawnService.start();
            SoulNpcConsole.success("NPC cache ready: \u001B[32m" + total + "\u001B[0m total, "
                    + "\u001B[32m" + enabled + "\u001B[0m enabled (storage: "
                    + storageBackend.type().configKey() + ")");
            SoulNpcConsole.success("SoulNPC successfully enabled");
            SoulNpcConsole.sectionEnd();
        });

        getServer().getPluginManager().registerEvents(new PlayerNpcLifecycleListener(spawnService), this);
        getServer().getPluginManager().registerEvents(new NpcHologramListener(spawnService, textLabels), this);
        getServer().getPluginManager().registerEvents(new NpcGroundItemListener(groundItemEffectService), this);
        getServer().getPluginManager().registerEvents(adminMenuListener, this);
        getServer().getPluginManager().registerEvents(new GuiChatInputListener(chatInputService), this);
        getServer().getPluginManager().registerEvents(
                new NpcInspectorStickGuardListener(this, soulNpcKeys),
                this
        );
        getServer().getPluginManager().registerEvents(
                new NpcInspectorListener(spawnService, pluginConfig, messageService, soulNpcKeys),
                this
        );
        getServer().getPluginManager().registerEvents(
                new NpcAimInteractListener(spawnService, interactionService, textLabels, soulNpcKeys),
                this
        );

        var pluginCommand = getCommand("soulnpc");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
        SoulNpcConsole.info("Command /soulnpc registered (aliases: snpc, npc)");

        SoulNpcUpdateChecker.schedule(this, version);
    }

    @Override
    public void onDisable() {
        SoulNpcConsole.blank();
        SoulNpcConsole.line("Disabling SoulNPC…");
        SoulNpcApi.unregister();
        if (spawnService != null) {
            spawnService.stop();
        }
        if (databaseLifecycle != null) {
            databaseLifecycle.closeAll();
        }
        SoulNpcConsole.success("SoulNPC disabled");
        SoulNpcConsole.blank();
    }

    private static boolean isPacketEventsPresent() {
        return isPluginPresent("packetevents") || isPluginPresent("PacketEvents");
    }

    private static boolean isPluginPresent(String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null;
    }

    private static void logOptionalIntegration(String label, String... pluginNames) {
        for (String pluginName : pluginNames) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (plugin != null && plugin.isEnabled()) {
                SoulNpcConsole.integration(label, true, pluginName + " detected", "not found");
                return;
            }
        }
        SoulNpcConsole.integration(label, false, "detected", "not found");
    }
}
