package bm.b0b0b0.SoulNPC.appearance;

public final class InvalidSkinUsernameException extends IllegalArgumentException {

    private final String username;

    public InvalidSkinUsernameException(String username) {
        super("Invalid Minecraft username: " + username);
        this.username = username;
    }

    public String username() {
        return username;
    }
}
