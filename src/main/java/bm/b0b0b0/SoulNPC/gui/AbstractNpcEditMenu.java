package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcService;
import bm.b0b0b0.SoulNPC.util.SoulNpcKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

abstract class AbstractNpcEditMenu implements ClickableSoulNpcGui {

    protected final NpcEditGuiDependencies deps;
    protected final String npcId;
    protected final Inventory inventory;

    protected AbstractNpcEditMenu(NpcEditGuiDependencies deps, String npcId, int size, Component title) {
        this.deps = deps;
        this.npcId = npcId.toLowerCase(Locale.ROOT);
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        render(player);
        player.openInventory(inventory);
    }

    protected NpcFileData requireNpc(Player player) {
        NpcFileData data = deps.repository().findById(npcId).orElse(null);
        if (data == null) {
            player.closeInventory();
            player.sendMessage(deps.messageService().message(
                    player,
                    "command.delete-missing",
                    Placeholder.parsed("npc", npcId)
            ));
        }
        return data;
    }

    protected NpcFileData findNpc() {
        return deps.repository().findById(npcId).orElse(null);
    }

    protected void saveAndRefresh(NpcFileData data) {
        deps.repository().save(data);
        deps.npcService().saveAndRefresh(npcId);
    }

    protected void fillPane(int size, ItemStack filler) {
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, filler);
        }
    }

    protected PluginConfig pluginConfig() {
        return deps.pluginConfig();
    }

    protected MessageService messageService() {
        return deps.messageService();
    }

    protected NpcRepository repository() {
        return deps.repository();
    }

    protected NpcService npcService() {
        return deps.npcService();
    }

    protected AdminNpcMenuListener menus() {
        return deps.menus();
    }

    protected SoulNpcKeys soulNpcKeys() {
        return deps.soulNpcKeys();
    }

    protected ItemStackFactory itemStackFactory() {
        return deps.itemStackFactory();
    }

    protected abstract void render(Player player);
}
