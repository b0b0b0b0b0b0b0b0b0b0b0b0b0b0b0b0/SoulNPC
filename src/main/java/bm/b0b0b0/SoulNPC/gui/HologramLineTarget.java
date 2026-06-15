package bm.b0b0b0.SoulNPC.gui;

public sealed interface HologramLineTarget permits HologramLineTarget.Name, HologramLineTarget.Description, HologramLineTarget.Extra, HologramLineTarget.AddExtra {

    record Name() implements HologramLineTarget {
    }

    record Description() implements HologramLineTarget {
    }

    record Extra(int index) implements HologramLineTarget {
    }

    record AddExtra() implements HologramLineTarget {
    }
}
