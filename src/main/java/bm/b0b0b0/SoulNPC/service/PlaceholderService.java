package bm.b0b0b0.SoulNPC.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaceholderService {

    private static Boolean available;
    private static Object placeholderApi;

    private PlaceholderService() {
    }

    public static String apply(Player player, String text) {
        if (text == null || text.isBlank() || player == null) {
            return text;
        }
        if (!isAvailable()) {
            return text;
        }
        try {
            Object result = placeholderApi.getClass()
                    .getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(placeholderApi, player, text);
            return result == null ? text : result.toString();
        } catch (ReflectiveOperationException ignored) {
            return text;
        }
    }

    private static boolean isAvailable() {
        if (available != null) {
            return available;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            available = false;
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            placeholderApi = apiClass.getField("INSTANCE").get(null);
            available = true;
        } catch (ReflectiveOperationException exception) {
            available = false;
        }
        return available;
    }

    public static void init(JavaPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            plugin.getLogger().info("PlaceholderAPI подключён — плейсхолдеры в действиях NPC.");
        }
    }
}
