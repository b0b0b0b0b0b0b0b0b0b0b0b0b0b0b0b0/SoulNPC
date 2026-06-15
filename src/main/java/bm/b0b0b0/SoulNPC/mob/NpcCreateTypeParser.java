package bm.b0b0b0.SoulNPC.mob;

import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class NpcCreateTypeParser {

    private static final Map<String, NpcMobDisplayPose> DISPLAY_SUFFIXES = Map.of(
            "on_back", NpcMobDisplayPose.ON_BACK,
            "lying", NpcMobDisplayPose.ON_BACK,
            "hanging", NpcMobDisplayPose.HANGING,
            "upside_down", NpcMobDisplayPose.HANGING
    );

    private NpcCreateTypeParser() {
    }

    public record ParsedMob(String entityType, NpcMobDisplayPose mobDisplayPose) {
    }

    public static Optional<ParsedMob> parseMob(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        DisplaySuffix suffix = parseDisplaySuffix(normalized).orElse(null);
        if (suffix != null) {
            normalized = suffix.mobId();
        }
        if (!NpcEntityTypeResolver.isValidMobId(normalized)) {
            return Optional.empty();
        }
        NpcMobDisplayPose displayPose = suffix == null ? NpcMobDisplayPose.STANDING : suffix.pose();
        return Optional.of(new ParsedMob(
                NpcEntityTypeResolver.normalizeMobId(normalized),
                displayPose
        ));
    }

    private record DisplaySuffix(String mobId, NpcMobDisplayPose pose) {
    }

    private static Optional<DisplaySuffix> parseDisplaySuffix(String normalized) {
        int colon = normalized.indexOf(':');
        if (colon > 0 && colon < normalized.length() - 1) {
            String mobId = normalized.substring(0, colon);
            NpcMobDisplayPose pose = NpcMobDisplayPose.fromString(normalized.substring(colon + 1));
            if (NpcEntityTypeResolver.isValidMobId(mobId)) {
                return Optional.of(new DisplaySuffix(NpcEntityTypeResolver.normalizeMobId(mobId), pose));
            }
            return Optional.empty();
        }
        for (Map.Entry<String, NpcMobDisplayPose> entry : DISPLAY_SUFFIXES.entrySet()) {
            String token = "_" + entry.getKey();
            if (normalized.endsWith(token) && normalized.length() > token.length()) {
                String mobId = normalized.substring(0, normalized.length() - token.length());
                if (NpcEntityTypeResolver.isValidMobId(mobId)) {
                    return Optional.of(new DisplaySuffix(
                            NpcEntityTypeResolver.normalizeMobId(mobId),
                            entry.getValue()
                    ));
                }
            }
        }
        return Optional.empty();
    }

    public static String[] createTabChoices() {
        String[] mobs = NpcEntityTypeResolver.mobTabChoices();
        String[] extra = {
                "allay:on_back",
                "allay_on_back",
                "bat:hanging",
                "bat_hanging"
        };
        String[] result = new String[mobs.length + extra.length];
        System.arraycopy(mobs, 0, result, 0, mobs.length);
        System.arraycopy(extra, 0, result, mobs.length, extra.length);
        return result;
    }
}
