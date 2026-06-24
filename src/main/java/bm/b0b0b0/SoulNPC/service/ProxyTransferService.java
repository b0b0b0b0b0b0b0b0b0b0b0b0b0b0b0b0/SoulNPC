package bm.b0b0b0.SoulNPC.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class ProxyTransferService {

    private final JavaPlugin plugin;
    private boolean bungeeChannelRegistered;

    public ProxyTransferService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        if (Bukkit.getPluginManager().getPlugin("BungeeCord") != null
                || Bukkit.getPluginManager().getPlugin("bungeecord") != null) {
            Messenger messenger = plugin.getServer().getMessenger();
            if (!messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
                messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
            }
            bungeeChannelRegistered = true;
        }
        return bungeeChannelRegistered;
    }

    public boolean transfer(Player player, String serverName) {
        if (serverName == null || serverName.isBlank()) {
            return false;
        }
        String target = serverName.trim();
        if (bungeeChannelRegistered) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeUTF("Connect");
                out.writeUTF(target);
                player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
                return true;
            } catch (IOException exception) {
                plugin.getLogger().warning("BungeeCord transfer failed: " + exception.getMessage());
            }
        }
        return Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "send " + player.getName() + " " + target
        );
    }
}
