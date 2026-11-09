package jp.aquafactory.apprenticecodex.gametest.equipment.luminousdevice;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;

public final class ManaPotionTestSupport {
    private ManaPotionTestSupport() {}

    public static ItemStack createInstantManaPotion(Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }
}
