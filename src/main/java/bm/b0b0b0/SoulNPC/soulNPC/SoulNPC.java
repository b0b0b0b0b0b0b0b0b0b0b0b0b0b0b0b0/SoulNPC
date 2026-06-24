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
import bm.b0b0b0.SoulNPC.storage.NpcRepositoryFactory;
import bm.b0b0b0.SoulNPC.storage.NpcStorageBackend;
import bm.b0b0b0.SoulNPC.service.NpcAnimationService;
import bm.b0b0b0.SoulNPC.service.NpcDefaultsFactory;
import bm.b0b0b0.SoulNPC.service.NpcInteractionService;
import bm.b0b0b0.SoulNPC.service.PlaceholderService;
import bm.b0b0b0.SoulNPC.service.ProxyTransferService;
import bm.b0b0b0.SoulNPC.service.NpcService;
import bm.b0b0b0.SoulNPC.service.NpcSpawnService;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
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
        if (!isPacketEventsPresent()) {
            getLogger().severe("packetevents is required! Download: https://github.com/retrooper/packetevents");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getDataFolder().mkdirs();

        pluginConfig = PluginConfig.load(this);
        messageService = new MessageService(this, pluginConfig);
        SkinService skinService = new SkinService(this);
        ItemStackFactory itemStackFactory = new ItemStackFactory(this, pluginConfig);
        if (skinService.initSkinRestorer()) {
            getLogger().info("SkinRestorer подключён — скины NPC из базы SR.");
        }
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
        proxyTransferService.init();
        PlaceholderService.init(this);

        databaseLifecycle = new DatabaseLifecycle();
        NpcStorageBackend storageBackend = NpcRepositoryFactory.createActiveBackend(this, pluginConfig, databaseLifecycle);
        npcRepository = new CachedNpcRepository(this, storageBackend);
        migrationService = new NpcMigrationService(this, pluginConfig, databaseLifecycle);

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

        ((CachedNpcRepository) npcRepository).initialLoad(() -> {
            spawnService.bootstrapLoadedNpcs();
            spawnService.start();
            getLogger().info("SoulNPC ready — storage: " + storageBackend.type().configKey() + ".");
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
    }

    @Override
    public void onDisable() {
        SoulNpcApi.unregister();
        if (spawnService != null) {
            spawnService.stop();
        }
        if (databaseLifecycle != null) {
            databaseLifecycle.closeAll();
        }
    }

    private static boolean isPacketEventsPresent() {
        return Bukkit.getPluginManager().getPlugin("packetevents") != null
                || Bukkit.getPluginManager().getPlugin("PacketEvents") != null;
    }
}
