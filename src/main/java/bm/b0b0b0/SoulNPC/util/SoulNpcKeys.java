package bm.b0b0b0.SoulNPC.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class SoulNpcKeys {

    public final NamespacedKey npcId;
    public final NamespacedKey entityRole;
    public final NamespacedKey groundItem;
    public final NamespacedKey inspectorStick;
    public final NamespacedKey dressHint;

    public SoulNpcKeys(Plugin plugin) {
        this.npcId = new NamespacedKey(plugin, "npc-id");
        this.entityRole = new NamespacedKey(plugin, "entity-role");
        this.groundItem = new NamespacedKey(plugin, "ground-item");
        this.inspectorStick = new NamespacedKey(plugin, "inspector-stick");
        this.dressHint = new NamespacedKey(plugin, "dress-hint");
    }
}
