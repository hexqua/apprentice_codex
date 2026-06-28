package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpellchargedGreatswordServerConfig {
    private final ForgeConfigSpec.DoubleValue chargeLevel1AttackDamageBonus;
    private final ForgeConfigSpec.DoubleValue chargeLevel1AttackSpeedBonus;
    private final ForgeConfigSpec.DoubleValue chargeLevel2AttackDamageBonus;
    private final ForgeConfigSpec.DoubleValue chargeLevel2AttackSpeedBonus;
    private final ForgeConfigSpec.DoubleValue chargeLevel3AttackDamageBonus;
    private final ForgeConfigSpec.DoubleValue chargeLevel3AttackSpeedBonus;
    private final ForgeConfigSpec.DoubleValue overchargeAttackDamageBonus;
    private final ForgeConfigSpec.DoubleValue overchargeAttackSpeedBonus;
    private final ForgeConfigSpec.DoubleValue normalEntityReachBonus;
    private final ForgeConfigSpec.DoubleValue overchargeEntityReachBonus;
    private final ForgeConfigSpec.IntValue normalSweepingEdgeLevelBonus;
    private final ForgeConfigSpec.IntValue overchargeSweepingEdgeLevelBonus;

    private Values overrideValues;

    private SpellchargedGreatswordServerConfig(
            ForgeConfigSpec.DoubleValue chargeLevel1AttackDamageBonus,
            ForgeConfigSpec.DoubleValue chargeLevel1AttackSpeedBonus,
            ForgeConfigSpec.DoubleValue chargeLevel2AttackDamageBonus,
            ForgeConfigSpec.DoubleValue chargeLevel2AttackSpeedBonus,
            ForgeConfigSpec.DoubleValue chargeLevel3AttackDamageBonus,
            ForgeConfigSpec.DoubleValue chargeLevel3AttackSpeedBonus,
            ForgeConfigSpec.DoubleValue overchargeAttackDamageBonus,
            ForgeConfigSpec.DoubleValue overchargeAttackSpeedBonus,
            ForgeConfigSpec.DoubleValue normalEntityReachBonus,
            ForgeConfigSpec.DoubleValue overchargeEntityReachBonus,
            ForgeConfigSpec.IntValue normalSweepingEdgeLevelBonus,
            ForgeConfigSpec.IntValue overchargeSweepingEdgeLevelBonus
    ) {
        this.chargeLevel1AttackDamageBonus = chargeLevel1AttackDamageBonus;
        this.chargeLevel1AttackSpeedBonus = chargeLevel1AttackSpeedBonus;
        this.chargeLevel2AttackDamageBonus = chargeLevel2AttackDamageBonus;
        this.chargeLevel2AttackSpeedBonus = chargeLevel2AttackSpeedBonus;
        this.chargeLevel3AttackDamageBonus = chargeLevel3AttackDamageBonus;
        this.chargeLevel3AttackSpeedBonus = chargeLevel3AttackSpeedBonus;
        this.overchargeAttackDamageBonus = overchargeAttackDamageBonus;
        this.overchargeAttackSpeedBonus = overchargeAttackSpeedBonus;
        this.normalEntityReachBonus = normalEntityReachBonus;
        this.overchargeEntityReachBonus = overchargeEntityReachBonus;
        this.normalSweepingEdgeLevelBonus = normalSweepingEdgeLevelBonus;
        this.overchargeSweepingEdgeLevelBonus = overchargeSweepingEdgeLevelBonus;
    }

    public static SpellchargedGreatswordServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("SpellchargedGreatsword");
        var chargeLevel1AttackDamageBonus = defineBonus(builder, "chargeLevel1AttackDamageBonus", 2.0D);
        var chargeLevel1AttackSpeedBonus = defineBonus(builder, "chargeLevel1AttackSpeedBonus", -0.0D);
        var chargeLevel2AttackDamageBonus = defineBonus(builder, "chargeLevel2AttackDamageBonus", 4.0D);
        var chargeLevel2AttackSpeedBonus = defineBonus(builder, "chargeLevel2AttackSpeedBonus", -0.2D);
        var chargeLevel3AttackDamageBonus = defineBonus(builder, "chargeLevel3AttackDamageBonus", 8.0D);
        var chargeLevel3AttackSpeedBonus = defineBonus(builder, "chargeLevel3AttackSpeedBonus", -0.5D);
        var overchargeAttackDamageBonus = defineBonus(builder, "overchargeAttackDamageBonus", 0.0D);
        var overchargeAttackSpeedBonus = defineBonus(builder, "overchargeAttackSpeedBonus", 0.2D);
        var normalEntityReachBonus = defineReachBonus(builder, "normalEntityReachBonus", 0.5D);
        var overchargeEntityReachBonus = defineReachBonus(builder, "overchargeEntityReachBonus", 1.0D);
        var normalSweepingEdgeLevelBonus = defineSweepingEdgeLevelBonus(builder, "normalSweepingEdgeLevelBonus", 1);
        var overchargeSweepingEdgeLevelBonus = defineSweepingEdgeLevelBonus(
                builder,
                "overchargeSweepingEdgeLevelBonus",
                3
        );
        builder.pop();

        return new SpellchargedGreatswordServerConfig(
                chargeLevel1AttackDamageBonus,
                chargeLevel1AttackSpeedBonus,
                chargeLevel2AttackDamageBonus,
                chargeLevel2AttackSpeedBonus,
                chargeLevel3AttackDamageBonus,
                chargeLevel3AttackSpeedBonus,
                overchargeAttackDamageBonus,
                overchargeAttackSpeedBonus,
                normalEntityReachBonus,
                overchargeEntityReachBonus,
                normalSweepingEdgeLevelBonus,
                overchargeSweepingEdgeLevelBonus
        );
    }

    public Values values() {
        if (overrideValues != null) {
            return overrideValues;
        }
        return new Values(
                chargeLevel1AttackDamageBonus.get(),
                chargeLevel1AttackSpeedBonus.get(),
                chargeLevel2AttackDamageBonus.get(),
                chargeLevel2AttackSpeedBonus.get(),
                chargeLevel3AttackDamageBonus.get(),
                chargeLevel3AttackSpeedBonus.get(),
                overchargeAttackDamageBonus.get(),
                overchargeAttackSpeedBonus.get(),
                normalEntityReachBonus.get(),
                overchargeEntityReachBonus.get(),
                normalSweepingEdgeLevelBonus.get(),
                overchargeSweepingEdgeLevelBonus.get()
        );
    }

    public void setForGameTest(Values values) {
        this.overrideValues = values;
    }

    private static ForgeConfigSpec.DoubleValue defineBonus(
            ForgeConfigSpec.Builder builder,
            String name,
            double defaultValue
    ) {
        return builder.defineInRange(name, defaultValue, -100.0D, 1000.0D);
    }

    private static ForgeConfigSpec.DoubleValue defineReachBonus(
            ForgeConfigSpec.Builder builder,
            String name,
            double defaultValue
    ) {
        return builder.defineInRange(name, defaultValue, 0.0D, 100.0D);
    }

    private static ForgeConfigSpec.IntValue defineSweepingEdgeLevelBonus(
            ForgeConfigSpec.Builder builder,
            String name,
            int defaultValue
    ) {
        return builder.defineInRange(name, defaultValue, 0, 100);
    }

    public record Values(
            double chargeLevel1AttackDamageBonus,
            double chargeLevel1AttackSpeedBonus,
            double chargeLevel2AttackDamageBonus,
            double chargeLevel2AttackSpeedBonus,
            double chargeLevel3AttackDamageBonus,
            double chargeLevel3AttackSpeedBonus,
            double overchargeAttackDamageBonus,
            double overchargeAttackSpeedBonus,
            double normalEntityReachBonus,
            double overchargeEntityReachBonus,
            int normalSweepingEdgeLevelBonus,
            int overchargeSweepingEdgeLevelBonus
    ) {
    }
}
