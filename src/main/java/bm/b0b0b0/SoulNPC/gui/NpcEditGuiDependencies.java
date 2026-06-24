package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcService;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import org.bukkit.plugin.java.JavaPlugin;

public record NpcEditGuiDependencies(
        JavaPlugin plugin,
        PluginConfig pluginConfig,
        MessageService messageService,
        NpcRepository repository,
        NpcService npcService,
        AdminNpcMenuListener menus,
        ItemStackFactory itemStackFactory,
        SoulNpcKeys soulNpcKeys
) {
}
