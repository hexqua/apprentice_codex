package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;

final class AttributeEnchantmentsServerConfig {
    private final EnumMap<AttributeEnchantmentType, ModConfigSpec.DoubleValue> amountsPerLevel;

    private AttributeEnchantmentsServerConfig(
            EnumMap<AttributeEnchantmentType, ModConfigSpec.DoubleValue> amountsPerLevel
    ) {
        this.amountsPerLevel = amountsPerLevel;
    }

    static AttributeEnchantmentsServerConfig define(
            ModConfigSpec.Builder builder,
            AttributeEnchantmentType[] types
    ) {
        builder.comment("Amount added by each attribute enchantment level.")
                .push("Enchantments");

        var amountsPerLevel =
                new EnumMap<AttributeEnchantmentType, ModConfigSpec.DoubleValue>(AttributeEnchantmentType.class);
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
