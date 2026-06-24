package bm.b0b0b0.SoulNPC.model;

import java.util.Locale;

public final class NpcInteractionActionParser {

    private NpcInteractionActionParser() {
    }

    public static NpcInteractionAction fromLegacyLine(String raw, NpcClickBinding binding) {
        NpcInteractionAction action = new NpcInteractionAction();
        action.click = binding;
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.regionMatches(true, 0, "[message]", 0, 9)) {
            action.type = NpcActionType.MESSAGE;
            action.value = trimmed.substring(9).trim();
            return action;
        }
        if (trimmed.regionMatches(true, 0, "[switchserver]", 0, 14)) {
            action.type = NpcActionType.SWITCH_SERVER;
            action.value = trimmed.substring(14).trim();
            return action;
        }
        if (trimmed.regionMatches(true, 0, "[console]", 0, 9)) {
            action.type = NpcActionType.CONSOLE_CMD;
            action.value = trimmed.substring(9).trim();
            return action;
        }
        if (trimmed.regionMatches(true, 0, "[op]", 0, 4)) {
            action.type = NpcActionType.OP_CMD;
            action.value = trimmed.substring(4).trim();
            return action;
        }
        if (trimmed.regionMatches(true, 0, "[player]", 0, 8)) {
            action.type = NpcActionType.PLAYER_CMD;
            action.value = trimmed.substring(8).trim();
            return action;
        }
        action.type = NpcActionType.PLAYER_CMD;
        action.value = trimmed;
        return action;
    }

    public static String legacyPrefix(NpcActionType type) {
        return switch (type) {
            case MESSAGE -> "[message] ";
            case CONSOLE_CMD -> "[console] ";
            case OP_CMD -> "[op] ";
            case SWITCH_SERVER -> "[switchserver] ";
            case PLAYER_CMD -> "[player] ";
        };
    }

    public static String describe(NpcInteractionAction action) {
        return action.click.name().toLowerCase(Locale.ROOT) + " "
                + legacyPrefix(action.type).trim() + action.value;
    }
}
