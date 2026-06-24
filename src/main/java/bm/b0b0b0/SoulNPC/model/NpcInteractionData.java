package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;

import java.util.ArrayList;
import java.util.List;

@Comment(value = {
        @CommentValue(" Клики по NPC (ПКМ/ЛКМ по модели или прицелом)."),
        @CommentValue(""),
        @CommentValue(" Каждая строка в списках ниже — одно действие; порядок сверху вниз."),
        @CommentValue(" Строки с # в начале — подсказки в yaml, плагин их не выполняет."),
        @CommentValue(" Чтобы включить действие — убери # и пробел после него (или добавь новую строку)."),
        @CommentValue(""),
        @CommentValue(" Префиксы (регистр не важен):"),
        @CommentValue("   [message] <текст>  — сообщение кликнувшему (MiniMessage, без /)"),
        @CommentValue("   [player] <команда> — выполнить от имени игрока (/ необязателен)"),
        @CommentValue("   [console] <команда> — выполнить от консоли (без /)"),
        @CommentValue("   [op] <команда>     — временный OP у игрока (без /)"),
        @CommentValue("   <команда>          — без префикса = то же, что [player]"),
        @CommentValue(""),
        @CommentValue(" Плейсхолдеры: {player} {uuid} {npc} {world}"),
})
public final class NpcInteractionData {

    @Comment(value = {
            @CommentValue(" false — клики отключены (игроку: interaction.disabled)"),
            @CommentValue(" true — смотри списки команд ниже (#-строки не считаются действием)")
    })
    public boolean enabled = true;

    @Comment(value = {
            @CommentValue(" Пауза между успешными кликами одним игроком по этому NPC (мс)"),
            @CommentValue(" 0 = взять default-interaction-cooldown-ms из config.yml плагина"),
            @CommentValue(" Обход: permission soulnpc.bypass.cooldown")
    })
    public long cooldownMs = 500L;

    @Comment(value = {
            @CommentValue(" Дополнительный permission для клика по этому NPC"),
            @CommentValue(" Пусто \"\" — достаточно soulnpc.use (из config.yml)"),
            @CommentValue(" Пример: \"shop.vip\" — нужны soulnpc.use И shop.vip"),
            @CommentValue(" При отказе: interaction.no-permission + deny-sound (если задан)")
    })
    public String permission = "";

    @Comment(value = {
            @CommentValue(" Звук при успешном клике (когда хотя бы одна команда выполнилась)"),
            @CommentValue(" Имя из org.bukkit.Sound, UPPER_SNAKE_CASE (регистр не важен)"),
            @CommentValue(" Примеры: ENTITY_EXPERIENCE_ORB_PICKUP, UI_BUTTON_CLICK, BLOCK_NOTE_BLOCK_PLING"),
            @CommentValue(" Пусто \"\" — без звука"),
            @CommentValue(" Список: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html")
    })
    public String clickSound = "";

    @Comment(value = {
            @CommentValue(" Звук при отказе: нет permission, кулдаун, interaction.enabled: false"),
            @CommentValue(" Формат как у click-sound; пусто \"\" — без звука"),
            @CommentValue(" Пример: ENTITY_VILLAGER_NO")
    })
    public String denySound = "";

    @NewLine
    @Comment(value = {
            @CommentValue(" === ПКМ (основной клик) ==="),
            @CommentValue(" Сейчас только #-подсказки — клик ничего не делает."),
            @CommentValue(" Раскомментируй строку (убери #) или добавь свою:"),
            @CommentValue("   - \"[message]<green>Открываю магазин</green>\""),
            @CommentValue("   - \"[player] shop open {npc}\""),
            @CommentValue("   - \"[console] say {player} кликнул NPC {npc}\""),
            @CommentValue("   - \"[op] gamemode creative\""),
            @CommentValue("   - \"spawn\"   # без префикса = [player]")
    })
    public List<String> rightClickCommands = commandSpecExamples();

    @NewLine
    @Comment(value = {
            @CommentValue(" === ЛКМ ==="),
            @CommentValue(" Тот же формат строк, что у right-click-commands."),
            @CommentValue(" Пример:"),
            @CommentValue("   - \"[message]<gray>Информация о NPC</gray>\""),
            @CommentValue("   - \"[player] npc info {npc}\""),
            @CommentValue(" Пустой список [] — клик ничего не делает")
    })
    public List<String> leftClickCommands = commandSpecHint();

    @NewLine
    @Comment(value = {
            @CommentValue(" === СКМ (средняя кнопка / pick block на entity) ==="),
            @CommentValue(" Формат строк — как у right-click-commands."),
            @CommentValue(" Пример:"),
            @CommentValue("   - \"[console] say {player} использовал СКМ на NPC {npc}\""),
            @CommentValue(" Пустой список [] — клик ничего не делает")
    })
    public List<String> middleClickCommands = commandSpecHint();

