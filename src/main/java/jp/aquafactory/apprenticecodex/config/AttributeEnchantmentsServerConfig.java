package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;

final class AttributeEnchantmentsServerConfig {
    private final EnumMap<AttributeEnchantmentType, ForgeConfigSpec.DoubleValue> amountsPerLevel;

    private AttributeEnchantmentsServerConfig(
            EnumMap<AttributeEnchantmentType, ForgeConfigSpec.DoubleValue> amountsPerLevel
    ) {
        this.amountsPerLevel = amountsPerLevel;
    }

    static AttributeEnchantmentsServerConfig define(
            ForgeConfigSpec.Builder builder,
            AttributeEnchantmentType[] types
    ) {
        builder.comment("Amount added by each attribute enchantment level.")
                .push("Enchantments");

        var amountsPerLevel =
                new EnumMap<AttributeEnchantmentType, ForgeConfigSpec.DoubleValue>(AttributeEnchantmentType.class);
        for (var type : types) {
            amountsPerLevel.put(
                    type,
                    builder.defineInRange(
                            type.configKey(),
                            type.defaultAmountPerLevel(),
                            0.0D,
                            Double.MAX_VALUE
                    )
            );
        }
        builder.pop();
        return new AttributeEnchantmentsServerConfig(amountsPerLevel);
    }

    double amountPerLevel(AttributeEnchantmentType type) {
        return amountsPerLevel.get(type).get();
    }

    void setAmountPerLevelForGameTest(AttributeEnchantmentType type, double value) {
        amountsPerLevel.get(type).set(value);
    }
}
