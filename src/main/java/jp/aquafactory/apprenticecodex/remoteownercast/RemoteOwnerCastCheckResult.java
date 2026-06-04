package jp.aquafactory.apprenticecodex.remoteownercast;

import java.util.Objects;
import java.util.Optional;

public record RemoteOwnerCastCheckResult(
        RemoteOwnerCastFailureReason failureReason,
        Optional<RemoteOwnerCastProfile> profile
) {
    public RemoteOwnerCastCheckResult {
        Objects.requireNonNull(failureReason, "failureReason");
        profile = Objects.requireNonNull(profile, "profile");
    }

    public static RemoteOwnerCastCheckResult allowed(RemoteOwnerCastProfile profile) {
        return new RemoteOwnerCastCheckResult(
                RemoteOwnerCastFailureReason.NONE,
                Optional.of(Objects.requireNonNull(profile, "profile"))
        );
    }

    public static RemoteOwnerCastCheckResult denied(RemoteOwnerCastFailureReason failureReason) {
        if (failureReason == RemoteOwnerCastFailureReason.NONE) {
            throw new IllegalArgumentException("NONE is not a denial reason");
        }
        return new RemoteOwnerCastCheckResult(failureReason, Optional.empty());
    }

    public boolean isAllowed() {
        return failureReason == RemoteOwnerCastFailureReason.NONE && profile.isPresent();
    }
}
