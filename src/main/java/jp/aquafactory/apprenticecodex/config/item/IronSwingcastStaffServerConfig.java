package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class IronSwingcastStaffServerConfig {
    public static final double DEFAULT_CRYSTALLINE_ARCANE_SHARD_DROP_CHANCE = 0.05D;

    private final ForgeConfigSpec.DoubleValue crystallineArcaneShardDropChance;
    private Double crystallineArcaneShardDropChanceOverride;

    private IronSwingcastStaffServerConfig(ForgeConfigSpec.DoubleValue crystallineArcaneShardDropChance) {
        this.crystallineArcaneShardDropChance = crystallineArcaneShardDropChance;
    }

    public static IronSwingcastStaffServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("IronSwingcastStaff");
        var crystallineArcaneShardDropChance = builder
                .comment("Chance for a mob killed while Iron Swingcast Staff is held in the main hand to drop a Crystalline Arcane Shard. 1.0 = always, 0.0 = disabled.")
                .defineInRange(
                        "crystallineArcaneShardDropChance",
                        DEFAULT_CRYSTALLINE_ARCANE_SHARD_DROP_CHANCE,
                        0.0D,
                        1.0D
                );
        builder.pop();
        return new IronSwingcastStaffServerConfig(crystallineArcaneShardDropChance);
    }

    public double crystallineArcaneShardDropChance() {
        return crystallineArcaneShardDropChanceOverride == null
                ? crystallineArcaneShardDropChance.get()
                : crystallineArcaneShardDropChanceOverride;
    }

    public void setForGameTest(double value) {
        crystallineArcaneShardDropChanceOverride = value;
    }
}
