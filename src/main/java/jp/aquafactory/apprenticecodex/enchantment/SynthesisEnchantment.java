package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class SynthesisEnchantment extends Enchantment {
    private static final EnchantmentCategory FOCUS_STAFFBOW_CATEGORY =
            EnchantmentCategory.create("apprenticecodex_synthesis_focus_staffbow", MagicItemEnchantmentTargeting::isSupportedSynthesisEnchantingItem);

    public SynthesisEnchantment() {
        super(Rarity.VERY_RARE, FOCUS_STAFFBOW_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 20;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isTreasureOnly() {
        return false;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }
}
