package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("RedundantMethodOverride")
public class SurgeEnchantment extends Enchantment {
    private static final EnchantmentCategory OFFHAND_MAGIC_CATEGORY =
            EnchantmentCategory.create("apprenticecodex_surge_offhand_magic", SurgeEnchantment::isOffhandMagicItem);

    public SurgeEnchantment() {
        super(Rarity.COMMON, OFFHAND_MAGIC_CATEGORY, new EquipmentSlot[]{EquipmentSlot.OFFHAND});
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
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    @Override
    protected boolean checkCompatibility(@NotNull Enchantment other) {
        return !(other instanceof AttunementEnchantment)
                && !(other instanceof TranscendenceEnchantment)
                && super.checkCompatibility(other);
    }

    private static boolean isOffhandMagicItem(Item item) {
        return item instanceof AbstractOffhandMagicItem;
    }
}
