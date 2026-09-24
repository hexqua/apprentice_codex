package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ShootingStarMantleServerConfig {
    private final ModConfigSpec.DoubleValue normalCost;
    private final ModConfigSpec.DoubleValue recoveryCost;

    public ShootingStarMantleServerConfig(ModConfigSpec.Builder builder) {
        builder.push("ShootingStarMantle");
        normalCost = builder.comment("Mana paid in full every 10 idle ticks to restore 2 mantle energy.")
                .defineInRange("normalRecoveryManaCost", 20D, 0D, 1000000D);
        recoveryCost = builder.comment("Mana paid in full every 10 idle ticks to restore 10 mantle energy while depleted or calibrated with a Recovery Rune.")
                .defineInRange("depletedRecoveryManaCost", 100D, 0D, 1000000D);
        builder.pop();
    }

    public float cost(boolean recovering) { return (recovering ? recoveryCost : normalCost).get().floatValue(); }
}
