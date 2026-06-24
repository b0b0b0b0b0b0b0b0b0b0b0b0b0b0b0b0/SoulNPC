package bm.b0b0b0.SoulNPC.util;

import java.util.Locale;

public final class NpcIdValidator {

    public static final int MAX_LENGTH = 32;

    private NpcIdValidator() {
    }

    public static boolean isValidId(String id) {
        if (id == null || id.isBlank() || id.length() > MAX_LENGTH) {
            return false;
        }
        for (int index = 0; index < id.length(); index++) {
            char character = id.charAt(index);
            if (Character.isLetterOrDigit(character) || character == '_' || character == '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    public static String normalize(String id) {
        return id == null ? "" : id.trim();
    }

    public static String canonicalKey(String id) {
        return normalize(id).toLowerCase(Locale.ROOT);
    }
}
