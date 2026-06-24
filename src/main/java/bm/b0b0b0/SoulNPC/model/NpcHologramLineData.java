package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

@Comment(value = {
        @CommentValue(" Строка голограммы (MiniMessage) или предмет (ITEM)"),
        @CommentValue(" hidden: true — не показывается, текст сохраняется, место в голограмме остаётся")
})
public final class NpcHologramLineData {

    @Comment(value = {
            @CommentValue(" TEXT или ITEM")
    })
    public NpcHologramLineType lineType = NpcHologramLineType.TEXT;

    @Comment(value = {
            @CommentValue(" Текст строки (MiniMessage)")
    })
    public String text = "";

    @Comment(value = {
            @CommentValue(" Скрыта в голограмме (не удалять)")
    })
    public boolean hidden = false;

    @Comment(value = {
            @CommentValue(" ITEM: Bukkit-материал")
    })
    public Material material = Material.STONE;

    @Comment(value = {
            @CommentValue(" ITEM: Custom Model Data")
    })
    public int customModelData = 0;

    @Comment(value = {
            @CommentValue(" ITEM: ItemsAdder ID")
    })
    public String itemsAdderId = "";

    @Comment(value = {
            @CommentValue(" ITEM: Nexo ID")
    })
    public String nexoId = "";

    @Comment(value = {
            @CommentValue(" ITEM: масштаб ItemDisplay")
    })
    public float scale = 1.0F;

    public NpcHologramLineData() {
    }

    public NpcHologramLineData(String text) {
        this.text = text == null ? "" : text;
    }

    public static NpcHologramLineData of(String text) {
        return new NpcHologramLineData(text);
    }

    public boolean hasContent() {
        if (lineType == NpcHologramLineType.ITEM) {
            return material != null && !material.isAir();
        }
        return text != null && !text.isBlank();
    }

    public float resolvedScale() {
        return scale <= 0.0F ? 1.0F : scale;
    }
}
