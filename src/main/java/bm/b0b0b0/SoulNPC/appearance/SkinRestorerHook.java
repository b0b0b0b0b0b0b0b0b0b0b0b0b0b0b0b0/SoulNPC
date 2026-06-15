package bm.b0b0b0.SoulNPC.appearance;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

final class SkinRestorerHook {

    private final JavaPlugin plugin;
    private Object api;

    SkinRestorerHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    boolean tryInit() {
        if (Bukkit.getPluginManager().getPlugin("SkinsRestorer") == null) {
            return false;
        }
        try {
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            api = providerClass.getMethod("get").invoke(null);
            return api != null;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[SoulNPC] SkinRestorer найден, но API недоступен: " + throwable.getMessage());
            return false;
        }
    }

    boolean isAvailable() {
        return api != null;
    }

    Optional<ResolvedSkinTexture> resolveOnlinePlayer(Player player) {
        if (api == null) {
            return Optional.empty();
        }
        try {
            Object playerStorage = invoke(api, "getPlayerStorage");
            Optional<ResolvedSkinTexture> linked = optionalResult(
                    invoke(playerStorage, "getSkinOfPlayer", player.getUniqueId())
            ).flatMap(SkinRestorerHook::toTexture);
            if (linked.isPresent()) {
                return linked;
            }
            return optionalResult(invoke(
                    playerStorage,
                    "getSkinForPlayer",
                    player.getUniqueId(),
                    player.getName(),
                    Bukkit.getOnlineMode()
            )).flatMap(SkinRestorerHook::toTexture);
        } catch (Throwable throwable) {
            plugin.getLogger().fine("[SoulNPC] SkinRestorer online lookup for " + player.getName() + ": "
                    + throwable.getMessage());
            return Optional.empty();
        }
    }

    Optional<ResolvedSkinTexture> resolveOffline(String input) {
        if (api == null || input == null || input.isBlank()) {
            return Optional.empty();
        }
        String name = input.trim();
        try {
            Object skinStorage = invoke(api, "getSkinStorage");

            Optional<ResolvedSkinTexture> stored = optionalResult(invoke(skinStorage, "findSkinData", name))
                    .flatMap(SkinRestorerHook::propertyFromInputData)
                    .flatMap(SkinRestorerHook::toTexture);
            if (stored.isPresent()) {
                return stored;
            }

            Optional<ResolvedSkinTexture> playerSkin = optionalResult(
                    invoke(skinStorage, "getPlayerSkin", name, true)
            ).flatMap(SkinRestorerHook::propertyFromMojangData).flatMap(SkinRestorerHook::toTexture);
            if (playerSkin.isPresent()) {
                return playerSkin;
            }

            return optionalResult(invoke(skinStorage, "findOrCreateSkinData", name))
                    .flatMap(SkinRestorerHook::propertyFromInputData)
                    .flatMap(SkinRestorerHook::toTexture);
        } catch (Throwable throwable) {
            plugin.getLogger().fine("[SoulNPC] SkinRestorer lookup for " + name + ": " + throwable.getMessage());
            return Optional.empty();
        }
    }

    String resolveProfileKey(Player player) {
        if (api == null) {
            return player.getName();
        }
        try {
            Object playerStorage = invoke(api, "getPlayerStorage");
            Optional<String> linked = optionalResult(invoke(playerStorage, "getSkinIdOfPlayer", player.getUniqueId()))
                    .flatMap(SkinRestorerHook::identifierValue);
            if (linked.isPresent()) {
                return linked.get();
            }
            Optional<String> joinSkin = optionalResult(invoke(
                    playerStorage,
                    "getSkinIdForPlayer",
                    player.getUniqueId(),
                    player.getName(),
                    Bukkit.getOnlineMode()
            )).flatMap(SkinRestorerHook::identifierValue);
            if (joinSkin.isPresent()) {
                return joinSkin.get();
            }
        } catch (Throwable throwable) {
            plugin.getLogger().fine("[SoulNPC] SkinRestorer profile key for " + player.getName() + ": "
                    + throwable.getMessage());
        }
        return player.getName();
    }

    private static Optional<Object> propertyFromInputData(Object inputDataResult) {
        return invokeSafe(inputDataResult, "getProperty");
    }

    private static Optional<Object> propertyFromMojangData(Object mojangDataResult) {
        return invokeSafe(mojangDataResult, "getSkinProperty");
    }

    private static Optional<String> identifierValue(Object skinIdentifier) {
        return invokeSafe(skinIdentifier, "getIdentifier").map(Object::toString);
    }

    private static Optional<ResolvedSkinTexture> toTexture(Object skinProperty) {
        if (skinProperty == null) {
            return Optional.empty();
        }
        try {
            String value = (String) skinProperty.getClass().getMethod("getValue").invoke(skinProperty);
            String signature = (String) skinProperty.getClass().getMethod("getSignature").invoke(skinProperty);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ResolvedSkinTexture(value, signature == null ? "" : signature));
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Object> invokeSafe(Object target, String methodName, Object... args) {
        try {
            return optionalResult(invoke(target, methodName, args));
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }
    }

    private static Object invoke(Object target, String methodName, Object... args) throws ReflectiveOperationException {
        for (var method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            return method.invoke(target, args);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + methodName);
    }

    @SuppressWarnings("unchecked")
    private static Optional<Object> optionalResult(Object result) {
        if (result == null) {
            return Optional.empty();
        }
        if (result instanceof Optional<?> optional) {
            return (Optional<Object>) optional;
        }
        return Optional.of(result);
    }
}
