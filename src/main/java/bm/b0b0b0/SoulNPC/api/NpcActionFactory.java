package bm.b0b0b0.SoulNPC.api;

import bm.b0b0b0.SoulNPC.model.NpcActionType;
import bm.b0b0b0.SoulNPC.model.NpcClickBinding;
import bm.b0b0b0.SoulNPC.model.NpcInteractionAction;

public final class NpcActionFactory {

    private NpcActionFactory() {
    }

    public static NpcInteractionAction message(NpcClickBinding click, String text) {
        return action(click, NpcActionType.MESSAGE, text);
    }

    public static NpcInteractionAction playerCommand(NpcClickBinding click, String command) {
        return action(click, NpcActionType.PLAYER_CMD, command);
    }

    public static NpcInteractionAction consoleCommand(NpcClickBinding click, String command) {
        return action(click, NpcActionType.CONSOLE_CMD, command);
    }

    public static NpcInteractionAction opCommand(NpcClickBinding click, String command) {
        return action(click, NpcActionType.OP_CMD, command);
    }

    public static NpcInteractionAction switchServer(NpcClickBinding click, String serverName) {
        return action(click, NpcActionType.SWITCH_SERVER, serverName);
    }

    public static NpcInteractionAction action(NpcClickBinding click, NpcActionType type, String value) {
        NpcInteractionAction action = new NpcInteractionAction();
        action.click = click;
        action.type = type;
        action.value = value == null ? "" : value;
        return action;
    }

    public static NpcInteractionAction withDelay(NpcInteractionAction action, int delayTicks) {
        action.delayTicks = Math.max(0, delayTicks);
        return action;
    }

    public static NpcInteractionAction withCooldown(NpcInteractionAction action, int cooldownSeconds) {
        action.cooldownSeconds = Math.max(0, cooldownSeconds);
        return action;
    }
}
