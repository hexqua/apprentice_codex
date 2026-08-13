package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SoulstainedSteelSwingcastStaffServerConfig {
    public static final double DEFAULT_MANA_COST_PER_BLADE = 15.0D;
    private static final double MAX_MANA_COST_PER_BLADE = 10000.0D;

    private final ModConfigSpec.DoubleValue manaCostPerBlade;
    private Double manaCostPerBladeOverride;

    private SoulstainedSteelSwingcastStaffServerConfig(ModConfigSpec.DoubleValue manaCostPerBlade) {
        this.manaCostPerBlade = manaCostPerBlade;
    }

    public static SoulstainedSteelSwingcastStaffServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("SoulstainedSteelSwingcastStaff");
        var manaCostPerBlade = builder
                .comment(
                        "Base mana consumed for each Mnemonic Blade before Charge Recovery Rate scaling. "
                                + "Set to 0 to make the full burst free and hide its mana-cost tooltip."
                )
                .defineInRange(
                        "manaCostPerBlade",
                        DEFAULT_MANA_COST_PER_BLADE,
                        0.0D,
                        MAX_MANA_COST_PER_BLADE
                );
        builder.pop();
        return new SoulstainedSteelSwingcastStaffServerConfig(manaCostPerBlade);
    }

    public double manaCostPerBlade() {
        return manaCostPerBladeOverride == null ? manaCostPerBlade.get() : manaCostPerBladeOverride;
    }

    public void setForGameTest(double value) {
        manaCostPerBladeOverride = Math.clamp(value, 0.0D, MAX_MANA_COST_PER_BLADE);
    }
}
