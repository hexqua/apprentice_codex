package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("RedundantMethodOverride")
public class AlacrityEnchantment extends Enchantment {
    private static final EnchantmentCategory MAGIC_ITEM_CATEGORY =
            EnchantmentCategory.create("apprenticecodex_alacrity_magic", MagicItemEnchantmentTargeting::isSupportedOffhandOrArmorMagicItem);

    public AlacrityEnchantment() {
        super(Rarity.UNCOMMON, MAGIC_ITEM_CATEGORY,
                new EquipmentSlot[]{EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 8;
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
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    @Override
    protected boolean checkCompatibility(@NotNull Enchantment other) {
        return !(other instanceof TenseEnchantment)
                && super.checkCompatibility(other);
    }
}
