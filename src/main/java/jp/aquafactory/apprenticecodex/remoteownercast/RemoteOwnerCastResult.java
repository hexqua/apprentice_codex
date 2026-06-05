package jp.aquafactory.apprenticecodex.remoteownercast;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record RemoteOwnerCastResult(
        boolean handled,
        boolean succeeded,
        RemoteOwnerCastFailureReason failureReason,
        @Nullable RemoteOwnerCastRunner.ContinuousCastSession continuousSession
) {
    public RemoteOwnerCastResult {
        Objects.requireNonNull(failureReason, "failureReason");
        if (succeeded && failureReason != RemoteOwnerCastFailureReason.NONE) {
            throw new IllegalArgumentException("Successful result must use NONE");
        }
        if (!succeeded && failureReason == RemoteOwnerCastFailureReason.NONE) {
            throw new IllegalArgumentException("Failed result requires a failure reason");
        }
        if (!succeeded && continuousSession != null) {
            throw new IllegalArgumentException("Only successful continuous results can carry a session");
        }
    }

    public static RemoteOwnerCastResult success() {
        return new RemoteOwnerCastResult(true, true, RemoteOwnerCastFailureReason.NONE, null);
    }

    public static RemoteOwnerCastResult success(RemoteOwnerCastRunner.ContinuousCastSession continuousSession) {
        Objects.requireNonNull(continuousSession, "continuousSession");
        return new RemoteOwnerCastResult(true, true, RemoteOwnerCastFailureReason.NONE, continuousSession);
    }

    public static RemoteOwnerCastResult failed(RemoteOwnerCastFailureReason failureReason) {
        return new RemoteOwnerCastResult(true, false, failureReason, null);
    }

    public static RemoteOwnerCastResult notHandled(RemoteOwnerCastFailureReason failureReason) {
        return new RemoteOwnerCastResult(false, false, failureReason, null);
    }
}
