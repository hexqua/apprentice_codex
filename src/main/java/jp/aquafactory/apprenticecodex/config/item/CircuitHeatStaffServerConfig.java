package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;

public final class CircuitHeatStaffServerConfig {
    private final ModConfigSpec.IntValue additionalManaReferenceCooldownTicks;
    private final ModConfigSpec.DoubleValue additionalManaLinearMultiplier;
    private final ModConfigSpec.DoubleValue additionalManaQuadraticMultiplier;
    private final ModConfigSpec.IntValue cooldownBypassMaxRemainingTicks;
    private final ModConfigSpec.ConfigValue<List<? extends String>> spellDenylist;
    private final ModConfigSpec.DoubleValue staffOverheatDurationMultiplier;
    private final ModConfigSpec.IntValue staffOverheatDurationMinTicks;
    private final ModConfigSpec.IntValue staffOverheatDurationCapTicks;
    private final ModConfigSpec.BooleanValue dropCoolingEnabled;
    private final ModConfigSpec.IntValue dropCoolingProcessIntervalTicks;
    private final ModConfigSpec.IntValue dropCoolingReductionTicks;
    private final ModConfigSpec.IntValue dropCoolingWaterConsumeProcessCount;
    private final ModConfigSpec.BooleanValue consumeWaterSourceOnCooling;
    private final ModConfigSpec.BooleanValue consumeWaterCauldronOnCooling;
    private Integer additionalManaReferenceCooldownTicksOverride;
    private Double additionalManaLinearMultiplierOverride;
    private Double additionalManaQuadraticMultiplierOverride;
    private Integer cooldownBypassMaxRemainingTicksOverride;
    private List<String> spellDenylistOverride;
    private Double staffOverheatDurationMultiplierOverride;
    private Integer staffOverheatDurationMinTicksOverride;
    private Integer staffOverheatDurationCapTicksOverride;
    private Boolean dropCoolingEnabledOverride;
    private Integer dropCoolingProcessIntervalTicksOverride;
    private Integer dropCoolingReductionTicksOverride;
    private Integer dropCoolingWaterConsumeProcessCountOverride;
    private Boolean consumeWaterSourceOnCoolingOverride;
    private Boolean consumeWaterCauldronOnCoolingOverride;

    private CircuitHeatStaffServerConfig(
            ModConfigSpec.IntValue additionalManaReferenceCooldownTicks,
            ModConfigSpec.DoubleValue additionalManaLinearMultiplier,
            ModConfigSpec.DoubleValue additionalManaQuadraticMultiplier,
            ModConfigSpec.IntValue cooldownBypassMaxRemainingTicks,
            ModConfigSpec.ConfigValue<List<? extends String>> spellDenylist,
            ModConfigSpec.DoubleValue staffOverheatDurationMultiplier,
            ModConfigSpec.IntValue staffOverheatDurationMinTicks,
            ModConfigSpec.IntValue staffOverheatDurationCapTicks,
            ModConfigSpec.BooleanValue dropCoolingEnabled,
            ModConfigSpec.IntValue dropCoolingProcessIntervalTicks,
            ModConfigSpec.IntValue dropCoolingReductionTicks,
            ModConfigSpec.IntValue dropCoolingWaterConsumeProcessCount,
            ModConfigSpec.BooleanValue consumeWaterSourceOnCooling,
            ModConfigSpec.BooleanValue consumeWaterCauldronOnCooling
    ) {
        this.additionalManaReferenceCooldownTicks = additionalManaReferenceCooldownTicks;
        this.additionalManaLinearMultiplier = additionalManaLinearMultiplier;
        this.additionalManaQuadraticMultiplier = additionalManaQuadraticMultiplier;
        this.cooldownBypassMaxRemainingTicks = cooldownBypassMaxRemainingTicks;
        this.spellDenylist = spellDenylist;
        this.staffOverheatDurationMultiplier = staffOverheatDurationMultiplier;
        this.staffOverheatDurationMinTicks = staffOverheatDurationMinTicks;
        this.staffOverheatDurationCapTicks = staffOverheatDurationCapTicks;
        this.dropCoolingEnabled = dropCoolingEnabled;
        this.dropCoolingProcessIntervalTicks = dropCoolingProcessIntervalTicks;
        this.dropCoolingReductionTicks = dropCoolingReductionTicks;
        this.dropCoolingWaterConsumeProcessCount = dropCoolingWaterConsumeProcessCount;
        this.consumeWaterSourceOnCooling = consumeWaterSourceOnCooling;
        this.consumeWaterCauldronOnCooling = consumeWaterCauldronOnCooling;
    }

