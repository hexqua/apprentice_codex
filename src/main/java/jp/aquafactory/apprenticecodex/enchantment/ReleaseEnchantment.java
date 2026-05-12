package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;

public final class ReleaseEnchantment extends Enchantment {
    private static final EnchantmentCategory SMASHCAST_SCEPTER_CATEGORY =
            EnchantmentCategory.create("apprenticecodex_release_smashcast_scepter",
                    MagicItemEnchantmentTargeting::isSupportedSmashcastScepterItem);

    // 1.21.1ではWind Burstで置き換える想定.
    public ReleaseEnchantment() {
        super(Rarity.VERY_RARE, SMASHCAST_SCEPTER_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 15 + (level - 1) * 9;
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
    protected boolean checkCompatibility(@NotNull Enchantment other) {
        return !(other instanceof CompressEnchantment)
                && !(other instanceof DamageEnchantment)
                && other != Enchantments.IMPALING
                && super.checkCompatibility(other);
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
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
