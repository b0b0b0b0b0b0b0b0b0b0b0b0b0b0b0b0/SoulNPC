package bm.b0b0b0.SoulNPC.util.upd;

import bm.b0b0b0.SoulNPC.util.SoulNpcConsole;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public final class SoulNpcUpdateChecker {

    private static final String VERSION_URL = "https://b0b0b0.dev/pl/souls/soulnps.txt";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private SoulNpcUpdateChecker() {
    }

    public static void schedule(JavaPlugin plugin, String currentVersion) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> check(currentVersion), 60L);
    }

    public static void check(String currentVersion) {
        try {
            String latestVersion = fetchLatestVersion();
            if (latestVersion == null) {
                SoulNpcConsole.error("Failed to fetch the latest version. Skipping version check.");
                return;
            }
            if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                SoulNpcConsole.blank();
                SoulNpcConsole.warn("You are using an outdated version!");
                SoulNpcConsole.warn("Current version: \u001B[90m" + currentVersion
                        + "\u001B[33m, latest version: \u001B[32m" + latestVersion + "\u001B[0m.");
                SoulNpcConsole.blank();
                return;
            }
            SoulNpcConsole.success("You are using the latest version of the plugin! Version: \u001B[32m"
                    + currentVersion + "\u001B[0m.");
        } catch (Exception exception) {
            SoulNpcConsole.error("Error during version check: " + exception.getMessage());
        }
    }

    private static String fetchLatestVersion() {
        try {
            URL url = URI.create(VERSION_URL).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line = reader.readLine();
                return line == null ? null : line.trim();
            }
        } catch (IOException exception) {
            SoulNpcConsole.error("Connection error to " + VERSION_URL + ": " + exception.getMessage());
            return null;
        }
    }
}
