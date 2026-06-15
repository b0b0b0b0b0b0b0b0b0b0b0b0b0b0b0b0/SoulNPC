package bm.b0b0b0.SoulNPC.listener;

import bm.b0b0b0.SoulNPC.gui.GuiChatInputService;
import io.papermc.paper.event.player.AbstractChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuiChatInputListener implements Listener {

    private final JavaPlugin plugin;
    private final GuiChatInputService chatInputService;

    public GuiChatInputListener(JavaPlugin plugin, GuiChatInputService chatInputService) {
        this.plugin = plugin;
        this.chatInputService = chatInputService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        handleChat(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatInputService.clear(event.getPlayer());
    }

    private void handleChat(AbstractChatEvent event) {
        Player player = event.getPlayer();
        if (!chatInputService.hasSession(player)) {
            return;
        }
        String text = extractChatText(event);
        event.setCancelled(true);
        event.viewers().clear();
        if (event.isAsynchronous()) {
            Bukkit.getScheduler().runTask(plugin, () -> chatInputService.submit(player, text));
        } else {
            chatInputService.submit(player, text);
        }
    }

    private static String extractChatText(AbstractChatEvent event) {
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        SignedMessage signed = event.signedMessage();
        if (signed != null) {
            String raw = signed.message();
            if (raw != null && !raw.isEmpty()) {
                return raw;
            }
            Component unsigned = signed.unsignedContent();
            if (unsigned != null) {
                return plain.serialize(unsigned);
            }
        }
        Component original = event.originalMessage();
        if (original != null) {
            String fromOriginal = plain.serialize(original);
            if (!fromOriginal.isEmpty()) {
                return fromOriginal;
            }
        }
        return plain.serialize(event.message());
    }
}
