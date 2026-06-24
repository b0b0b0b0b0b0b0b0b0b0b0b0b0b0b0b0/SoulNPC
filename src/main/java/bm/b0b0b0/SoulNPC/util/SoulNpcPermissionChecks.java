package bm.b0b0b0.SoulNPC.util;

import bm.b0b0b0.SoulNPC.config.PluginConfig;
import bm.b0b0b0.SoulNPC.config.settings.SoulNpcSettings;
import bm.b0b0b0.SoulNPC.lang.MessageService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SoulNpcPermissionChecks {

    private SoulNpcPermissionChecks() {
    }

    public static boolean hasAdmin(CommandSender sender, PluginConfig config) {
        return sender.hasPermission(permissions(config).admin);
    }

    public static boolean hasCreate(CommandSender sender, PluginConfig config) {
        return hasAdmin(sender, config) || sender.hasPermission(permissions(config).create);
    }

    public static boolean hasDelete(CommandSender sender, PluginConfig config) {
        return hasAdmin(sender, config) || sender.hasPermission(permissions(config).delete);
    }

    public static boolean hasEditGui(CommandSender sender, PluginConfig config) {
        return hasAdmin(sender, config) || sender.hasPermission(permissions(config).edit);
    }

    public static boolean hasAnyCommandAccess(CommandSender sender, PluginConfig config) {
        SoulNpcSettings.Permissions permissions = permissions(config);
        return sender.hasPermission(permissions.admin)
                || sender.hasPermission(permissions.create)
                || sender.hasPermission(permissions.delete)
                || sender.hasPermission(permissions.edit);
    }

    public static boolean requireAdmin(CommandSender sender, PluginConfig config, MessageService messages) {
        if (hasAdmin(sender, config)) {
            return true;
        }
        deny(sender, messages);
        return false;
    }

    public static boolean requireCreate(CommandSender sender, PluginConfig config, MessageService messages) {
        if (hasCreate(sender, config)) {
            return true;
        }
        deny(sender, messages);
        return false;
    }

    public static boolean requireDelete(CommandSender sender, PluginConfig config, MessageService messages) {
        if (hasDelete(sender, config)) {
            return true;
        }
        deny(sender, messages);
        return false;
    }

    public static boolean requireEditGui(Player player, PluginConfig config, MessageService messages) {
        if (hasEditGui(player, config)) {
            return true;
        }
        deny(player, messages);
        return false;
    }

    public static boolean requireAnyCommandAccess(CommandSender sender, PluginConfig config, MessageService messages) {
        if (hasAnyCommandAccess(sender, config)) {
            return true;
        }
        deny(sender, messages);
        return false;
    }

    private static SoulNpcSettings.Permissions permissions(PluginConfig config) {
        return config.settings().permissions;
    }

    private static void deny(CommandSender sender, MessageService messages) {
        sender.sendMessage(messages.message(sender instanceof Player player ? player : null, "command.no-permission"));
    }
}
