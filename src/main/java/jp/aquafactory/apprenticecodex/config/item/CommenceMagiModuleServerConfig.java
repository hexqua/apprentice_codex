package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CommenceMagiModuleServerConfig {
    public static final Values DEFAULTS = new Values(100, 25, 30);
    private final ModConfigSpec.IntValue initialBonus;
    private final ModConfigSpec.IntValue additionalBonus;
    private final ModConfigSpec.IntValue manaCost;
    private Values override;

    private CommenceMagiModuleServerConfig(ModConfigSpec.Builder builder) {
        builder.push("CommenceMagiModule");
        initialBonus = builder.comment("Additional headshot damage percentage provided by the first module.")
                .defineInRange("initialHeadshotBonusPercent", 100, 0, Integer.MAX_VALUE);
        additionalBonus = builder.comment("Additional headshot damage percentage per module after the first.")
                .defineInRange("additionalHeadshotBonusPercent", 25, 0, Integer.MAX_VALUE);
        manaCost = builder.comment("Mana consumed per gunshot, regardless of projectile or module count. 0 disables mana consumption.")
                .defineInRange("manaCost", 30, 0, Integer.MAX_VALUE);
        builder.pop();
    }

    public static CommenceMagiModuleServerConfig define(ModConfigSpec.Builder builder) {
        return new CommenceMagiModuleServerConfig(builder);
    }

    public Values values() {
        return override != null ? override : new Values(initialBonus.get(), additionalBonus.get(), manaCost.get());
    }

    public AutoCloseable overrideForGameTest(Values values) {
        var previous = override;
        override = values;
        return () -> override = previous;
    }

    public record Values(int initialHeadshotBonusPercent, int additionalHeadshotBonusPercent, int manaCost) {
        public double headshotMultiplier(int modules) {
            return modules <= 0 ? 1 : 1 + (initialHeadshotBonusPercent + (double) additionalHeadshotBonusPercent * (modules - 1)) / 100;
        }
    }
}
