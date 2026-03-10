package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("RedundantMethodOverride")
public class ReservoirEnchantment extends Enchantment {
    private static final EnchantmentCategory MAGIC_ITEM_CATEGORY =
            EnchantmentCategory.create("apprenticecodex_reservoir_magic", MagicItemEnchantmentTargeting::isSupportedMagicItem);

    public ReservoirEnchantment() {
        super(Rarity.COMMON, MAGIC_ITEM_CATEGORY, new EquipmentSlot[]{EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 50;
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

    @Override
    protected boolean checkCompatibility(@NotNull Enchantment other) {
        return !(other instanceof RefluxEnchantment)
                && super.checkCompatibility(other);
    }
}
