package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ParrycastBucklerServerConfig {
    private final ModConfigSpec.IntValue perfectGuardTicks;
    private final ModConfigSpec.IntValue releaseCooldownTicks;
    private final ModConfigSpec.IntValue perfectGuardReleaseCooldownGraceTicks;
    private final ModConfigSpec.IntValue perfectGuardReleaseCooldownGraceUses;

    private ParrycastBucklerServerConfig(
            ModConfigSpec.IntValue perfectGuardTicks,
            ModConfigSpec.IntValue releaseCooldownTicks,
            ModConfigSpec.IntValue perfectGuardReleaseCooldownGraceTicks,
            ModConfigSpec.IntValue perfectGuardReleaseCooldownGraceUses
    ) {
        this.perfectGuardTicks = perfectGuardTicks;
        this.releaseCooldownTicks = releaseCooldownTicks;
        this.perfectGuardReleaseCooldownGraceTicks = perfectGuardReleaseCooldownGraceTicks;
        this.perfectGuardReleaseCooldownGraceUses = perfectGuardReleaseCooldownGraceUses;
    }

    public static ParrycastBucklerServerConfig define(ModConfigSpec.Builder builder) {
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
