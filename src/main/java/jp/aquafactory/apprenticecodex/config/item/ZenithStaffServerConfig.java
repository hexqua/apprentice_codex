package jp.aquafactory.apprenticecodex.config.item;

import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffConfigState;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ZenithStaffServerConfig {
    private final ForgeConfigSpec.DoubleValue manaCostMultiplier;
    private Double manaCostMultiplierOverride;

    private ZenithStaffServerConfig(ForgeConfigSpec.DoubleValue manaCostMultiplier) {
        this.manaCostMultiplier = manaCostMultiplier;
    }

    public static ZenithStaffServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ZenithStaff");
        var manaCostMultiplier = builder
                .comment("Mana cost multiplier for non-strongest-school spells while holding Zenith Staff.")
                .defineInRange("manaCostMultiplier", ZenithStaffConfigState.DEFAULT_MANA_COST_MULTIPLIER, 1.0D, 100.0D);
        builder.pop();

        return new ZenithStaffServerConfig(manaCostMultiplier);
    }

    public float manaCostMultiplier() {
        if (manaCostMultiplierOverride != null) {
            return manaCostMultiplierOverride.floatValue();
        }
        return manaCostMultiplier.get().floatValue();
    }

    public void setManaCostMultiplierForGameTest(double value) {
        manaCostMultiplierOverride = Math.max(1.0D, value);
    }
}
