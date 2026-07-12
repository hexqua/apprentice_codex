package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ParrycastBucklerServerConfig {
    private final ForgeConfigSpec.IntValue perfectGuardTicks;
    private final ForgeConfigSpec.IntValue releaseCooldownTicks;
    private final ForgeConfigSpec.IntValue perfectGuardReleaseCooldownGraceTicks;
    private final ForgeConfigSpec.IntValue perfectGuardReleaseCooldownGraceUses;

    private ParrycastBucklerServerConfig(
            ForgeConfigSpec.IntValue perfectGuardTicks,
            ForgeConfigSpec.IntValue releaseCooldownTicks,
            ForgeConfigSpec.IntValue perfectGuardReleaseCooldownGraceTicks,
            ForgeConfigSpec.IntValue perfectGuardReleaseCooldownGraceUses
    ) {
        this.perfectGuardTicks = perfectGuardTicks;
        this.releaseCooldownTicks = releaseCooldownTicks;
        this.perfectGuardReleaseCooldownGraceTicks = perfectGuardReleaseCooldownGraceTicks;
        this.perfectGuardReleaseCooldownGraceUses = perfectGuardReleaseCooldownGraceUses;
    }

    public static ParrycastBucklerServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ParrycastBuckler");
        var perfectGuardTicks = builder
                .comment("Ticks after starting to use Parrycast Buckler that are treated as a perfect guard.")
                .defineInRange("perfectGuardTicks", 10, 0, 72000);
        var releaseCooldownTicks = builder
                .comment("Cooldown ticks applied when Parrycast Buckler use ends without perfect-guard grace.")
                .defineInRange("releaseCooldownTicks", 40, 0, 72000);
        var graceTicks = builder
                .comment("Ticks after a perfect guard during which one or more releases can skip the item cooldown.")
                .defineInRange("perfectGuardReleaseCooldownGraceTicks", 40, 0, 72000);
        var graceUses = builder
                .comment("Release cooldown skips granted by a perfect guard.")
                .defineInRange("perfectGuardReleaseCooldownGraceUses", 1, 0, 1000000);
        builder.pop();
        return new ParrycastBucklerServerConfig(perfectGuardTicks, releaseCooldownTicks, graceTicks, graceUses);
    }

    public int perfectGuardTicks() { return perfectGuardTicks.get(); }
    public int releaseCooldownTicks() { return releaseCooldownTicks.get(); }
    public int perfectGuardReleaseCooldownGraceTicks() { return perfectGuardReleaseCooldownGraceTicks.get(); }
    public int perfectGuardReleaseCooldownGraceUses() { return perfectGuardReleaseCooldownGraceUses.get(); }
}
