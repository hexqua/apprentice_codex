package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("RedundantMethodOverride")
public class TranscendenceEnchantment extends Enchantment {
    private static final EnchantmentCategory MAGIC_ITEM_CATEGORY =
            EnchantmentCategory.create("apprenticecodex_transcendence_magic", TranscendencePolicy::supportsDirectApplication);

    public TranscendenceEnchantment() {
        super(Rarity.VERY_RARE, MAGIC_ITEM_CATEGORY,
                new EquipmentSlot[]{
                        EquipmentSlot.MAINHAND,
                        EquipmentSlot.OFFHAND,
                        EquipmentSlot.HEAD,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.FEET
                });
    }

    @Override
    public int getMinCost(int level) {
        return 25 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
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
        return !(other instanceof SurgeEnchantment)
                && !(other instanceof AttunementEnchantment)
                && super.checkCompatibility(other);
    }
}
