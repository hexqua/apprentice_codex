package jp.aquafactory.apprenticecodex.item.focusstaffbow;

public record FocusStaffbowChargeEffectState(
        boolean visible,
        String spellId,
        long startedGameTime,
        long elapsedTicks,
        double chargeMultiplier,
        float longRampProgress
) {
    public static final FocusStaffbowChargeEffectState HIDDEN =
            new FocusStaffbowChargeEffectState(false, "", 0L, 0L, 1.0D, 0.0F);

    public static FocusStaffbowChargeEffectState visible(String spellId, long startedGameTime, long elapsedTicks,
                                                         double chargeMultiplier, float longRampProgress) {
        if (spellId == null || spellId.isEmpty()) {
            return HIDDEN;
        }

        return new FocusStaffbowChargeEffectState(
                true,
                spellId,
                startedGameTime,
                Math.max(0L, elapsedTicks),
                Math.max(1.0D, chargeMultiplier),
                net.minecraft.util.Mth.clamp(longRampProgress, 0.0F, 1.0F)
        );
    }
}
