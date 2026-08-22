package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 1.21 の PotionContents 利用箇所を 1.20.1 の PotionUtils へ集約する。 */
public final class PotionContentsHelper {
    private PotionContentsHelper() { }

    public static @Nullable Potion getPotion(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var potion = PotionUtils.getPotion(stack);
        return potion == Potions.EMPTY ? null : potion;
    }

    public static List<MobEffectInstance> getMobEffects(@Nullable ItemStack stack) {
        return stack == null || stack.isEmpty() ? List.of() : PotionUtils.getMobEffects(stack);
    }

    public static void setPotion(ItemStack stack, Potion potion) {
        PotionUtils.setPotion(stack, potion);
    }

    public static ItemStack createPotionStack(Item item, Potion potion) {
        return PotionUtils.setPotion(new ItemStack(item), potion);
    }
}
