package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;

final class PacketNpcDebug {

    private PacketNpcDebug() {
    }

    static void log(JavaPlugin plugin, PluginConfig pluginConfig, String message) {
        if (pluginConfig == null || !pluginConfig.settings().general.debugPackets) {
            return;
        }
        plugin.getLogger().info("[SoulNPC][packets] " + message);
    }
}
