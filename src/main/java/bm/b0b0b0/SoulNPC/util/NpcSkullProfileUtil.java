package bm.b0b0b0.SoulNPC.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Optional;
import java.util.regex.Pattern;

public final class NpcSkullProfileUtil {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    private NpcSkullProfileUtil() {
    }

    public static void applySkullProfile(SkullMeta skullMeta, String profileName, String npcId) {
        if (skullMeta == null) {
            return;
        }
        resolveProfileName(profileName, npcId).ifPresent(name -> {
            try {
                skullMeta.setPlayerProfile(Bukkit.createProfile(name));
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    public static Optional<String> resolveProfileName(String profileName, String npcId) {
        Optional<String> fromProfile = sanitize(profileName);
        if (fromProfile.isPresent()) {
            return fromProfile;
        }
        return sanitize(npcId);
    }

    private static Optional<String> sanitize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (VALID_NAME.matcher(trimmed).matches()) {
            return Optional.of(trimmed);
        }
        return Optional.empty();
    }
}
