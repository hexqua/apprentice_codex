package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;

public final class HighTierSwingcastStaffServerConfig {
    public static final int DEFAULT_DIAMOND_COOLDOWN_REDUCTION_TICKS = 20;
    public static final int DEFAULT_NETHERITE_COOLDOWN_REDUCTION_TICKS = 10;
    private static final int MAX_CONFIGURED_TICKS = 72000;

    private final ForgeConfigSpec.IntValue diamondCooldownReductionTicks;
    private final ForgeConfigSpec.IntValue netheriteCooldownReductionTicks;
    private Integer diamondCooldownReductionTicksOverride;
    private Integer netheriteCooldownReductionTicksOverride;

    private HighTierSwingcastStaffServerConfig(
            ForgeConfigSpec.IntValue diamondCooldownReductionTicks,
            ForgeConfigSpec.IntValue netheriteCooldownReductionTicks
    ) {
        this.diamondCooldownReductionTicks = diamondCooldownReductionTicks;
        this.netheriteCooldownReductionTicks = netheriteCooldownReductionTicks;
    }

    public static HighTierSwingcastStaffServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("DiamondSwingcastStaff");
        var diamondCooldownReductionTicks = builder
                .comment("Cooldown ticks removed from the imbued spell after a fully charged Diamond Swingcast Staff attack hits.")
                .defineInRange(
                        "cooldownReductionTicks",
                        DEFAULT_DIAMOND_COOLDOWN_REDUCTION_TICKS,
                        0,
                        MAX_CONFIGURED_TICKS
                );
        builder.pop();

        builder.push("NetheriteSwingcastStaff");
        var netheriteCooldownReductionTicks = builder
                .comment("Cooldown ticks removed from the imbued spell after a fully charged Netherite Swingcast Staff attack hits.")
                .defineInRange(
                        "cooldownReductionTicks",
                        DEFAULT_NETHERITE_COOLDOWN_REDUCTION_TICKS,
                        0,
                        MAX_CONFIGURED_TICKS
                );
        builder.pop();

        return new HighTierSwingcastStaffServerConfig(
                diamondCooldownReductionTicks,
                netheriteCooldownReductionTicks
        );
    }

    public int diamondCooldownReductionTicks() {
        return diamondCooldownReductionTicksOverride == null
                ? diamondCooldownReductionTicks.get()
                : diamondCooldownReductionTicksOverride;
    }

    public int netheriteCooldownReductionTicks() {
        return netheriteCooldownReductionTicksOverride == null
                ? netheriteCooldownReductionTicks.get()
                : netheriteCooldownReductionTicksOverride;
    }

    public void setForGameTest(int diamondTicks, int netheriteTicks) {
        diamondCooldownReductionTicksOverride = clampTicks(diamondTicks);
        netheriteCooldownReductionTicksOverride = clampTicks(netheriteTicks);
    }

    private static int clampTicks(int ticks) {
        return Mth.clamp(ticks, 0, MAX_CONFIGURED_TICKS);
    }
}
