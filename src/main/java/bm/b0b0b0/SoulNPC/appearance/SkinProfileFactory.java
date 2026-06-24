package bm.b0b0b0.SoulNPC.appearance;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class SkinProfileFactory {

    private SkinProfileFactory() {
    }

    static PlayerProfile fromTexture(String displayName, ResolvedSkinTexture texture) {
        return fromProperties(displayName, List.of(new ProfileProperty(
                "textures",
                texture.value(),
                texture.signature()
        )));
    }

    static PlayerProfile fromProperties(String displayName, Collection<ProfileProperty> properties) {
        PlayerProfile profile = org.bukkit.Bukkit.createProfile(displayName);
        for (ProfileProperty property : properties) {
            profile.setProperty(property);
        }
        return profile;
    }

    static PlayerProfile copyProperties(PlayerProfile source) {
        List<ProfileProperty> properties = new ArrayList<>();
        for (ProfileProperty property : source.getProperties()) {
            properties.add(new ProfileProperty(property.getName(), property.getValue(), property.getSignature()));
        }
        String name = source.getName() == null ? "Steve" : source.getName();
        return fromProperties(name, properties);
    }
}
