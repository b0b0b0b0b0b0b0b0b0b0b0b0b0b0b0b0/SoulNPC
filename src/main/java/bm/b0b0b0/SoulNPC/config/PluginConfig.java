package bm.b0b0b0.SoulNPC.config;

import bm.b0b0b0.SoulNPC.config.settings.GuiAdminSettings;
import bm.b0b0b0.SoulNPC.config.settings.GuiNpcEditSettings;
import bm.b0b0b0.SoulNPC.config.settings.SoulNpcSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class PluginConfig {

    private final SoulNpcSettings settings;
    private final GuiAdminSettings guiAdminSettings;
    private final GuiNpcEditSettings guiNpcEditSettings;

    public PluginConfig(SoulNpcSettings settings, GuiAdminSettings guiAdminSettings, GuiNpcEditSettings guiNpcEditSettings) {
        this.settings = settings;
        this.guiAdminSettings = guiAdminSettings;
        this.guiNpcEditSettings = guiNpcEditSettings;
    }

    public static PluginConfig load(JavaPlugin plugin) {
        Path dataFolder = plugin.getDataFolder().toPath();
        SoulNpcSettings settings = new SoulNpcSettings();
        settings.reload(dataFolder.resolve("config.yml"));
        mergeLegacyStorage(settings);

        Path guiFolder = dataFolder.resolve("gui");
        guiFolder.toFile().mkdirs();
        GuiAdminSettings guiAdminSettings = new GuiAdminSettings();
        guiAdminSettings.reload(guiFolder.resolve("admin.yml"));

        GuiNpcEditSettings guiNpcEditSettings = new GuiNpcEditSettings();
        guiNpcEditSettings.reload(guiFolder.resolve("npc-edit.yml"));

        return new PluginConfig(settings, guiAdminSettings, guiNpcEditSettings);
    }

    public void reload(JavaPlugin plugin) {
        Path dataFolder = plugin.getDataFolder().toPath();
        settings.reload(dataFolder.resolve("config.yml"));
        mergeLegacyStorage(settings);
        guiAdminSettings.reload(dataFolder.resolve("gui").resolve("admin.yml"));
        guiNpcEditSettings.reload(dataFolder.resolve("gui").resolve("npc-edit.yml"));
    }

    public SoulNpcSettings settings() {
        return settings;
    }

    public GuiAdminSettings guiAdmin() {
        return guiAdminSettings;
    }

    public GuiNpcEditSettings guiNpcEdit() {
        return guiNpcEditSettings;
    }

    public String yamlNpcFolder() {
        String folder = settings.storage.yaml.folder;
        if (folder == null || folder.isBlank()) {
            folder = settings.general.npcFolder;
        }
        return folder == null || folder.isBlank() ? "npcs" : folder;
    }

    private static void mergeLegacyStorage(SoulNpcSettings settings) {
        if (settings.storage.yaml.folder == null || settings.storage.yaml.folder.isBlank()) {
            settings.storage.yaml.folder = settings.general.npcFolder;
        }
        if (settings.general.npcFolder == null || settings.general.npcFolder.isBlank()) {
            settings.general.npcFolder = settings.storage.yaml.folder;
        }
    }
}
