package jp.aquafactory.apprenticecodex.remoteownercast;

public enum RemoteOwnerCooldownPolicy {
    WEAPON_IMBUE(true, false, false),
    WEAPON_IMBUE_WITH_LONG_CAST_EXTENSION(true, true, false),
    FOLLOWCAST(false, true, true);

    private final boolean skipRecastCooldown;
    private final boolean addLongCastExtension;
    private final boolean useResolvedSpellLevelForLongCastExtension;

    RemoteOwnerCooldownPolicy(
            boolean skipRecastCooldown,
            boolean addLongCastExtension,
            boolean useResolvedSpellLevelForLongCastExtension
    ) {
        this.skipRecastCooldown = skipRecastCooldown;
        this.addLongCastExtension = addLongCastExtension;
        this.useResolvedSpellLevelForLongCastExtension = useResolvedSpellLevelForLongCastExtension;
    }

    boolean skipRecastCooldown() {
        return skipRecastCooldown;
    }

    boolean addLongCastExtension() {
        return addLongCastExtension;
    }

    boolean useResolvedSpellLevelForLongCastExtension() {
        return useResolvedSpellLevelForLongCastExtension;
    }
}
