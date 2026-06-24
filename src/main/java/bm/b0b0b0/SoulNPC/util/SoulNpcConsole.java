package bm.b0b0b0.SoulNPC.util;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

public final class SoulNpcConsole {

    public static final String PREFIX = "\u001B[37m[\u001B[90mSoulNPC\u001B[37m]\u001B[0m ";

    private SoulNpcConsole() {
    }

    public static void blank() {
        console().sendMessage(" ");
    }

    public static void line(String message) {
        console().sendMessage(PREFIX + message);
    }

    public static void sectionStart() {
        blank();
        line("==============================");
    }

    public static void sectionEnd() {
        line("==============================");
        blank();
    }

    public static void banner(String version, String author) {
        sectionStart();
        line("SoulNPC is starting on your server");
        line("Version:\u001B[90m " + version + " \u001B[0m| Author: \u001B[90m" + author + "\u001B[0m");
        blank();
        line("Initialization:");
    }

    public static void info(String message) {
        line("\u001B[90m→\u001B[0m " + message);
    }

    public static void success(String message) {
        line("\u001B[32m" + message + "\u001B[0m");
    }

    public static void warn(String message) {
        line("\u001B[33m" + message + "\u001B[0m");
    }

    public static void error(String message) {
        line("\u001B[31m" + message + "\u001B[0m");
    }

    public static void errorBlock(String... lines) {
        blank();
        line("\u001B[31m========================================================\u001B[0m");
        for (String message : lines) {
            error(message);
        }
        line("\u001B[31m========================================================\u001B[0m");
        blank();
    }

    public static void integration(String name, boolean present, String whenPresent, String whenAbsent) {
        if (present) {
            success(name + ": " + whenPresent);
        } else {
            info(name + ": " + whenAbsent);
        }
    }

    private static ConsoleCommandSender console() {
        return Bukkit.getConsoleSender();
    }
}
