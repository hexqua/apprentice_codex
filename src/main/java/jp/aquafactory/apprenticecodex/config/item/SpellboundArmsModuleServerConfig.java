package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellboundArmsModuleServerConfig {
    public static final Values DEFAULTS = new Values(0.5, 0.15, 1.0, 0.25);
    private final ModConfigSpec.DoubleValue firstModuleMultiplier;
    private final ModConfigSpec.DoubleValue additionalModuleMultiplier;
    private final ModConfigSpec.DoubleValue normalManaCostMultiplier;
    private final ModConfigSpec.DoubleValue magicManaCostMultiplier;
    private Values override;

    private SpellboundArmsModuleServerConfig(ModConfigSpec.Builder builder) {
        builder.push("SpellboundArmsModule");
        firstModuleMultiplier = builder.comment("Fraction of the spell power bonus applied by the first module.")
                .defineInRange("firstModuleMultiplier", 0.5, 0.0, Double.MAX_VALUE);
        additionalModuleMultiplier = builder.comment("Additional fraction of the spell power bonus applied per module after the first.")
                .defineInRange("additionalModuleMultiplier", 0.15, 0.0, Double.MAX_VALUE);
        normalManaCostMultiplier = builder.comment("Mana cost per firing, per damage after other modifiers but before this module's spell power bonus.")
                .defineInRange("normalManaCostMultiplier", 1.0, 0.0, Double.MAX_VALUE);
        magicManaCostMultiplier = builder.comment("Mana cost per firing, per additional damage from this module. Set both mana coefficients to 0 to disable its mana cost.")
                .defineInRange("magicManaCostMultiplier", 0.25, 0.0, Double.MAX_VALUE);
        builder.pop();
    }

    public static SpellboundArmsModuleServerConfig define(ModConfigSpec.Builder builder) {
        return new SpellboundArmsModuleServerConfig(builder);
    }

    public Values values() {
        return override != null ? override : new Values(firstModuleMultiplier.get(), additionalModuleMultiplier.get(),
                normalManaCostMultiplier.get(), magicManaCostMultiplier.get());
    }

    public AutoCloseable overrideForGameTest(Values values) {
        var previous = override;
        override = values;
        return () -> override = previous;
    }

    public record Values(double firstModuleMultiplier, double additionalModuleMultiplier,
                         double normalManaCostMultiplier, double magicManaCostMultiplier) {
        public double damageMultiplier(double power, int modules) {
            return modules <= 0 ? 1 : 1 + (power - 1) * (firstModuleMultiplier + (modules - 1) * additionalModuleMultiplier);
        }
    }
}
