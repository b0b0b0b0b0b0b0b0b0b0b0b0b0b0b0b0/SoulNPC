package bm.b0b0b0.SoulNPC.command;

import bm.b0b0b0.SoulNPC.mob.NpcCreateTypeParser;
import bm.b0b0b0.SoulNPC.model.NpcDisplayType;
import bm.b0b0b0.SoulNPC.model.NpcMobDisplayPose;
import bm.b0b0b0.SoulNPC.repository.NpcRepository;
import bm.b0b0b0.SoulNPC.util.NpcIdValidator;

import java.util.Optional;

final class NpcCreateArgsParser {

    enum Failure {
        INVALID_ID,
        INVALID_TYPE
    }

    record Result(String id, boolean autoId, NpcDisplayType type, String entityType, NpcMobDisplayPose mobDisplayPose) {
    }

    private NpcCreateArgsParser() {
    }

    static Optional<Result> parse(String[] args, NpcRepository repository) {
        if (args.length <= 1) {
            return Optional.of(playerAuto(repository));
        }
        if (args.length == 2) {
            return parseSingleArg(args[1], repository);
        }
        return parseIdAndType(args[1], args[2]);
    }

    static Optional<Failure> failureReason(String[] args) {
        if (args.length <= 1) {
            return Optional.empty();
        }
        if (args.length == 2) {
            String arg = args[1];
            if ("player".equalsIgnoreCase(arg) || NpcCreateTypeParser.parseMob(arg).isPresent()) {
                return Optional.empty();
            }
            return NpcIdValidator.isValidId(arg) ? Optional.empty() : Optional.of(Failure.INVALID_ID);
        }
        if (!NpcIdValidator.isValidId(args[1])) {
            return Optional.of(Failure.INVALID_ID);
        }
        if (resolveType(args[2]).isEmpty()) {
            return Optional.of(Failure.INVALID_TYPE);
        }
        return Optional.empty();
    }

    private static Optional<Result> parseSingleArg(String raw, NpcRepository repository) {
        if ("player".equalsIgnoreCase(raw)) {
            return Optional.of(playerAuto(repository));
        }
        Optional<NpcCreateTypeParser.ParsedMob> mob = NpcCreateTypeParser.parseMob(raw);
        if (mob.isPresent()) {
            return Optional.of(mobAuto(repository, mob.get()));
        }
        if (!NpcIdValidator.isValidId(raw)) {
            return Optional.empty();
        }
        return Optional.of(new Result(
                NpcIdValidator.normalize(raw),
                false,
                NpcDisplayType.PLAYER,
                null,
                NpcMobDisplayPose.STANDING
        ));
    }

    private static Optional<Result> parseIdAndType(String rawId, String rawType) {
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
                selection.mobDisplayPose()
        ));
    }

    private static Result playerAuto(NpcRepository repository) {
        return new Result(
                repository.nextAutoId(),
                true,
                NpcDisplayType.PLAYER,
                null,
                NpcMobDisplayPose.STANDING
        );
    }

    private static Result mobAuto(NpcRepository repository, NpcCreateTypeParser.ParsedMob mob) {
        return new Result(
                repository.nextAutoId(),
                true,
                NpcDisplayType.MOB,
                mob.entityType(),
                mob.mobDisplayPose()
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
