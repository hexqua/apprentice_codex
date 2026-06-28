package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(
            method = "getItemEnchantmentLevel",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void apprenticecodex$boostSpellchargedGreatswordSweepingEdge(
            Holder<Enchantment> enchantment,
            ItemStack stack,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (!enchantment.is(Enchantments.SWEEPING_EDGE)) {
            return;
        }

        var bonusLevel = SpellchargedGreatsword.getSweepingEdgeLevelBonus(stack);
        if (bonusLevel <= 0) {
            return;
        }

        callback.setReturnValue(callback.getReturnValueI() + bonusLevel);
    }
}
