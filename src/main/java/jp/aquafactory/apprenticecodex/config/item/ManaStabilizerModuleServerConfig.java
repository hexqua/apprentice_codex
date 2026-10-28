package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ManaStabilizerModuleServerConfig {
    public static final Values DEFAULTS = new Values(0.7, 0.75, -0.5, 0.75, 1);
    private final ForgeConfigSpec.DoubleValue movement, recoil, spread, recovery, mana;
    private Values override;

    private ManaStabilizerModuleServerConfig(ForgeConfigSpec.Builder builder) {
        builder.push("ManaStabilizerModule");
        movement = builder.comment("Movement speed multiplier while aiming, independent of module count.")
                .defineInRange("adsMovementSpeedMultiplier", 0.7, 0, 1);
        recoil = builder.comment("Recoil multiplier per module while aiming.")
                .defineInRange("adsRecoilMultiplier", 0.75, 0, 1);
        spread = builder.comment("Spread added per module while aiming. Negative values improve accuracy.")
                .defineInRange("adsSpreadBonus", -0.5, -Double.MAX_VALUE, 0);
        recovery = builder.comment("Additional residual recoil recovery in degrees per tick per module. 0 disables assistance.")
                .defineInRange("recoveryDegreesPerTick", 0.75, 0, Double.MAX_VALUE);
        mana = builder.comment("Mana consumed per assisted tick per module. 0 makes assistance free.")
                .defineInRange("manaCostPerTick", 1.0, 0, Double.MAX_VALUE);
        builder.pop();
    }

    public static ManaStabilizerModuleServerConfig define(ForgeConfigSpec.Builder builder) {
        return new ManaStabilizerModuleServerConfig(builder);
    }

    public Values values() {
        return override != null ? override : new Values(movement.get(), recoil.get(), spread.get(), recovery.get(), mana.get());
    }

    public AutoCloseable overrideForGameTest(Values values) {
        var previous = override;
        override = values;
        return () -> override = previous;
    }

    public record Values(double adsMovementSpeedMultiplier, double adsRecoilMultiplier, double adsSpreadBonus,
                         double recoveryDegreesPerTick, double manaCostPerTick) {}
}
