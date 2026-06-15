package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

import java.util.ArrayList;
import java.util.List;

public final class NpcInteractionData {

    @Comment(value = {
            @CommentValue(" Включить клики по NPC")
    })
    public boolean enabled = true;

    @Comment(value = {
            @CommentValue(" Кулдаун между кликами (мс)")
    })
    public long cooldownMs = 500L;

    @Comment(value = {
            @CommentValue(" Доп. permission (пусто = только soulnpc.use)")
    })
    public String permission = "";

    @Comment(value = {
            @CommentValue(" Звук при успешном клике (Bukkit Sound, пусто = нет)")
    })
    public String clickSound = "";

    @Comment(value = {
            @CommentValue(" Звук при отказе (Bukkit Sound, пусто = нет)")
    })
    public String denySound = "";

    @Comment(value = {
            @CommentValue(" ПКМ — префиксы: [message], [player], [console], [op]")
    })
    public List<String> rightClickCommands = defaultList("[message]<yellow>ПКМ</yellow>");

    @Comment(value = {
            @CommentValue(" ЛКМ")
    })
    public List<String> leftClickCommands = defaultList("[message]<gray>ЛКМ</gray>");

    @Comment(value = {
            @CommentValue(" Средняя кнопка мыши (колёсико / pick entity)")
    })
    public List<String> middleClickCommands = defaultList("[message]<gray>СКМ</gray>");

    @Comment(value = {
            @CommentValue(" Shift + ПКМ")
    })
    public List<String> shiftRightClickCommands = defaultList("[message]<gray>Shift + ПКМ</gray>");

    @Comment(value = {
            @CommentValue(" Shift + ЛКМ")
    })
    public List<String> shiftLeftClickCommands = defaultList("[message]<gray>Shift + ЛКМ</gray>");

    private static List<String> defaultList(String line) {
        List<String> lines = new ArrayList<>(1);
        lines.add(line);
        return lines;
    }
}
