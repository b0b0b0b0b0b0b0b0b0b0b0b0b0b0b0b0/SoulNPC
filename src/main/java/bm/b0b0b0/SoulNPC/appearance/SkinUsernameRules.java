package bm.b0b0b0.SoulNPC.appearance;

public final class SkinUsernameRules {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 16;

    private SkinUsernameRules() {
    }

    public static boolean isValidMojangUsername(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            return false;
        }
        for (int index = 0; index < trimmed.length(); index++) {
            char character = trimmed.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '_') {
                return false;
            }
        }
        return true;
    }

    public static InvalidSkinUsernameException invalid(String name) {
        return new InvalidSkinUsernameException(name == null ? "" : name.trim());
    }
}
