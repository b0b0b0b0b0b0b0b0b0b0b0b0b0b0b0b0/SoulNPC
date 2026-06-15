package bm.b0b0b0.SoulNPC.packet;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;

import java.util.UUID;

public final class PacketProfileConverter {

    private PacketProfileConverter() {
    }

    public static UserProfile toUserProfile(PlayerProfile paperProfile, UUID npcUuid, String profileName) {
        UserProfile userProfile = new UserProfile(npcUuid, profileName);
        for (ProfileProperty property : paperProfile.getProperties()) {
            userProfile.getTextureProperties().add(
                    new TextureProperty(property.getName(), property.getValue(), property.getSignature())
            );
        }
        return userProfile;
    }
}
