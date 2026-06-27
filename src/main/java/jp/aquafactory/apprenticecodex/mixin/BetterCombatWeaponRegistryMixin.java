package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WeaponRegistry.class, remap = false)
public abstract class BetterCombatWeaponRegistryMixin {
    @Inject(
            method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void apprenticecodex$extendSpellchargedGreatswordOverchargeRange(
            ItemStack stack,
            CallbackInfoReturnable<WeaponAttributes> callback
    ) {
        var attributes = callback.getReturnValue();
        if (attributes == null || !SpellchargedGreatsword.isOverchargeActive(stack)) {
            return;
        }

        callback.setReturnValue(new WeaponAttributes(
                attributes.attackRange() + SpellchargedGreatsword.ENTITY_REACH_BONUS,
                attributes.pose(),
                attributes.offHandPose(),
                attributes.two_handed(),
                attributes.category(),
                attributes.attacks()
        ));
    }
}
