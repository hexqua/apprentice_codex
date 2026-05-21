package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MulticastEchoStaffServerConfig {
    private final ForgeConfigSpec.IntValue multicastDelayTicks;
    private final ForgeConfigSpec.DoubleValue cooldownMultiplier;
    private final ForgeConfigSpec.DoubleValue castTimeCooldownMultiplier;
    private final ForgeConfigSpec.IntValue cooldownCapTicks;
    private final ForgeConfigSpec.IntValue maxMulticastCount;
    private final ForgeConfigSpec.BooleanValue mobEffectProfilesEnabled;
    private final ForgeConfigSpec.BooleanValue beneficialMobEffectsEnabled;
    private final ForgeConfigSpec.BooleanValue harmfulMobEffectsEnabled;
    private final ForgeConfigSpec.BooleanValue neutralMobEffectsEnabled;
    private final ForgeConfigSpec.BooleanValue durationServerCapEnabled;
    private final ForgeConfigSpec.IntValue durationServerCapTicks;
    private final ForgeConfigSpec.BooleanValue amplifierServerCapEnabled;
    private final ForgeConfigSpec.IntValue amplifierServerCap;
    private final ForgeConfigSpec.BooleanValue attackProfilesEnabled;
    private final ForgeConfigSpec.DoubleValue repeatDamageMultiplier;
    private Integer multicastDelayTicksOverride;
    private Double cooldownMultiplierOverride;
    private Double castTimeCooldownMultiplierOverride;
    private Integer cooldownCapTicksOverride;
    private Integer maxMulticastCountOverride;
    private Boolean mobEffectProfilesEnabledOverride;
    private Boolean beneficialMobEffectsEnabledOverride;
    private Boolean harmfulMobEffectsEnabledOverride;
    private Boolean neutralMobEffectsEnabledOverride;
    private Boolean durationServerCapEnabledOverride;
    private Integer durationServerCapTicksOverride;
    private Boolean amplifierServerCapEnabledOverride;
    private Integer amplifierServerCapOverride;
    private Boolean attackProfilesEnabledOverride;
    private Double repeatDamageMultiplierOverride;

    private MulticastEchoStaffServerConfig(
            ForgeConfigSpec.IntValue multicastDelayTicks,
            ForgeConfigSpec.DoubleValue cooldownMultiplier,
            ForgeConfigSpec.DoubleValue castTimeCooldownMultiplier,
            ForgeConfigSpec.IntValue cooldownCapTicks,
            ForgeConfigSpec.IntValue maxMulticastCount,
            ForgeConfigSpec.BooleanValue mobEffectProfilesEnabled,
            ForgeConfigSpec.BooleanValue beneficialMobEffectsEnabled,
            ForgeConfigSpec.BooleanValue harmfulMobEffectsEnabled,
            ForgeConfigSpec.BooleanValue neutralMobEffectsEnabled,
            ForgeConfigSpec.BooleanValue durationServerCapEnabled,
            ForgeConfigSpec.IntValue durationServerCapTicks,
            ForgeConfigSpec.BooleanValue amplifierServerCapEnabled,
            ForgeConfigSpec.IntValue amplifierServerCap,
            ForgeConfigSpec.BooleanValue attackProfilesEnabled,
            ForgeConfigSpec.DoubleValue repeatDamageMultiplier
    ) {
        this.multicastDelayTicks = multicastDelayTicks;
        this.cooldownMultiplier = cooldownMultiplier;
        this.castTimeCooldownMultiplier = castTimeCooldownMultiplier;
        this.cooldownCapTicks = cooldownCapTicks;
        this.maxMulticastCount = maxMulticastCount;
        this.mobEffectProfilesEnabled = mobEffectProfilesEnabled;
        this.beneficialMobEffectsEnabled = beneficialMobEffectsEnabled;
        this.harmfulMobEffectsEnabled = harmfulMobEffectsEnabled;
        this.neutralMobEffectsEnabled = neutralMobEffectsEnabled;
        this.durationServerCapEnabled = durationServerCapEnabled;
        this.durationServerCapTicks = durationServerCapTicks;
        this.amplifierServerCapEnabled = amplifierServerCapEnabled;
        this.amplifierServerCap = amplifierServerCap;
        this.attackProfilesEnabled = attackProfilesEnabled;
        this.repeatDamageMultiplier = repeatDamageMultiplier;
    }

    public static MulticastEchoStaffServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("MulticastEchoStaff");
        var multicastDelayTicks = builder
                .comment("Delay in ticks between Multicast Echo Staff repeated casts. Must be at least 1 tick.")
                .defineInRange("multicastDelayTicks", 2, 1, 20);
        var cooldownMultiplier = builder
                .comment("Cooldown multiplier applied per EchoSpell stack when Multicast Echo Staff finishes.")
                .defineInRange("cooldownMultiplier", 1.2D, 0.0D, 100.0D);
        var castTimeCooldownMultiplier = builder
                .comment("Cooldown ticks added from skipped cast time per repeated Multicast Echo Staff cast.")
                .defineInRange("castTimeCooldownMultiplier", 1.0D, 0.0D, 100.0D);
        var cooldownCapTicks = builder
                .comment("Maximum adjusted Multicast Echo Staff cooldown in ticks. Original cooldowns above this value are preserved.")
                .defineInRange("cooldownCapTicks", 12000, 1, 72000);
        var maxMulticastCount = builder
                .comment("Maximum number of repeated casts stored by Echo Cast for Multicast Echo Staff.")
                .defineInRange("maxMulticastCount", 10, 1, 20);
        var mobEffectProfilesEnabled = builder
                .comment("Enables Multicast Echo Staff mob effect profile handling for repeated casts.")
                .define("mobEffectProfilesEnabled", true);
        var beneficialMobEffectsEnabled = builder
                .comment("Allows Multicast Echo Staff mob effect profile handling for beneficial effects.")
                .define("beneficialMobEffectsEnabled", true);
        var harmfulMobEffectsEnabled = builder
                .comment("Allows Multicast Echo Staff mob effect profile handling for harmful effects.")
                .define("harmfulMobEffectsEnabled", true);
        var neutralMobEffectsEnabled = builder
                .comment("Allows Multicast Echo Staff mob effect profile handling for neutral effects.")
                .define("neutralMobEffectsEnabled", true);
        var durationServerCapEnabled = builder
                .comment("Enables an additional server-side duration cap for Multicast Echo Staff mob effect profile handling.")
                .define("durationServerCapEnabled", false);
        var durationServerCapTicks = builder
                .comment("Additional server-side duration cap in ticks. 0 disables this cap even when enabled.")
                .defineInRange("durationServerCapTicks", 6000, 0, 72000);
        var amplifierServerCapEnabled = builder
                .comment("Enables an additional server-side amplifier cap for Multicast Echo Staff mob effect profile handling.")
                .define("amplifierServerCapEnabled", false);
        var amplifierServerCap = builder
                .comment("Additional server-side amplifier cap. 0 disables this cap even when enabled.")
                .defineInRange("amplifierServerCap", 10, 0, 255);
        var attackProfilesEnabled = builder
                .comment("Enables Multicast Echo Staff attack profile handling for repeated casts.")
                .define("attackProfilesEnabled", true);
        var repeatDamageMultiplier = builder
                .comment("Server-wide multiplier applied to repeated Multicast Echo Staff attack profile damage.")
                .defineInRange("repeatDamageMultiplier", 1.0D, 0.0D, 100.0D);
        builder.pop();

        return new MulticastEchoStaffServerConfig(
                multicastDelayTicks,
                cooldownMultiplier,
                castTimeCooldownMultiplier,
                cooldownCapTicks,
                maxMulticastCount,
                mobEffectProfilesEnabled,
                beneficialMobEffectsEnabled,
                harmfulMobEffectsEnabled,
                neutralMobEffectsEnabled,
                durationServerCapEnabled,
                durationServerCapTicks,
                amplifierServerCapEnabled,
                amplifierServerCap,
                attackProfilesEnabled,
                repeatDamageMultiplier
        );
    }

    public int multicastDelayTicks() {
        return multicastDelayTicksOverride == null ? multicastDelayTicks.get() : multicastDelayTicksOverride;
    }

    public double cooldownMultiplier() {
        return cooldownMultiplierOverride == null ? cooldownMultiplier.get() : cooldownMultiplierOverride;
    }

    public double castTimeCooldownMultiplier() {
        return castTimeCooldownMultiplierOverride == null
                ? castTimeCooldownMultiplier.get()
                : castTimeCooldownMultiplierOverride;
    }

    public int cooldownCapTicks() {
        return cooldownCapTicksOverride == null ? cooldownCapTicks.get() : cooldownCapTicksOverride;
    }

    public int maxMulticastCount() {
        return maxMulticastCountOverride == null ? maxMulticastCount.get() : maxMulticastCountOverride;
    }

    public boolean mobEffectProfilesEnabled() {
        return mobEffectProfilesEnabledOverride == null
                ? mobEffectProfilesEnabled.get()
                : mobEffectProfilesEnabledOverride;
    }

    public boolean beneficialMobEffectsEnabled() {
        return beneficialMobEffectsEnabledOverride == null
                ? beneficialMobEffectsEnabled.get()
                : beneficialMobEffectsEnabledOverride;
    }

    public boolean harmfulMobEffectsEnabled() {
        return harmfulMobEffectsEnabledOverride == null
                ? harmfulMobEffectsEnabled.get()
                : harmfulMobEffectsEnabledOverride;
    }

    public boolean neutralMobEffectsEnabled() {
        return neutralMobEffectsEnabledOverride == null
                ? neutralMobEffectsEnabled.get()
                : neutralMobEffectsEnabledOverride;
    }

    public boolean durationServerCapEnabled() {
        return durationServerCapEnabledOverride == null
                ? durationServerCapEnabled.get()
                : durationServerCapEnabledOverride;
    }

    public int durationServerCapTicks() {
        return durationServerCapTicksOverride == null ? durationServerCapTicks.get() : durationServerCapTicksOverride;
    }

    public boolean amplifierServerCapEnabled() {
        return amplifierServerCapEnabledOverride == null
                ? amplifierServerCapEnabled.get()
                : amplifierServerCapEnabledOverride;
    }

    public int amplifierServerCap() {
        return amplifierServerCapOverride == null ? amplifierServerCap.get() : amplifierServerCapOverride;
    }

    public boolean attackProfilesEnabled() {
        return attackProfilesEnabledOverride == null
                ? attackProfilesEnabled.get()
                : attackProfilesEnabledOverride;
    }

    public double repeatDamageMultiplier() {
        return repeatDamageMultiplierOverride == null
                ? repeatDamageMultiplier.get()
                : repeatDamageMultiplierOverride;
    }

    public void setOverridesForGameTest(
            int multicastDelayTicks,
            double cooldownMultiplier,
            double castTimeCooldownMultiplier,
            int cooldownCapTicks,
            int maxMulticastCount
    ) {
        this.multicastDelayTicksOverride = multicastDelayTicks;
        this.cooldownMultiplierOverride = cooldownMultiplier;
        this.castTimeCooldownMultiplierOverride = castTimeCooldownMultiplier;
        this.cooldownCapTicksOverride = cooldownCapTicks;
        this.maxMulticastCountOverride = maxMulticastCount;
    }

    public void setMobEffectOverridesForGameTest(
            boolean mobEffectProfilesEnabled,
            boolean beneficialMobEffectsEnabled,
            boolean harmfulMobEffectsEnabled,
            boolean neutralMobEffectsEnabled,
            boolean durationServerCapEnabled,
            int durationServerCapTicks,
            boolean amplifierServerCapEnabled,
            int amplifierServerCap
    ) {
        this.mobEffectProfilesEnabledOverride = mobEffectProfilesEnabled;
        this.beneficialMobEffectsEnabledOverride = beneficialMobEffectsEnabled;
        this.harmfulMobEffectsEnabledOverride = harmfulMobEffectsEnabled;
        this.neutralMobEffectsEnabledOverride = neutralMobEffectsEnabled;
        this.durationServerCapEnabledOverride = durationServerCapEnabled;
        this.durationServerCapTicksOverride = durationServerCapTicks;
        this.amplifierServerCapEnabledOverride = amplifierServerCapEnabled;
        this.amplifierServerCapOverride = amplifierServerCap;
    }

    public void setAttackOverridesForGameTest(
            boolean attackProfilesEnabled,
            double repeatDamageMultiplier
    ) {
        this.attackProfilesEnabledOverride = attackProfilesEnabled;
        this.repeatDamageMultiplierOverride = repeatDamageMultiplier;
    }
}
