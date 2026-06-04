package jp.aquafactory.apprenticecodex.remoteownercast;

import java.util.Objects;

public final class RemoteOwnerCastService {
    private RemoteOwnerCastService() {
    }

    public static RemoteOwnerCastResult cast(RemoteOwnerCastRequest request) {
        Objects.requireNonNull(request, "request");

        var check = RemoteOwnerCastRules.checkExecution(request.spellData(), request.owner(), request.origin());
        if (!check.isAllowed()) {
            return deniedResult(check.failureReason());
        }

        var profile = check.profile().orElseThrow();
        var runnerResult = RemoteOwnerCastRunner.tryCast(
                request.level(),
                request.owner(),
                request.sourceStack(),
                request.spellData(),
                profile,
                request.origin(),
                request.providedOrigin(),
                request.providedForward(),
                request.castSource(),
                request.castingSlot(),
                request.postSpellPreCastEvent(),
                request.manaPolicy(),
                request.reservedOwnerMana()
        );
        if (!runnerResult.handled()) {
            return RemoteOwnerCastResult.notHandled(RemoteOwnerCastFailureReason.UNSUPPORTED_CAST_TYPE);
        }
        if (!runnerResult.succeeded()) {
            return RemoteOwnerCastResult.failed(RemoteOwnerCastFailureReason.CAST_FAILED);
        }

        return RemoteOwnerCastResult.success();
    }

    private static RemoteOwnerCastResult deniedResult(RemoteOwnerCastFailureReason failureReason) {
        return switch (failureReason) {
            case EMPTY_SPELL, NO_PROFILE, ORIGIN_NOT_ALLOWED, UNSUPPORTED_CAST_TYPE ->
                    RemoteOwnerCastResult.notHandled(failureReason);
            case RECAST_NOT_ALLOWED, ACTIVE_RECAST_EXISTS, SERVER_DENIED, REMOTE_PLAYER_GEOMETRY_DISABLED, CAST_FAILED ->
                    RemoteOwnerCastResult.failed(failureReason);
            case NONE -> throw new IllegalArgumentException("NONE is not a denial reason");
        };
    }
}
