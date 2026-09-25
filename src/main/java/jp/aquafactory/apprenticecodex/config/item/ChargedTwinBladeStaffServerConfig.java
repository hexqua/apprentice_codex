package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ChargedTwinBladeStaffServerConfig {
    private final ForgeConfigSpec.IntValue throwManaCost;
    private final ForgeConfigSpec.IntValue initialManaCost;
    private final ForgeConfigSpec.IntValue sustainManaCostPer10Ticks;
    private Values override;

    private ChargedTwinBladeStaffServerConfig(ForgeConfigSpec.IntValue initial, ForgeConfigSpec.IntValue sustain, ForgeConfigSpec.IntValue throwCost) {
        throwManaCost = throwCost;
        initialManaCost = initial;
        sustainManaCostPer10Ticks = sustain;
    }

    public static ChargedTwinBladeStaffServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ChargedTwinBladeStaff");
        var throwCost = builder.comment("Base mana consumed when throwing a duplicate staff. Divided by Loyalty level plus one.")
                .defineInRange("throwManaCost", 100, 0, Integer.MAX_VALUE);
        var initial = builder.comment("Mana consumed once when Charged Twin Blade Staff activates Riptide.")
                .defineInRange("riptideInitialManaCost", 50, 0, Integer.MAX_VALUE);
        var sustain = builder.comment("Mana consumed every 10 ticks while sustaining Riptide, starting 20 ticks after activation. 20 ticks = 1 second.")
                .defineInRange("riptideSustainManaCostPer10Ticks", 20, 0, Integer.MAX_VALUE);
        builder.pop();
        return new ChargedTwinBladeStaffServerConfig(initial, sustain, throwCost);
    }

    public Values values() {
        return override != null ? override : new Values(initialManaCost.get(), sustainManaCostPer10Ticks.get(), throwManaCost.get());
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    public record Values(int riptideInitialManaCost, int riptideSustainManaCostPer10Ticks, int throwManaCost) {
        public static final Values DEFAULT = new Values(50, 20, 100);

        public Values {
            throwManaCost = Math.max(0, throwManaCost);
            riptideInitialManaCost = Math.max(0, riptideInitialManaCost);
            riptideSustainManaCostPer10Ticks = Math.max(0, riptideSustainManaCostPer10Ticks);
        }
    }
}
