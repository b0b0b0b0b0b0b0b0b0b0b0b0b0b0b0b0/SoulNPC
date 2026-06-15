package bm.b0b0b0.SoulNPC.service;

import bm.b0b0b0.SoulNPC.mob.NpcEntityTypeResolver;
import bm.b0b0b0.SoulNPC.mob.NpcMobProfile;
import bm.b0b0b0.SoulNPC.mob.NpcMobProfileRegistry;
import bm.b0b0b0.SoulNPC.model.NpcAnimationType;
import bm.b0b0b0.SoulNPC.model.NpcDisplayType;
import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import bm.b0b0b0.SoulNPC.model.NpcGreetStyle;
import bm.b0b0b0.SoulNPC.model.NpcMobPoseMode;
import bm.b0b0b0.SoulNPC.util.NpcLocationUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class NpcDefaultsFactory {

    public NpcFileData createFromPlayer(Player player, String id) {
        return createFromPlayer(player, id, NpcDisplayType.PLAYER);
    }

    public NpcFileData createFromPlayer(Player player, String id, NpcDisplayType type) {
        return createFromPlayer(player, id, type, null, NpcMobDisplayPose.STANDING);
    }

    public NpcFileData createFromPlayer(
            Player player,
            String id,
            NpcDisplayType type,
            String entityType
    ) {
        return createFromPlayer(player, id, type, entityType, NpcMobDisplayPose.STANDING);
    }

    public NpcFileData createFromPlayer(
            Player player,
            String id,
            NpcDisplayType type,
            String entityType,
            NpcMobDisplayPose mobDisplayPose
    ) {
        Location location = NpcLocationUtil.createAtPlayer(player);
        NpcFileData data = new NpcFileData(id);
        data.world = location.getWorld().getName();
        data.x = location.getX();
        data.y = location.getY();
        data.z = location.getZ();
        data.yaw = location.getYaw();
        data.pitch = location.getPitch();

        data.appearance.useTextDisplay = true;
        data.appearance.noGravity = true;
        data.appearance.name = "<gradient:#7C3AED:#A855F7>" + id + "</gradient>";
        data.appearance.description = "<gray>Тут ваше описание</gray>";
        data.appearance.profile = player.getName();

        applyDisplayType(data, type, entityType, mobDisplayPose);

        data.prepareForYamlSave();
        return data;
    }

    private static void applyDisplayType(
            NpcFileData data,
            NpcDisplayType type,
            String entityType,
            NpcMobDisplayPose mobDisplayPose
    ) {
        if (type == null || type.isPlayerModel()) {
            data.appearance.type = NpcDisplayType.PLAYER;
            data.appearance.entityType = "";
            applyPlayerDefaults(data);
            return;
        }

        String resolvedEntity = resolveEntityType(type, entityType);
        if (resolvedEntity == null) {
            data.appearance.type = NpcDisplayType.PLAYER;
            data.appearance.entityType = "";
            applyPlayerDefaults(data);
            return;
        }

        NpcEntityTypeResolver.applyMob(data.appearance, resolvedEntity);
        String canonicalEntity = NpcEntityTypeResolver.canonicalMobId(resolvedEntity);
        NpcMobProfile profile = NpcMobProfileRegistry.resolveForEntityId(canonicalEntity);
        data.appearance.profile = "";
        data.appearance.hologramBaseOffset = profile.hologramBaseOffset();
        data.appearance.entityPose = NpcEntityPose.STANDING;
        data.appearance.mobDisplayPose = profile.resolveDisplayPose(mobDisplayPose);
        data.lookAtPlayers = true;
        data.lookAtRange = 12;
        if (profile.mobPoseAnimation()) {
            data.animation.enabled = true;
            data.animation.type = NpcAnimationType.MOB_POSE;
            data.animation.intervalTicks = 100;
            data.animation.mobPoseMode = NpcMobPoseMode.SEQUENTIAL;
            data.animation.mobPoses = defaultMobPosesFor(profile);
        } else {
            data.animation.enabled = false;
            data.animation.type = NpcAnimationType.NONE;
        }
    }

    private static List<String> defaultMobPosesFor(NpcMobProfile profile) {
        List<String> poses = new ArrayList<>();
        for (NpcEntityPose pose : profile.supportedPoses()) {
            poses.add(pose.name());
        }
        if (poses.isEmpty()) {
            poses.add(NpcEntityPose.STANDING.name());
        }
        return poses;
    }

    private static String resolveEntityType(NpcDisplayType type, String entityType) {
        if (entityType != null && !entityType.isBlank()) {
            return NpcEntityTypeResolver.normalizeMobId(entityType);
        }
        return switch (type.name()) {
            case "FOX" -> "fox";
            case "ARMOR_STAND" -> "armor_stand";
            default -> null;
        };
    }

    private static void applyPlayerDefaults(NpcFileData data) {
        data.lookAtPlayers = true;
        data.lookAtRange = 12;
        data.animation.enabled = true;
        data.animation.type = NpcAnimationType.GREET;
        data.animation.greetRange = 8;
        data.animation.greetTurnTicks = 16;
        data.animation.greetNodTicks = 22;
        data.animation.greetNodDegrees = 9.0F;
        data.animation.greetStyle = NpcGreetStyle.CROUCH;
        data.animation.greetCrouchTicks = 18;
        data.animation.greetCooldownSeconds = 25;
    }
}
