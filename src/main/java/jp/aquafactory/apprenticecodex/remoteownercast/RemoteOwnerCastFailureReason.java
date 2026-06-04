package jp.aquafactory.apprenticecodex.remoteownercast;

public enum RemoteOwnerCastFailureReason {
    NONE,
    EMPTY_SPELL,
    NO_PROFILE,
    ORIGIN_NOT_ALLOWED,
    RECAST_NOT_ALLOWED,
    ACTIVE_RECAST_EXISTS,
    SERVER_DENIED,
    REMOTE_PLAYER_GEOMETRY_DISABLED,
    UNSUPPORTED_CAST_TYPE
}
