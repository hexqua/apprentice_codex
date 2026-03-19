package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class GuzzleEnchantment extends Enchantment {
    private static final EnchantmentCategory FLASK_CATEGORY =
            EnchantmentCategory.create("apprenticecodex_guzzle_flask", FlaskEnchantmentTargeting::isSupportedFlaskItem);

    public GuzzleEnchantment() {
        super(Rarity.UNCOMMON, FLASK_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 11;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 5;
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
