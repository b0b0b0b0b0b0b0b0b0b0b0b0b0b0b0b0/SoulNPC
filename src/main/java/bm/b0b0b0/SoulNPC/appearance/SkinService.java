package bm.b0b0b0.SoulNPC.appearance;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
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

    public boolean isSkinRestorerAvailable() {
        return skinRestorerHook.isAvailable();
    }

    public String resolveProfileKeyForPlayer(Player player) {
        return skinRestorerHook.resolveProfileKey(player);
    }

    public void resolveProfile(String profileName, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        String name = profileName == null || profileName.isBlank() ? "Steve" : profileName.trim();

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            resolveOnlinePlayer(online, onReady, onError);
            return;
        }

        if (skinRestorerHook.isAvailable()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                skinRestorerHook.resolveOffline(name).ifPresentOrElse(
                        texture -> deliverProfile(name, texture, onReady),
                        () -> resolveViaMojang(name, onReady, onError)
                );
            });
            return;
        }

        resolveViaMojang(name, onReady, onError);
    }

    private void resolveOnlinePlayer(Player player, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        Runnable deliver = () -> {
            if (skinRestorerHook.isAvailable()) {
                skinRestorerHook.resolveOnlinePlayer(player).ifPresentOrElse(
                        texture -> deliverProfile(player.getName(), texture, onReady),
                        () -> onReady.accept(player.getPlayerProfile())
                );
                return;
            }
            onReady.accept(player.getPlayerProfile());
        };
        if (Bukkit.isPrimaryThread()) {
            deliver.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, deliver);
        }
    }

    private void deliverProfile(String displayName, ResolvedSkinTexture texture, Consumer<PlayerProfile> onReady) {
        plugin.getServer().getScheduler().runTask(plugin, () -> onReady.accept(toPaperProfile(displayName, texture)));
    }

    private void resolveViaMojang(String name, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        PlayerProfile profile = Bukkit.createProfile(name);
        profile.update().whenComplete((updated, error) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (error != null) {
                onError.accept(error);
                return;
            }
            onReady.accept(updated);
        }));
    }

    private static PlayerProfile toPaperProfile(String displayName, ResolvedSkinTexture texture) {
        PlayerProfile profile = Bukkit.createProfile(displayName);
        profile.setProperty(new ProfileProperty("textures", texture.value(), texture.signature()));
        return profile;
    }
}
