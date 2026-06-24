package bm.b0b0b0.SoulNPC.gui;

public sealed interface HologramLineTarget permits HologramLineTarget.Line {

    record Line(int index) implements HologramLineTarget {
    }
}
