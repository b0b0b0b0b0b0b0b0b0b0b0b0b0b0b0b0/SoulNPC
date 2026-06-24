package bm.b0b0b0.SoulNPC.command;

import bm.b0b0b0.SoulNPC.mob.NpcCreateTypeParser;
import bm.b0b0b0.SoulNPC.model.NpcDisplayType;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.util.NpcIdValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class NpcCreateArgsParser {

    enum Failure {
        INVALID_ID,
        INVALID_TYPE
    }

    record Result(
            String id,
            boolean autoId,
            NpcDisplayType type,
            String entityType,
            NpcMobDisplayPose mobDisplayPose,
            String skinProfile
    ) {
    }

    private record ParsedArgs(List<String> positional, String skinProfile) {
    }

    private NpcCreateArgsParser() {
    }

    static Optional<Result> parse(String[] args, NpcRepository repository) {
        ParsedArgs parsedArgs = extractSkinFlags(args);
        List<String> positional = parsedArgs.positional();
        String skinProfile = parsedArgs.skinProfile();
        if (positional.isEmpty()) {
            return Optional.of(playerAuto(repository, skinProfile));
        }
        if (positional.size() == 1) {
            return parseSingleArg(positional.get(0), repository, skinProfile);
        }
        if (positional.size() == 2) {
            return parseIdAndType(positional.get(0), positional.get(1), skinProfile);
        }
        return Optional.empty();
    }

    static Optional<Failure> failureReason(String[] args) {
        ParsedArgs parsedArgs = extractSkinFlags(args);
        List<String> positional = parsedArgs.positional();
        if (positional.isEmpty()) {
            return Optional.empty();
        }
        if (positional.size() == 1) {
            String arg = positional.get(0);
            if ("player".equalsIgnoreCase(arg) || NpcCreateTypeParser.parseMob(arg).isPresent()) {
                return Optional.empty();
            }
            return NpcIdValidator.isValidId(arg) ? Optional.empty() : Optional.of(Failure.INVALID_ID);
        }
        if (positional.size() == 2) {
            if (!NpcIdValidator.isValidId(positional.get(0))) {
                return Optional.of(Failure.INVALID_ID);
            }
            if (resolveType(positional.get(1)).isEmpty()) {
                return Optional.of(Failure.INVALID_TYPE);
            }
            return Optional.empty();
        }
        return Optional.of(Failure.INVALID_ID);
    }

    private static ParsedArgs extractSkinFlags(String[] args) {
        List<String> positional = new ArrayList<>();
        String skinProfile = null;
        for (int index = 1; index < args.length; index++) {
            String arg = args[index];
            if (arg.regionMatches(true, 0, "s-", 0, 2) && arg.length() > 2) {
                skinProfile = arg.substring(2).trim();
                continue;
            }
            if (arg.regionMatches(true, 0, "-s", 0, 2) && arg.length() > 2) {
                skinProfile = arg.substring(2).trim();
                continue;
            }
            if (arg.equalsIgnoreCase("s-") || arg.equalsIgnoreCase("-s")) {
                if (index + 1 < args.length) {
                    skinProfile = args[++index].trim();
                }
                continue;
            }
            positional.add(arg);
        }
        if (skinProfile != null && skinProfile.isBlank()) {
            skinProfile = null;
        }
        return new ParsedArgs(positional, skinProfile);
    }

    private static Optional<Result> parseSingleArg(String raw, NpcRepository repository, String skinProfile) {
        if ("player".equalsIgnoreCase(raw)) {
            return Optional.of(playerAuto(repository, skinProfile));
        }
        Optional<NpcCreateTypeParser.ParsedMob> mob = NpcCreateTypeParser.parseMob(raw);
        if (mob.isPresent()) {
            return Optional.of(mobAuto(repository, mob.get(), skinProfile));
        }
        if (!NpcIdValidator.isValidId(raw)) {
            return Optional.empty();
        }
        return Optional.of(new Result(
                NpcIdValidator.normalize(raw),
                false,
                NpcDisplayType.PLAYER,
                null,
                NpcMobDisplayPose.STANDING,
                skinProfile
        ));
    }

    private static Optional<Result> parseIdAndType(String rawId, String rawType, String skinProfile) {
        if (!NpcIdValidator.isValidId(rawId)) {
            return Optional.empty();
        }
        Optional<TypeSelection> type = resolveType(rawType);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        TypeSelection selection = type.get();
        return Optional.of(new Result(
                NpcIdValidator.normalize(rawId),
                false,
                selection.type(),
                selection.entityType(),
                selection.mobDisplayPose(),
                skinProfile
        ));
    }

    private static Result playerAuto(NpcRepository repository, String skinProfile) {
        return new Result(
                repository.nextAutoId(),
                true,
                NpcDisplayType.PLAYER,
                null,
                NpcMobDisplayPose.STANDING,
                skinProfile
        );
    }

    private static Result mobAuto(NpcRepository repository, NpcCreateTypeParser.ParsedMob mob, String skinProfile) {
        return new Result(
                repository.nextAutoId(),
                true,
                NpcDisplayType.MOB,
                mob.entityType(),
                mob.mobDisplayPose(),
                skinProfile
        );
    }

    private record TypeSelection(
            NpcDisplayType type,
            String entityType,
            NpcMobDisplayPose mobDisplayPose
    ) {
    }

    private static Optional<TypeSelection> resolveType(String rawType) {
        if ("player".equalsIgnoreCase(rawType)) {
            return Optional.of(new TypeSelection(NpcDisplayType.PLAYER, null, NpcMobDisplayPose.STANDING));
        }
        Optional<NpcCreateTypeParser.ParsedMob> mob = NpcCreateTypeParser.parseMob(rawType);
        if (mob.isEmpty()) {
            return Optional.empty();
        }
        NpcCreateTypeParser.ParsedMob parsed = mob.get();
        return Optional.of(new TypeSelection(
                NpcDisplayType.MOB,
                parsed.entityType(),
                parsed.mobDisplayPose()
        ));
    }
}
