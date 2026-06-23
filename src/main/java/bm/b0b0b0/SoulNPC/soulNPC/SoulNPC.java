package bm.b0b0b0.SoulNPC.soulNPC;

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
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.repository.YamlNpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcAnimationService;
import bm.b0b0b0.SoulNPC.service.NpcDefaultsFactory;
import bm.b0b0b0.SoulNPC.service.NpcInteractionService;
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
        NpcTextLabels textLabels = new NpcTextLabels(soulNpcKeys);
        PacketNpcViewerService viewerService = new PacketNpcViewerService(
                this,
                pluginConfig,
                skinService,
                itemStackFactory
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

        npcRepository = new YamlNpcRepository(this, pluginConfig);
        npcRepository.reload();

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
        NpcInteractionService interactionService = new NpcInteractionService(this, pluginConfig, messageService);
        npcService = new NpcService(npcRepository, spawnService, new NpcDefaultsFactory(pluginConfig));

        spawnService.reloadAll();
        spawnService.start();

        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketNpcInteractListener(this, spawnService, interactionService)
        );

        AdminNpcMenuListener adminMenuListener = new AdminNpcMenuListener(
                this,
                pluginConfig,
                messageService,
                npcRepository,
                npcService
        );
        GuiChatInputService chatInputService = new GuiChatInputService(
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
                soulNpcKeys
        );

        getServer().getPluginManager().registerEvents(new PlayerNpcLifecycleListener(spawnService), this);
        getServer().getPluginManager().registerEvents(new NpcHologramListener(spawnService, textLabels), this);
        getServer().getPluginManager().registerEvents(new NpcGroundItemListener(groundItemEffectService), this);
        getServer().getPluginManager().registerEvents(adminMenuListener, this);
        getServer().getPluginManager().registerEvents(new GuiChatInputListener(this, chatInputService), this);
        getServer().getPluginManager().registerEvents(
                new NpcInspectorListener(spawnService, messageService, soulNpcKeys),
                this
        );
        getServer().getPluginManager().registerEvents(
                new NpcAimInteractListener(spawnService, interactionService, textLabels, soulNpcKeys),
                this
        );

        var pluginCommand = getCommand("soulnpc");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getLogger().info("SoulNPC enabled — packet PLAYER NPCs (PacketEvents).");
    }

    @Override
    public void onDisable() {
        if (spawnService != null) {
            spawnService.stop();
        }
    }

    private static boolean isPacketEventsPresent() {
        return Bukkit.getPluginManager().getPlugin("packetevents") != null
                || Bukkit.getPluginManager().getPlugin("PacketEvents") != null;
    }
}
