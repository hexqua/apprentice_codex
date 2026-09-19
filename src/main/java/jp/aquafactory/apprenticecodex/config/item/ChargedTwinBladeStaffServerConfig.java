package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ChargedTwinBladeStaffServerConfig {
    private final ForgeConfigSpec.IntValue initialManaCost;
    private final ForgeConfigSpec.IntValue sustainManaCostPer10Ticks;
    private Values override;

    private ChargedTwinBladeStaffServerConfig(ForgeConfigSpec.IntValue initial, ForgeConfigSpec.IntValue sustain) {
        initialManaCost = initial;
        sustainManaCostPer10Ticks = sustain;
    }

    public static ChargedTwinBladeStaffServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ChargedTwinBladeStaff");
        var initial = builder.comment("Mana consumed once when Charged Twin Blade Staff activates Riptide.")
                .defineInRange("riptideInitialManaCost", 50, 0, Integer.MAX_VALUE);
        var sustain = builder.comment("Mana consumed every 10 ticks while sustaining Riptide, starting 20 ticks after activation. 20 ticks = 1 second.")
                .defineInRange("riptideSustainManaCostPer10Ticks", 20, 0, Integer.MAX_VALUE);
        builder.pop();
        return new ChargedTwinBladeStaffServerConfig(initial, sustain);
    }

    public Values values() {
        return override != null ? override : new Values(initialManaCost.get(), sustainManaCostPer10Ticks.get());
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    public record Values(int riptideInitialManaCost, int riptideSustainManaCostPer10Ticks) {
        public static final Values DEFAULT = new Values(50, 20);

        public Values {
            riptideInitialManaCost = Math.max(0, riptideInitialManaCost);
            riptideSustainManaCostPer10Ticks = Math.max(0, riptideSustainManaCostPer10Ticks);
        }
    }
}
