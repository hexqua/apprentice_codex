package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagiCompressorGadgetServerConfig {
    private final ForgeConfigSpec.DoubleValue manaCostPerSecond;
    private final ForgeConfigSpec.DoubleValue airFillPerSecond;
    private final ForgeConfigSpec.DoubleValue maxAir;
    private Double manaCostPerSecondOverride;
    private Double airFillPerSecondOverride;
    private Double maxAirOverride;

    private MagiCompressorGadgetServerConfig(
            ForgeConfigSpec.DoubleValue manaCostPerSecond,
            ForgeConfigSpec.DoubleValue airFillPerSecond,
            ForgeConfigSpec.DoubleValue maxAir
    ) {
        this.manaCostPerSecond = manaCostPerSecond;
        this.airFillPerSecond = airFillPerSecond;
        this.maxAir = maxAir;
    }

    public static MagiCompressorGadgetServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("MagiCompressorGadget");

        var manaCostPerSecond = builder
                .comment("Mana consumed by the Magi-Compressor Gadget per second while converting air.")
                .defineInRange("manaCostPerSecond", 40.0D, 0.0D, 100000.0D);
        var airFillPerSecond = builder
                .comment("Compressed air filled by the Magi-Compressor Gadget per second.")
                .defineInRange("airFillPerSecond", 5.0D, 0.0D, 100000.0D);
        var maxAir = builder
                .comment("Maximum compressed air held by the Magi-Compressor Gadget. Values above Create's Backtank capacity cannot be fully exposed to Create air consumers.")
                .defineInRange("maxAir", 50.0D, 0.0D, 100000.0D);

        builder.pop();
        return new MagiCompressorGadgetServerConfig(manaCostPerSecond, airFillPerSecond, maxAir);
    }

    public float manaCostPerSecond() {
        return (manaCostPerSecondOverride == null
                ? manaCostPerSecond.get()
                : manaCostPerSecondOverride).floatValue();
    }

    public float airFillPerSecond() {
        return (airFillPerSecondOverride == null
                ? airFillPerSecond.get()
                : airFillPerSecondOverride).floatValue();
    }

    public float maxAir() {
        return (maxAirOverride == null
                ? maxAir.get()
                : maxAirOverride).floatValue();
    }

    public void setForGameTest(double manaCostPerSecond, double airFillPerSecond, double maxAir) {
        this.manaCostPerSecondOverride = manaCostPerSecond;
        this.airFillPerSecondOverride = airFillPerSecond;
        this.maxAirOverride = maxAir;
    }
}
