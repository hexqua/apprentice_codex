package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;

public final class CompressEnchantment extends Enchantment {
    private static final EnchantmentCategory SMASHCAST_SCEPTER_CATEGORY =
            EnchantmentCategory.create("apprenticecodex_compress_smashcast_scepter",
                    MagicItemEnchantmentTargeting::isSupportedSmashcastScepterItem);

    // 1.21.1ではDensityで置き換える想定.
    public CompressEnchantment() {
        super(Rarity.RARE, SMASHCAST_SCEPTER_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
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
    protected boolean checkCompatibility(@NotNull Enchantment other) {
        return !(other instanceof ReleaseEnchantment)
                && !(other instanceof DamageEnchantment)
                && other != Enchantments.IMPALING
                && super.checkCompatibility(other);
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
