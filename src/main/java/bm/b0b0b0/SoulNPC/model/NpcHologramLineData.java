package bm.b0b0b0.SoulNPC.model;

import org.bukkit.Material;

public final class NpcHologramLineData {

    public NpcHologramLineType lineType = NpcHologramLineType.TEXT;
    public String text = "";
    public boolean hidden = false;
    public Material material = Material.STONE;
    public int customModelData = 0;
    public String itemsAdderId = "";
    public String nexoId = "";
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
