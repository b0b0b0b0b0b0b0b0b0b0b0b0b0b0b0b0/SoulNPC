package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.service.NpcService;
import bm.b0b0b0.SoulNPC.util.SoulNpcPermissionChecks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiChatInputService {

    private record Deps(
            PluginConfig pluginConfig,
            MessageService messageService,
            NpcRepository repository,
            NpcService npcService,
            AdminNpcMenuListener menuListener
    ) {
    }

    private final Deps deps;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public GuiChatInputService(
            PluginConfig pluginConfig,
            MessageService messageService,
            NpcRepository repository,
            NpcService npcService,
            AdminNpcMenuListener menuListener
    ) {
        this.deps = new Deps(pluginConfig, messageService, repository, npcService, menuListener);
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void beginActionEdit(Player player, String npcId, int actionIndex) {
        if (!SoulNpcPermissionChecks.requireEditGui(player, deps.pluginConfig(), deps.messageService())) {
            return;
        }
        sessions.put(player.getUniqueId(), new Session(npcId.toLowerCase(), null, true, actionIndex));
        player.closeInventory();
        Component prompt = deps.messageService().message(
                player,
                "gui.actions.chat-prompt",
                Placeholder.parsed("index", String.valueOf(actionIndex + 1))
        );
        player.sendMessage(prompt.append(Component.space()).append(deps.messageService().chatCancelButton(player)));
    }

    public void beginVisibilityPermission(Player player, String npcId) {
        if (!SoulNpcPermissionChecks.requireEditGui(player, deps.pluginConfig(), deps.messageService())) {
            return;
        }
        sessions.put(player.getUniqueId(), new Session(npcId.toLowerCase(), null, false, -2));
        player.closeInventory();
        Component prompt = deps.messageService().message(player, "gui.edit.visibility-permission-chat-prompt");
        player.sendMessage(prompt.append(Component.space()).append(deps.messageService().chatCancelButton(player)));
    }

    public void begin(Player player, String npcId, HologramLineTarget target) {
        begin(player, npcId, target, false);
    }

    public void begin(Player player, String npcId, HologramLineTarget target, boolean returnToLinesMenu) {
        if (!SoulNpcPermissionChecks.requireEditGui(player, deps.pluginConfig(), deps.messageService())) {
            return;
        }
        sessions.put(player.getUniqueId(), new Session(npcId.toLowerCase(), target, returnToLinesMenu, -1));
        player.closeInventory();
        Component prompt = deps.messageService().message(
                player,
                "gui.edit.chat-prompt",
                Placeholder.parsed("line", lineLabel(player, target))
        );
        player.sendMessage(prompt.append(Component.space()).append(deps.messageService().chatCancelButton(player)));
    }

    public void cancel(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (!SoulNpcPermissionChecks.requireEditGui(player, deps.pluginConfig(), deps.messageService())) {
            return;
        }
        player.sendMessage(deps.messageService().message(player, "gui.edit.chat-cancelled"));
        reopen(session, player);
    }

    public void submit(Player player, String text) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (!SoulNpcPermissionChecks.requireEditGui(player, deps.pluginConfig(), deps.messageService())) {
            return;
        }
        NpcFileData data = deps.repository().findById(session.npcId()).orElse(null);
        if (data == null) {
            player.sendMessage(deps.messageService().message(player, "command.delete-missing", Placeholder.parsed("npc", session.npcId())));
            return;
        }
        String trimmed = text == null ? "" : text.trim();
        if (session.actionIndex() >= 0) {
            data.interaction.ensureActionsMigrated();
            if (session.actionIndex() < data.interaction.actions.size()) {
                data.interaction.actions.get(session.actionIndex()).value = trimmed;
            }
        } else if (session.actionIndex() == -2) {
            if ("none".equalsIgnoreCase(trimmed) || "-".equals(trimmed)) {
                data.visibilityPermission = "";
            } else {
                data.visibilityPermission = trimmed;
            }
        } else {
            apply(data.appearance, session.target(), trimmed);
            data.appearance.normalizePresentation();
        }
        deps.repository().save(data);
        deps.npcService().saveAndRefresh(session.npcId());
        player.sendMessage(deps.messageService().message(player, "gui.edit.chat-saved"));
        reopen(session, player);
    }

    public void clear(Player player) {
        sessions.remove(player.getUniqueId());
    }

    private void reopen(Session session, Player player) {
        if (session.actionIndex() >= 0) {
            deps.menuListener().openActions(player, session.npcId());
            return;
        }
        if (session.returnToLinesMenu()) {
            deps.menuListener().openLines(player, session.npcId());
        } else {
            deps.menuListener().openEdit(player, session.npcId());
        }
    }

    private void apply(NpcAppearanceData appearance, HologramLineTarget target, String text) {
        if (target instanceof HologramLineTarget.Line(int index)) {
            NpcHologramLines.setLineText(appearance, index, text);
            if (NpcHologramLines.hasText(text)) {
                NpcHologramLines.setLineHidden(appearance, index, false);
            }
        }
    }

    private String lineLabel(Player player, HologramLineTarget target) {
        if (target instanceof HologramLineTarget.Line(int index)) {
            if (index == NpcHologramLines.NAME_LINE_INDEX) {
                return deps.messageService().plain(player, "gui.lines.line-name-label");
            }
            return deps.messageService().plain(player, "gui.lines.line-label")
                    .replace("{line}", String.valueOf(index + 1));
        }
        return "?";
    }

    private record Session(String npcId, HologramLineTarget target, boolean returnToLinesMenu, int actionIndex) {
    }
}