    @NewLine
    @Comment(value = {
            @CommentValue(" === Shift + ПКМ ==="),
            @CommentValue(" Формат строк — как у right-click-commands."),
            @CommentValue(" Пример:"),
            @CommentValue("   - \"[player] trade {npc}\""),
            @CommentValue(" Пустой список [] — клик ничего не делает")
    })
    public List<String> shiftRightClickCommands = commandSpecHint();

    @NewLine
    @Comment(value = {
            @CommentValue(" === Shift + ЛКМ ==="),
            @CommentValue(" Формат строк — как у right-click-commands."),
            @CommentValue(" Пример:"),
            @CommentValue("   - \"[message]<red>Только для админов</red>\""),
            @CommentValue("   - \"[op] npc edit {npc}\""),
            @CommentValue(" Пустой список [] — клик ничего не делает")
    })
    public List<String> shiftLeftClickCommands = commandSpecHint();

    @NewLine
    @Comment(value = {
            @CommentValue(" === Структурированные действия (приоритет над списками выше) ==="),
            @CommentValue(" click: RIGHT, LEFT, MIDDLE, SHIFT_*, ANY"),
            @CommentValue(" type: MESSAGE, PLAYER_CMD, CONSOLE_CMD, OP_CMD, SWITCH_SERVER"),
            @CommentValue(" value: текст/команда/сервер; delay-ticks; cooldown-seconds (0 = глобальный)")
    })
    public List<NpcInteractionAction> actions = new ArrayList<>();

    public void normalizeActions() {
        if (actions == null) {
            actions = new ArrayList<>();
            return;
        }
        actions.removeIf(action -> action == null || !action.isActionable());
    }

    public void ensureActionsMigrated() {
        normalizeActions();
        if (!actions.isEmpty()) {
            return;
        }
        actions = new ArrayList<>();
        migrateLegacyList(rightClickCommands, NpcClickBinding.RIGHT);
        migrateLegacyList(leftClickCommands, NpcClickBinding.LEFT);
        migrateLegacyList(middleClickCommands, NpcClickBinding.MIDDLE);
        migrateLegacyList(shiftRightClickCommands, NpcClickBinding.SHIFT_RIGHT);
        migrateLegacyList(shiftLeftClickCommands, NpcClickBinding.SHIFT_LEFT);
    }

    private void migrateLegacyList(List<String> commands, NpcClickBinding binding) {
        if (commands == null) {
            return;
        }
        for (String line : commands) {
            if (!hasActionableCommand(line)) {
                continue;
            }
            actions.add(NpcInteractionActionParser.fromLegacyLine(line, binding));
        }
    }

    public List<NpcInteractionAction> actionsFor(NpcClickType clickType) {
        ensureActionsMigrated();
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        List<NpcInteractionAction> specific = new ArrayList<>();
        List<NpcInteractionAction> any = new ArrayList<>();
        for (NpcInteractionAction action : actions) {
            if (action == null || !action.isActionable()) {
                continue;
            }
            if (action.click == NpcClickBinding.ANY) {
                any.add(action);
            } else if (action.click.matches(clickType)) {
                specific.add(action);
            }
        }
        specific.addAll(any);
        return specific;
    }

    public boolean hasActionableActions(NpcClickType clickType) {
        return !actionsFor(clickType).isEmpty();
    }

    public int actionableActionCount() {
        ensureActionsMigrated();
        if (actions == null) {
            return 0;
        }
        int count = 0;
        for (NpcInteractionAction action : actions) {
            if (action != null && action.isActionable()) {
                count++;
            }
        }
        return count;
    }

    private static List<String> commandSpecExamples() {
        List<String> lines = new ArrayList<>(7);
        lines.add("# --- примеры (убери # чтобы включить) ---");
        lines.add("# [message]<green>Текст игроку</green>");
        lines.add("# [player] warp shop");
        lines.add("# [console] eco give {player} 100");
        lines.add("# [op] gamemode creative");
        lines.add("# spawn");
        lines.add("# --- пока все строки с # — ПКМ ничего не делает ---");
        return lines;
    }

    private static List<String> commandSpecHint() {
        List<String> lines = new ArrayList<>(1);
        lines.add("# формат: [message]текст | [player]cmd | [console]cmd | [op]cmd | cmd (см. right-click-commands)");
        return lines;
    }

    public static boolean hasActionableCommand(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String trimmed = raw.trim();
        return !trimmed.startsWith("#");
    }

    public static boolean hasActionableCommands(List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return false;
        }
        for (String line : commands) {
            if (hasActionableCommand(line)) {
                return true;
            }
        }
        return false;
    }
}
