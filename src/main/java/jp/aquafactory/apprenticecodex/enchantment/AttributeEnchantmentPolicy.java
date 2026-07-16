package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.EnumSet;
import java.util.Set;

/**
 * 6種の Attribute エンチャントへ参加するアイテムが、通常付与面だけを宣言するポリシー。
 * 強制付与・投影された効果は、直接付与可否にかかわらず全種を評価する。
 */
public interface AttributeEnchantmentPolicy {
    Set<AttributeEnchantmentType> ALL_ATTRIBUTE_ENCHANTMENTS =
            Set.copyOf(EnumSet.allOf(AttributeEnchantmentType.class));

    default Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return Set.of();
    }

    default boolean supportsDirectAttributeEnchantment(AttributeEnchantmentType type) {
        return directlyApplicableAttributeEnchantments().contains(type);
    }

    static boolean supportsDirectApplication(Item item, AttributeEnchantmentType type) {
        return item instanceof AttributeEnchantmentPolicy policy
                && policy.supportsDirectAttributeEnchantment(type);
    }

    static boolean supportsDirectApplication(Item item, Enchantment enchantment) {
        return AttributeEnchantmentType.from(enchantment)
                .map(type -> supportsDirectApplication(item, type))
                .orElse(false);
    }
}
