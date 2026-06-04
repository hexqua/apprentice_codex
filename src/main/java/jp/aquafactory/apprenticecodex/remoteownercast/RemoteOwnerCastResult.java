package jp.aquafactory.apprenticecodex.remoteownercast;

import java.util.Objects;

public record RemoteOwnerCastResult(
        boolean handled,
        boolean succeeded,
        RemoteOwnerCastFailureReason failureReason
) {
    public RemoteOwnerCastResult {
        Objects.requireNonNull(failureReason, "failureReason");
        if (succeeded && failureReason != RemoteOwnerCastFailureReason.NONE) {
            throw new IllegalArgumentException("Successful result must use NONE");
        }
        if (!succeeded && failureReason == RemoteOwnerCastFailureReason.NONE) {
            throw new IllegalArgumentException("Failed result requires a failure reason");
        }
    }

    public static RemoteOwnerCastResult success() {
        return new RemoteOwnerCastResult(true, true, RemoteOwnerCastFailureReason.NONE);
    }

    public static RemoteOwnerCastResult failed(RemoteOwnerCastFailureReason failureReason) {
        return new RemoteOwnerCastResult(true, false, failureReason);
    }

    public static RemoteOwnerCastResult notHandled(RemoteOwnerCastFailureReason failureReason) {
        return new RemoteOwnerCastResult(false, false, failureReason);
    }
}
