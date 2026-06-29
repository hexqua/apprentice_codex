package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

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

    @Inject(
            method = "forEachModifier(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V",
            at = @At("RETURN")
    )
    private static void apprenticecodex$addSpellchargedGreatswordSweepingAttribute(
            ItemStack stack,
            EquipmentSlotGroup slotGroup,
            BiConsumer<Holder<Attribute>, AttributeModifier> action,
            CallbackInfo callback
    ) {
        if (slotGroup != EquipmentSlotGroup.MAINHAND) {
            return;
        }

        apprenticecodex$addSpellchargedGreatswordSweepingAttribute(stack, action);
    }

    @Inject(
            method = "forEachModifier(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at = @At("RETURN")
    )
    private static void apprenticecodex$addSpellchargedGreatswordSweepingAttribute(
            ItemStack stack,
            EquipmentSlot slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> action,
            CallbackInfo callback
    ) {
        if (slot != EquipmentSlot.MAINHAND) {
            return;
        }

        apprenticecodex$addSpellchargedGreatswordSweepingAttribute(stack, action);
    }

    private static void apprenticecodex$addSpellchargedGreatswordSweepingAttribute(
            ItemStack stack,
            BiConsumer<Holder<Attribute>, AttributeModifier> action
    ) {
        var bonus = SpellchargedGreatsword.getSweepingDamageRatioModifierBonus(stack);
        if (bonus <= 0.0D) {
            return;
        }

        action.accept(
                Attributes.SWEEPING_DAMAGE_RATIO,
                new AttributeModifier(
                        SpellchargedGreatsword.SWEEPING_DAMAGE_RATIO_MODIFIER_ID,
                        bonus,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );
    }
}
