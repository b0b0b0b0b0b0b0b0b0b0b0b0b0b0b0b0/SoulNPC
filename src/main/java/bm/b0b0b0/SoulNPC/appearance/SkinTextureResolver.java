package bm.b0b0b0.SoulNPC.appearance;

import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcSkinSource;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SkinTextureResolver {

    private static final Pattern MINESKIN_VALUE = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MINESKIN_SIGNATURE = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]*)\"");

    private SkinTextureResolver() {
    }

    static void resolveUrl(JavaPlugin plugin, String url, String displayName, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        if (url == null || url.isBlank()) {
            onError.accept(new IllegalArgumentException("empty url"));
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url.trim())).GET().timeout(Duration.ofSeconds(20)).build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("HTTP " + response.statusCode());
                }
                byte[] body = response.body();
                ResolvedSkinTexture texture;
                String contentType = response.headers().firstValue("content-type").orElse("");
                if (contentType.contains("json") || looksLikeJson(body)) {
                    String json = new String(body, StandardCharsets.UTF_8);
                    texture = parseMineskinJson(json);
                } else {
                    texture = textureFromPng(body, url.trim());
                }
                deliver(plugin, displayName, texture, onReady);
            } catch (Exception error) {
                plugin.getServer().getScheduler().runTask(plugin, () -> onError.accept(error));
            }
        });
    }

    static void resolveFile(JavaPlugin plugin, String fileName, String displayName, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        if (fileName == null || fileName.isBlank()) {
            onError.accept(new IllegalArgumentException("empty file"));
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Path path = resolveSkinPath(plugin, fileName.trim());
                if (!Files.isRegularFile(path)) {
                    throw new IOException("file not found: " + path);
                }
                byte[] png = Files.readAllBytes(path);
                ResolvedSkinTexture texture = textureFromPng(png, "file:" + path.getFileName());
                deliver(plugin, displayName, texture, onReady);
            } catch (Exception error) {
                plugin.getServer().getScheduler().runTask(plugin, () -> onError.accept(error));
            }
        });
    }

    static void resolveMineskinId(JavaPlugin plugin, String mineskinId, String displayName, Consumer<PlayerProfile> onReady, Consumer<Throwable> onError) {
        if (mineskinId == null || mineskinId.isBlank()) {
            onError.accept(new IllegalArgumentException("empty mineskin id"));
            return;
        }
        String id = mineskinId.trim();
        String apiUrl = "https://api.mineskin.org/v2/queue/" + id;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl)).GET().timeout(Duration.ofSeconds(20)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("HTTP " + response.statusCode());
                }
                ResolvedSkinTexture texture = parseMineskinJson(response.body());
                deliver(plugin, displayName, texture, onReady);
            } catch (Exception error) {
                plugin.getServer().getScheduler().runTask(plugin, () -> onError.accept(error));
            }
        });
    }

    static String profileKey(NpcAppearanceData appearance, String fallback) {
        return switch (appearance.skinSource == null ? NpcSkinSource.NICK : appearance.skinSource) {
            case URL -> appearance.skinUrl == null || appearance.skinUrl.isBlank() ? fallback : appearance.skinUrl;
            case FILE -> appearance.skinFile == null || appearance.skinFile.isBlank() ? fallback : appearance.skinFile;
            case MINESKIN_ID -> appearance.profile == null || appearance.profile.isBlank() ? fallback : appearance.profile;
            case NICK -> appearance.profile == null || appearance.profile.isBlank() ? fallback : appearance.profile;
        };
    }

    private static Path resolveSkinPath(JavaPlugin plugin, String fileName) {
        Path direct = Path.of(fileName);
        if (direct.isAbsolute()) {
            return direct.normalize();
        }
        Path inSkins = plugin.getDataFolder().toPath().resolve("skins").resolve(fileName).normalize();
        if (Files.isRegularFile(inSkins)) {
            return inSkins;
        }
        return plugin.getDataFolder().toPath().resolve(fileName).normalize();
    }

    private static void deliver(JavaPlugin plugin, String displayName, ResolvedSkinTexture texture, Consumer<PlayerProfile> onReady) {
        String name = displayName == null || displayName.isBlank() ? "Steve" : displayName;
        PlayerProfile profile = SkinProfileFactory.fromTexture(name, texture);
        plugin.getServer().getScheduler().runTask(plugin, () -> onReady.accept(profile));
    }

    private static ResolvedSkinTexture textureFromPng(byte[] png, String url) {
        if (url != null && !url.isBlank() && !url.startsWith("file:")) {
            return textureFromSkinUrl(url);
        }
        String pngBase64 = Base64.getEncoder().encodeToString(png);
        String inner = "{\"textures\":{\"SKIN\":{\"url\":\"data:image/png;base64," + pngBase64 + "\"}}}";
        return new ResolvedSkinTexture(Base64.getEncoder().encodeToString(inner.getBytes(StandardCharsets.UTF_8)), "");
    }

    private static ResolvedSkinTexture textureFromSkinUrl(String url) {
        String inner = "{\"textures\":{\"SKIN\":{\"url\":\"" + escapeJson(url.trim()) + "\"}}}";
        return new ResolvedSkinTexture(Base64.getEncoder().encodeToString(inner.getBytes(StandardCharsets.UTF_8)), "");
    }

    private static ResolvedSkinTexture parseMineskinJson(String json) {
        Matcher valueMatcher = MINESKIN_VALUE.matcher(json);
        Matcher signatureMatcher = MINESKIN_SIGNATURE.matcher(json);
        if (!valueMatcher.find()) {
            throw new IllegalArgumentException("mineskin value missing");
        }
        String signature = signatureMatcher.find() ? signatureMatcher.group(1) : "";
        return new ResolvedSkinTexture(valueMatcher.group(1), signature);
    }

    private static boolean looksLikeJson(byte[] body) {
        if (body.length == 0) {
            return false;
        }
        char first = (char) body[0];
        return first == '{' || first == '[';
    }

    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
