package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.jetbrains.annotations.NotNull;

public abstract class ManaShieldCharmExclusiveEnchantment extends Enchantment {
    private static final EnchantmentCategory MANA_SHIELD_CHARM_CATEGORY =
            EnchantmentCategory.create("apprenticecodex_mana_shield_charm",
                    ManaShieldCharmEnchantmentTargeting::isSupportedManaShieldCharm);

    protected ManaShieldCharmExclusiveEnchantment() {
        super(Rarity.VERY_RARE, MANA_SHIELD_CHARM_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
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

    @Override
    protected boolean checkCompatibility(@NotNull Enchantment other) {
        return super.checkCompatibility(other) && !(other instanceof ManaShieldCharmExclusiveEnchantment);
    }
}
