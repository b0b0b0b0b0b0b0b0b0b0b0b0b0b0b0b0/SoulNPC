package bm.b0b0b0.SoulNPC.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class SoulNpcKeys {

    public final NamespacedKey npcId;
    public final NamespacedKey entityRole;
    public final NamespacedKey groundItem;

    public SoulNpcKeys(Plugin plugin) {
        this.npcId = new NamespacedKey(plugin, "npc-id");
        this.entityRole = new NamespacedKey(plugin, "entity-role");
        this.groundItem = new NamespacedKey(plugin, "ground-item");
    }
}
