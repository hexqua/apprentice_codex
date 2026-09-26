package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ShootingStarMantleServerConfig {
    private final ModConfigSpec.DoubleValue recoveryCost;

    public ShootingStarMantleServerConfig(ModConfigSpec.Builder builder) {
        builder.push("ShootingStarMantle");
        recoveryCost = builder.comment("Mana paid in full every 10 idle ticks to restore 10 mantle energy, or 15 with a Recovery Rune.")
                .defineInRange("recoveryManaCost", 50D, 0D, 1000000D);
        builder.pop();
    }

    public float cost() { return recoveryCost.get().floatValue(); }
}
