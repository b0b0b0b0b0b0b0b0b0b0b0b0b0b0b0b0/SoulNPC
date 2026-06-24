package bm.b0b0b0.SoulNPC.gui;

import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcHologramLineData;
import bm.b0b0b0.SoulNPC.model.NpcHologramLineType;
import org.bukkit.Material;

public final class NpcHologramLines {

    public static final int MAX_LINES = 14;
    public static final int NAME_LINE_INDEX = 0;

    private static final int[] LINE_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public enum LineState {
        EMPTY,
        HIDDEN,
        ACTIVE
    }

    private NpcHologramLines() {
    }

    public static int[] lineSlots() {
        return LINE_SLOTS.clone();
    }

    public static int slotToLineIndex(int slot) {
        for (int index = 0; index < LINE_SLOTS.length; index++) {
            if (LINE_SLOTS[index] == slot) {
                return index;
            }
        }
        return -1;
    }

    public static int lineIndexToSlot(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= LINE_SLOTS.length) {
            return -1;
        }
        return LINE_SLOTS[lineIndex];
    }

    public static LineState state(NpcAppearanceData appearance, int lineIndex) {
        if (!hasLineContent(appearance, lineIndex)) {
            return LineState.EMPTY;
        }
        return lineHidden(appearance, lineIndex) ? LineState.HIDDEN : LineState.ACTIVE;
    }

    public static Material flagMaterial(LineState state) {
        return switch (state) {
            case EMPTY -> Material.WHITE_BANNER;
            case HIDDEN -> Material.YELLOW_BANNER;
            case ACTIVE -> Material.LIME_BANNER;
        };
    }

    public static String lineText(NpcAppearanceData appearance, int lineIndex) {
        if (appearance == null || lineIndex < 0 || lineIndex >= MAX_LINES) {
            return "";
        }
        if (lineIndex == NAME_LINE_INDEX) {
            return appearance.name == null ? "" : appearance.name;
        }
        NpcHologramLineData line = extraLine(appearance, lineIndex);
        if (line == null) {
            return "";
        }
        if (line.lineType == NpcHologramLineType.ITEM) {
            return line.material == null ? "" : line.material.name();
        }
        return line.text == null ? "" : line.text;
    }

    public static NpcHologramLineType lineType(NpcAppearanceData appearance, int lineIndex) {
        if (lineIndex == NAME_LINE_INDEX) {
            return NpcHologramLineType.TEXT;
        }
        NpcHologramLineData line = extraLine(appearance, lineIndex);
        return line == null || line.lineType == null ? NpcHologramLineType.TEXT : line.lineType;
    }

    public static boolean lineHidden(NpcAppearanceData appearance, int lineIndex) {
        if (lineIndex == NAME_LINE_INDEX) {
            return appearance.nameHidden;
        }
        NpcHologramLineData line = extraLine(appearance, lineIndex);
        return line != null && line.hidden;
    }

    public static void setLineText(NpcAppearanceData appearance, int lineIndex, String text) {
        String value = text == null ? "" : text;
        if (lineIndex == NAME_LINE_INDEX) {
            appearance.name = value;
            return;
        }
        appearance.ensureExtraLine(lineIndex - 1);
        NpcHologramLineData line = appearance.extraLines.get(lineIndex - 1);
        line.lineType = NpcHologramLineType.TEXT;
        line.text = value;
    }

    public static void setLineHidden(NpcAppearanceData appearance, int lineIndex, boolean hidden) {
        if (lineIndex == NAME_LINE_INDEX) {
            appearance.nameHidden = hidden;
            return;
        }
        appearance.ensureExtraLine(lineIndex - 1);
        appearance.extraLines.get(lineIndex - 1).hidden = hidden;
    }

    public static void toggleLineHidden(NpcAppearanceData appearance, int lineIndex) {
        setLineHidden(appearance, lineIndex, !lineHidden(appearance, lineIndex));
    }

    public static void toggleLineType(NpcAppearanceData appearance, int lineIndex) {
        if (lineIndex == NAME_LINE_INDEX) {
            return;
        }
        appearance.ensureExtraLine(lineIndex - 1);
        NpcHologramLineData line = appearance.extraLines.get(lineIndex - 1);
        if (line.lineType == NpcHologramLineType.ITEM) {
            line.lineType = NpcHologramLineType.TEXT;
            line.text = line.material == null ? "" : line.material.name();
        } else {
            line.lineType = NpcHologramLineType.ITEM;
            line.material = Material.STONE;
            line.text = "";
        }
    }

    public static void clearLine(NpcAppearanceData appearance, int lineIndex) {
        if (lineIndex == NAME_LINE_INDEX) {
            setLineText(appearance, lineIndex, "");
            setLineHidden(appearance, lineIndex, false);
            return;
        }
        appearance.ensureExtraLine(lineIndex - 1);
        NpcHologramLineData line = appearance.extraLines.get(lineIndex - 1);
        line.text = "";
        line.lineType = NpcHologramLineType.TEXT;
        line.material = Material.STONE;
        line.customModelData = 0;
        line.itemsAdderId = "";
        line.nexoId = "";
        line.hidden = false;
    }

    public static int countActiveLines(NpcAppearanceData appearance) {
        int count = 0;
        for (int index = 0; index < MAX_LINES; index++) {
            if (state(appearance, index) == LineState.ACTIVE) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean hasLineContent(NpcAppearanceData appearance, int lineIndex) {
        if (lineIndex == NAME_LINE_INDEX) {
            return hasText(appearance.name);
        }
        NpcHologramLineData line = extraLine(appearance, lineIndex);
        return line != null && line.hasContent();
    }

    private static NpcHologramLineData extraLine(NpcAppearanceData appearance, int lineIndex) {
        int extraIndex = lineIndex - 1;
        if (appearance.extraLines == null || extraIndex < 0 || extraIndex >= appearance.extraLines.size()) {
            return null;
        }
        return appearance.extraLines.get(extraIndex);
    }
}
