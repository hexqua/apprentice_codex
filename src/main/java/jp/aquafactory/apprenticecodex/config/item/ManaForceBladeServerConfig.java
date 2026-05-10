package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ManaForceBladeServerConfig {
    private final ModConfigSpec.DoubleValue imbueDamageMultiplierScale;
    private final ModConfigSpec.DoubleValue attackManaCostMultiplier;
    private final ModConfigSpec.DoubleValue attackManaSchoolMultiplierScale;
    private final ModConfigSpec.DoubleValue meleeGuardManaCost;
    private final ModConfigSpec.DoubleValue rangedGuardManaCost;
    private final ModConfigSpec.BooleanValue disableManaRecoveryWhileGuarding;
    private final ModConfigSpec.IntValue perfectGuardTicks;

    private ManaForceBladeServerConfig(
            ModConfigSpec.DoubleValue imbueDamageMultiplierScale,
            ModConfigSpec.DoubleValue attackManaCostMultiplier,
            ModConfigSpec.DoubleValue attackManaSchoolMultiplierScale,
            ModConfigSpec.DoubleValue meleeGuardManaCost,
            ModConfigSpec.DoubleValue rangedGuardManaCost,
            ModConfigSpec.BooleanValue disableManaRecoveryWhileGuarding,
            ModConfigSpec.IntValue perfectGuardTicks
    ) {
        this.imbueDamageMultiplierScale = imbueDamageMultiplierScale;
        this.attackManaCostMultiplier = attackManaCostMultiplier;
        this.attackManaSchoolMultiplierScale = attackManaSchoolMultiplierScale;
        this.meleeGuardManaCost = meleeGuardManaCost;
        this.rangedGuardManaCost = rangedGuardManaCost;
        this.disableManaRecoveryWhileGuarding = disableManaRecoveryWhileGuarding;
        this.perfectGuardTicks = perfectGuardTicks;
    }

    public static ManaForceBladeServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("ManaForceBlade");

        var imbueDamageMultiplierScale = builder
                .comment("Scales the imbued school attack damage multiplier. 1.0 = default behavior. 0 disables imbued attack damage changes.")
                .defineInRange("imbueDamageMultiplierScale", 1.0d, 0.0d, 10.0d);
        var attackManaCostMultiplier = builder
                .comment("Mana cost multiplier for imbued melee hits. 0 disables hit mana cost and its tooltip.")
                .defineInRange("attackManaCostMultiplier", 3.0d, 0.0d, 10000.0d);
        var attackManaSchoolMultiplierScale = builder
                .comment("Scales only the school-derived hit mana cost increase. 1.0 follows damage growth. 0 ignores school multiplier for hit mana cost.")
                .defineInRange("attackManaSchoolMultiplierScale", 1.0d, 0.0d, 10.0d);
        var meleeGuardManaCost = builder.defineInRange("meleeGuardManaCost", 50.0d, 0.0d, 10000.0d);
        var rangedGuardManaCost = builder.defineInRange("rangedGuardManaCost", 20.0d, 0.0d, 10000.0d);
        var disableManaRecoveryWhileGuarding = builder.define("disableManaRecoveryWhileGuarding", true);
        var perfectGuardTicks = builder
                .comment("Ticks treated as perfect guard for vanilla-style Mana Force Blade guard. Not used when Epic Fight is installed because Epic Fight guard skills take priority.")
                .defineInRange("perfectGuardTicks", 10, 0, 72000);

        builder.pop();
        return new ManaForceBladeServerConfig(
                imbueDamageMultiplierScale,
                attackManaCostMultiplier,
                attackManaSchoolMultiplierScale,
                meleeGuardManaCost,
                rangedGuardManaCost,
                disableManaRecoveryWhileGuarding,
                perfectGuardTicks
        );
    }

    public float imbueDamageMultiplierScale() {
        return imbueDamageMultiplierScale.get().floatValue();
    }

    public float attackManaCostMultiplier() {
        return attackManaCostMultiplier.get().floatValue();
    }

    public float attackManaSchoolMultiplierScale() {
        return attackManaSchoolMultiplierScale.get().floatValue();
    }

    public float meleeGuardManaCost() {
        return meleeGuardManaCost.get().floatValue();
    }

    public float rangedGuardManaCost() {
        return rangedGuardManaCost.get().floatValue();
    }

    public boolean disableManaRecoveryWhileGuarding() {
        return disableManaRecoveryWhileGuarding.get();
    }

    public int perfectGuardTicks() {
        return perfectGuardTicks.get();
    }
}
