package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class NpcInteractionAction {

    @Comment(value = {
            @CommentValue(" LEFT, RIGHT, MIDDLE, SHIFT_LEFT, SHIFT_RIGHT, ANY")
    })
    public NpcClickBinding click = NpcClickBinding.RIGHT;

    @Comment(value = {
            @CommentValue(" MESSAGE, PLAYER_CMD, CONSOLE_CMD, OP_CMD, SWITCH_SERVER")
    })
    public NpcActionType type = NpcActionType.MESSAGE;

    @Comment(value = {
            @CommentValue(" Текст сообщения или команда / имя сервера для SWITCH_SERVER")
    })
    public String value = "";

    @Comment(value = {
            @CommentValue(" Задержка перед выполнением (тики)")
    })
    public int delayTicks;

    @Comment(value = {
            @CommentValue(" Кулдаун действия (сек); 0 = глобальный cooldown NPC")
    })
    public int cooldownSeconds;

    public NpcInteractionAction copy() {
        NpcInteractionAction copy = new NpcInteractionAction();
        copy.click = click;
        copy.type = type;
        copy.value = value;
        copy.delayTicks = delayTicks;
        copy.cooldownSeconds = cooldownSeconds;
        return copy;
    }
}
