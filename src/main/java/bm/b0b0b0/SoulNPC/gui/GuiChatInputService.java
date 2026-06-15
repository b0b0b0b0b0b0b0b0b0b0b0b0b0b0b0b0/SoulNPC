package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiChatInputService {

    private final MessageService messageService;
    private final NpcRepository repository;
    private final NpcService npcService;
    private final AdminNpcMenuListener menuListener;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public GuiChatInputService(
            MessageService messageService,
            NpcRepository repository,
            NpcService npcService,
            AdminNpcMenuListener menuListener
    ) {
        this.messageService = messageService;
        this.repository = repository;
        this.npcService = npcService;
        this.menuListener = menuListener;
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void begin(Player player, String npcId, HologramLineTarget target) {
        sessions.put(player.getUniqueId(), new Session(npcId.toLowerCase(), target));
        player.closeInventory();
        Component prompt = messageService.message(
                player,
                "gui.edit.chat-prompt",
                Placeholder.parsed("line", lineLabel(player, target))
        );
        player.sendMessage(prompt.append(Component.space()).append(messageService.chatCancelButton(player)));
    }

    public boolean cancel(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return false;
        }
        player.sendMessage(messageService.message(player, "gui.edit.chat-cancelled"));
        menuListener.openEdit(player, session.npcId());
        return true;
    }

    public boolean submit(Player player, String text) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return false;
        }
        NpcFileData data = repository.findById(session.npcId()).orElse(null);
        if (data == null) {
            player.sendMessage(messageService.message(player, "command.delete-missing", Placeholder.parsed("npc", session.npcId())));
            return true;
        }
        apply(data.appearance, session.target(), text == null ? "" : text.trim());
        repository.save(data);
        npcService.saveAndRefresh(session.npcId());
        player.sendMessage(messageService.message(player, "gui.edit.chat-saved"));
        menuListener.openEdit(player, session.npcId());
        return true;
    }

    public void clear(Player player) {
        sessions.remove(player.getUniqueId());
    }

    private void apply(NpcAppearanceData appearance, HologramLineTarget target, String text) {
        switch (target) {
            case HologramLineTarget.Name ignored -> appearance.name = text;
            case HologramLineTarget.Description ignored -> appearance.description = text;
            case HologramLineTarget.Extra(int index) -> {
                while (appearance.extraLines.size() <= index) {
                    appearance.extraLines.add("");
                }
                appearance.extraLines.set(index, text);
            }
            case HologramLineTarget.AddExtra ignored -> appearance.extraLines.add(text);
        }
    }

    private String lineLabel(Player player, HologramLineTarget target) {
        return switch (target) {
            case HologramLineTarget.Name ignored -> messageService.plain(player, "gui.edit.line-name-label");
            case HologramLineTarget.Description ignored -> messageService.plain(player, "gui.edit.line-description-label");
            case HologramLineTarget.Extra(int index) -> messageService.plain(player, "gui.edit.line-extra-label")
                    .replace("{index}", String.valueOf(index + 3));
            case HologramLineTarget.AddExtra ignored -> messageService.plain(player, "gui.edit.line-add-label");
        };
    }

    private record Session(String npcId, HologramLineTarget target) {
    }
}
