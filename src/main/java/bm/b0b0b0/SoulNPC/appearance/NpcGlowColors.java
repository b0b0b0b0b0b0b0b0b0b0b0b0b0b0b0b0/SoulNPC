package bm.b0b0b0.SoulNPC.appearance;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class NpcGlowColors {

    public record Option(String id, Material icon, NamedTextColor color) {
    }

    private static final List<Option> OPTIONS = List.of(
            new Option("white", Material.WHITE_DYE, NamedTextColor.WHITE),
            new Option("yellow", Material.YELLOW_DYE, NamedTextColor.YELLOW),
            new Option("gold", Material.ORANGE_DYE, NamedTextColor.GOLD),
            new Option("green", Material.LIME_DYE, NamedTextColor.GREEN),
            new Option("dark_green", Material.GREEN_DYE, NamedTextColor.DARK_GREEN),
            new Option("aqua", Material.LIGHT_BLUE_DYE, NamedTextColor.AQUA),
            new Option("blue", Material.BLUE_DYE, NamedTextColor.BLUE),
            new Option("dark_blue", Material.CYAN_DYE, NamedTextColor.DARK_BLUE),
            new Option("red", Material.RED_DYE, NamedTextColor.RED),
            new Option("dark_red", Material.BROWN_DYE, NamedTextColor.DARK_RED),
            new Option("light_purple", Material.MAGENTA_DYE, NamedTextColor.LIGHT_PURPLE),
            new Option("dark_purple", Material.PURPLE_DYE, NamedTextColor.DARK_PURPLE),
            new Option("gray", Material.LIGHT_GRAY_DYE, NamedTextColor.GRAY),
            new Option("dark_gray", Material.GRAY_DYE, NamedTextColor.DARK_GRAY),
            new Option("black", Material.BLACK_DYE, NamedTextColor.BLACK)
    );

    private NpcGlowColors() {
    }

    public static List<Option> options() {
        return OPTIONS;
    }

    public static NamedTextColor resolveColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return NamedTextColor.WHITE;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (Option option : OPTIONS) {
            if (option.id().equals(key)) {
                return option.color();
            }
        }
        return NamedTextColor.NAMES.valueOr(key, NamedTextColor.WHITE);
    }

    public static Optional<Option> findOption(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of(OPTIONS.get(0));
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (Option option : OPTIONS) {
            if (option.id().equals(key)) {
                return Optional.of(option);
            }
        }
        return Optional.empty();
    }

    public static String normalizeId(String raw) {
        return findOption(raw).map(Option::id).orElse(OPTIONS.get(0).id());
    }
}
