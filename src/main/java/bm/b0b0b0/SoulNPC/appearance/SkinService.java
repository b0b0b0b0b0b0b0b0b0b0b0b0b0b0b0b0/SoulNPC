package bm.b0b0b0.SoulNPC.appearance;

import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcSkinSource;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public final class SkinService {

    private final JavaPlugin plugin;
    private final SkinRestorerHook skinRestorerHook;

    public SkinService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.skinRestorerHook = new SkinRestorerHook(plugin);
    }

    public boolean initSkinRestorer() {
        return skinRestorerHook.tryInit();
    }

    public boolean hasSkinRestorer() {
        return skinRestorerHook.isAvailable();
    }

    public String resolveProfileKeyForPlayer(Player player) {
        return skinRestorerHook.resolveProfileKey(player);
    }

    public void resolveProfile(String profileName, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        String name = profileName == null || profileName.isBlank() ? "Steve" : profileName.trim();
        resolveNick(name, onReady, onError);
    }

    public String profileKey(NpcAppearanceData appearance, String fallback) {
        return SkinTextureResolver.profileKey(appearance, fallback);
    }

    public void resolveAppearance(
            NpcAppearanceData appearance,
            String fallbackName,
            Consumer<PlayerProfile> onReady,
            Consumer<Throwable> onError
    ) {
        if (appearance == null) {
            resolveProfile(fallbackName, onReady, onError);
            return;
        }
        NpcSkinSource source = appearance.skinSource == null ? NpcSkinSource.NICK : appearance.skinSource;
        String displayName = fallbackName == null || fallbackName.isBlank() ? "Steve" : fallbackName;
        switch (source) {
            case URL -> SkinTextureResolver.resolveUrl(
                    plugin,
                    appearance.skinUrl,
                    displayName,
                    onReady,
                    onError
            );
            case FILE -> SkinTextureResolver.resolveFile(
                    plugin,
                    appearance.skinFile,
                    displayName,
                    onReady,
                    onError
            );
            case MINESKIN_ID -> SkinTextureResolver.resolveMineskinId(
                    plugin,
                    appearance.profile,
                    displayName,
                    onReady,
                    onError
            );
            case NICK -> {
                String nick = appearance.profile == null || appearance.profile.isBlank() ? displayName : appearance.profile;
                resolveNick(nick, onReady, onError);
            }
        }
    }

    private void resolveNick(String name, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            resolveOnlinePlayer(online, onReady, onError);
            return;
        }

        if (skinRestorerHook.isAvailable()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                skinRestorerHook.resolveOfflineProfile(name).ifPresentOrElse(
                        profile -> deliverProfile(profile, onReady),
                        () -> resolveViaMojangOrReport(name, onReady, onError)
                );
            });
            return;
        }

        resolveViaMojangOrReport(name, onReady, onError);
    }

    private void resolveOnlinePlayer(Player player, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        Runnable deliver = () -> {
            skinRestorerHook.resolveOnlineProfile(player).ifPresentOrElse(
                    profile -> deliverProfile(profile, onReady),
                    () -> deliverProfile(SkinProfileFactory.copyProperties(player.getPlayerProfile()), onReady)
            );
        };
        if (Bukkit.isPrimaryThread()) {
            deliver.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, deliver);
        }
    }

    private void deliverProfile(PlayerProfile profile, Consumer<PlayerProfile> onReady) {
        plugin.getServer().getScheduler().runTask(plugin, () -> onReady.accept(profile));
    }

    private void resolveViaMojangOrReport(String name, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        if (!SkinUsernameRules.isValidMojangUsername(name)) {
            reportError(onError, SkinUsernameRules.invalid(name));
            return;
        }
        resolveViaMojang(name, onReady, onError);
    }

    private void resolveViaMojang(String name, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        final PlayerProfile profile;
        try {
            profile = Bukkit.createProfile(name);
        } catch (IllegalArgumentException exception) {
            reportError(onError, exception);
            return;
        }
        profile.update().whenComplete((updated, error) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (error != null) {
                if (onError != null) {
                    onError.accept(error);
                }
                return;
            }
            onReady.accept(updated);
        }));
    }

    private void reportError(Consumer<Throwable> onError, Throwable error) {
        if (onError == null) {
            plugin.getLogger().warning("[SoulNPC] Skin resolve failed: " + error.getMessage());
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> onError.accept(error));
    }
}