    public static CircuitHeatStaffServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("CircuitHeatStaff");
        var additionalManaReferenceCooldownTicks = builder
                .comment("Cooldown ticks treated as the reference amount for Circuit Heat Staff extra mana. 200 ticks = 10 seconds.")
                .defineInRange("additionalManaReferenceCooldownTicks", 20 * 10, 1, 72000);
        var additionalManaLinearMultiplier = builder
                .comment("Linear extra mana multiplier per repeated Circuit Heat Staff cooldown bypass step.")
                .defineInRange("additionalManaLinearMultiplier", 0.10D, 0.0D, 100.0D);
        var additionalManaQuadraticMultiplier = builder
                .comment("Quadratic extra mana multiplier per repeated Circuit Heat Staff cooldown bypass step.")
                .defineInRange("additionalManaQuadraticMultiplier", 0.10D, 0.0D, 100.0D);
        var cooldownBypassMaxRemainingTicks = builder
                .comment("Maximum remaining cooldown ticks Circuit Heat Staff may bypass. 0 disables this limit.")
                .defineInRange("cooldownBypassMaxRemainingTicks", 0, 0, 72000);
        var spellDenylist = builder
                .comment("Spell IDs blocked only for Circuit Heat Staff cooldown bypass. Entries use \"modid:path\".")
                .defineListAllowEmpty("spellDenylist", List.<String>of(), CircuitHeatStaffServerConfig::isSpellId);
        var staffOverheatDurationMultiplier = builder
                .comment("Multiplier applied to Circuit Heat Staff item overheat duration after failed mana coverage.")
                .defineInRange("staffOverheatDurationMultiplier", 1.0D, 0.0D, 100.0D);
        var staffOverheatDurationMinTicks = builder
                .comment("Minimum Circuit Heat Staff item overheat duration in ticks. 200 ticks = 10 seconds.")
                .defineInRange("staffOverheatDurationMinTicks", 20 * 10, 0, 72000);
        var staffOverheatDurationCapTicks = builder
                .comment("Maximum Circuit Heat Staff item overheat duration in ticks. 0 disables this cap.")
                .defineInRange("staffOverheatDurationCapTicks", 0, 0, Integer.MAX_VALUE);
        var dropCoolingEnabled = builder
                .comment("Enables Circuit Heat Staff dropped-item cooling in water and powder snow.")
                .define("dropCoolingEnabled", true);
        var dropCoolingProcessIntervalTicks = builder
                .comment("Tick interval between Circuit Heat Staff dropped-item cooling processes.")
                .defineInRange("dropCoolingProcessIntervalTicks", 10, 1, 72000);
        var dropCoolingReductionTicks = builder
                .comment("Overheat ticks reduced per Circuit Heat Staff dropped-item cooling process.")
                .defineInRange("dropCoolingReductionTicks", 20 * 10, 0, 72000);
        var dropCoolingWaterConsumeProcessCount = builder
                .comment("Cooling process count before Circuit Heat Staff consumes water source or water cauldron level.")
                .defineInRange("dropCoolingWaterConsumeProcessCount", 3, 1, 72000);
        var consumeWaterSourceOnCooling = builder
                .comment("Allows Circuit Heat Staff dropped-item cooling to remove water source blocks.")
                .define("consumeWaterSourceOnCooling", true);
        var consumeWaterCauldronOnCooling = builder
                .comment("Allows Circuit Heat Staff dropped-item cooling to lower water cauldron levels.")
                .define("consumeWaterCauldronOnCooling", true);
        builder.pop();

        return new CircuitHeatStaffServerConfig(
                additionalManaReferenceCooldownTicks,
                additionalManaLinearMultiplier,
                additionalManaQuadraticMultiplier,
                cooldownBypassMaxRemainingTicks,
                spellDenylist,
                staffOverheatDurationMultiplier,
                staffOverheatDurationMinTicks,
                staffOverheatDurationCapTicks,
                dropCoolingEnabled,
                dropCoolingProcessIntervalTicks,
                dropCoolingReductionTicks,
                dropCoolingWaterConsumeProcessCount,
                consumeWaterSourceOnCooling,
                consumeWaterCauldronOnCooling
        );
    }

    public int additionalManaReferenceCooldownTicks() {
        return additionalManaReferenceCooldownTicksOverride == null
                ? additionalManaReferenceCooldownTicks.get()
                : additionalManaReferenceCooldownTicksOverride;
    }

    public float additionalManaLinearMultiplier() {
        return (additionalManaLinearMultiplierOverride == null
                ? additionalManaLinearMultiplier.get()
                : additionalManaLinearMultiplierOverride).floatValue();
    }

    public float additionalManaQuadraticMultiplier() {
        return (additionalManaQuadraticMultiplierOverride == null
                ? additionalManaQuadraticMultiplier.get()
                : additionalManaQuadraticMultiplierOverride).floatValue();
    }

    public int cooldownBypassMaxRemainingTicks() {
        return cooldownBypassMaxRemainingTicksOverride == null
                ? cooldownBypassMaxRemainingTicks.get()
                : cooldownBypassMaxRemainingTicksOverride;
    }

    public boolean isSpellDenied(ResourceLocation spellId) {
        if (spellId == null) {
            return false;
        }
        for (var configuredId : spellDenylist()) {
            if (spellId.equals(ResourceLocation.tryParse(String.valueOf(configuredId)))) {
                return true;
            }
        }
        return false;
    }

    public List<String> spellDenylist() {
        return Objects.requireNonNullElseGet(spellDenylistOverride, () -> spellDenylist.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public double staffOverheatDurationMultiplier() {
        return staffOverheatDurationMultiplierOverride == null
                ? staffOverheatDurationMultiplier.get()
                : staffOverheatDurationMultiplierOverride;
    }

    public int staffOverheatDurationMinTicks() {
        return staffOverheatDurationMinTicksOverride == null
                ? staffOverheatDurationMinTicks.get()
                : staffOverheatDurationMinTicksOverride;
    }

    public int staffOverheatDurationCapTicks() {
        return staffOverheatDurationCapTicksOverride == null
                ? staffOverheatDurationCapTicks.get()
                : staffOverheatDurationCapTicksOverride;
    }

    public boolean dropCoolingEnabled() {
        return dropCoolingEnabledOverride == null ? dropCoolingEnabled.get() : dropCoolingEnabledOverride;
    }

    public int dropCoolingProcessIntervalTicks() {
        return dropCoolingProcessIntervalTicksOverride == null
                ? dropCoolingProcessIntervalTicks.get()
                : dropCoolingProcessIntervalTicksOverride;
    }

    public int dropCoolingReductionTicks() {
        return dropCoolingReductionTicksOverride == null ? dropCoolingReductionTicks.get() : dropCoolingReductionTicksOverride;
    }

    public int dropCoolingWaterConsumeProcessCount() {
        return dropCoolingWaterConsumeProcessCountOverride == null
                ? dropCoolingWaterConsumeProcessCount.get()
                : dropCoolingWaterConsumeProcessCountOverride;
    }

    public boolean consumeWaterSourceOnCooling() {
        return consumeWaterSourceOnCoolingOverride == null
                ? consumeWaterSourceOnCooling.get()
                : consumeWaterSourceOnCoolingOverride;
    }

    public boolean consumeWaterCauldronOnCooling() {
        return consumeWaterCauldronOnCoolingOverride == null
                ? consumeWaterCauldronOnCooling.get()
                : consumeWaterCauldronOnCoolingOverride;
    }

    public void setForGameTest(
            int additionalManaReferenceCooldownTicks,
            double additionalManaLinearMultiplier,
            double additionalManaQuadraticMultiplier,
            int cooldownBypassMaxRemainingTicks,
            List<String> spellDenylist,
            double staffOverheatDurationMultiplier,
            int staffOverheatDurationMinTicks,
            int staffOverheatDurationCapTicks,
            boolean dropCoolingEnabled,
            int dropCoolingProcessIntervalTicks,
            int dropCoolingReductionTicks,
            int dropCoolingWaterConsumeProcessCount,
            boolean consumeWaterSourceOnCooling,
            boolean consumeWaterCauldronOnCooling
    ) {
        this.additionalManaReferenceCooldownTicksOverride = additionalManaReferenceCooldownTicks;
        this.additionalManaLinearMultiplierOverride = additionalManaLinearMultiplier;
        this.additionalManaQuadraticMultiplierOverride = additionalManaQuadraticMultiplier;
        this.cooldownBypassMaxRemainingTicksOverride = cooldownBypassMaxRemainingTicks;
        this.spellDenylistOverride = List.copyOf(spellDenylist);
        this.staffOverheatDurationMultiplierOverride = staffOverheatDurationMultiplier;
        this.staffOverheatDurationMinTicksOverride = staffOverheatDurationMinTicks;
        this.staffOverheatDurationCapTicksOverride = staffOverheatDurationCapTicks;
        this.dropCoolingEnabledOverride = dropCoolingEnabled;
        this.dropCoolingProcessIntervalTicksOverride = dropCoolingProcessIntervalTicks;
        this.dropCoolingReductionTicksOverride = dropCoolingReductionTicks;
        this.dropCoolingWaterConsumeProcessCountOverride = dropCoolingWaterConsumeProcessCount;
        this.consumeWaterSourceOnCoolingOverride = consumeWaterSourceOnCooling;
        this.consumeWaterCauldronOnCoolingOverride = consumeWaterCauldronOnCooling;
    }

    private static boolean isSpellId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
