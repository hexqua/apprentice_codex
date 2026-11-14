package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.magic.MagicPerformanceHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MagicPerformanceHelper.class, remap = false)
public abstract class MagitechMagicPerformanceMixin {
    @Inject(method = "getOutgoingMagicDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;FFLnet/stln/magitech/feature/element/Element;)F", at = @At("HEAD"), cancellable = true)
    private static void apprenticecodex$power(LivingEntity caster, ItemStack wand, float cost, float damage,
                                              Element element, CallbackInfoReturnable<Float> cir) {
        if (MagiEngineersStaffCompat.isStaff(wand))
            cir.setReturnValue(MagiEngineersStaffCompat.outgoing(caster, damage, element));
    }

    @Inject(method = "getEffectiveSpellPropertyWithDivide", at = @At("HEAD"), cancellable = true)
    private static void apprenticecodex$divide(LivingEntity caster, ItemStack wand, float cost, float value,
                                               Holder<Attribute> attribute, CallbackInfoReturnable<Float> cir) {
        if (MagiEngineersStaffCompat.isStaff(wand))
            cir.setReturnValue((float) (value / MagiEngineersStaffCompat.coefficient(caster, wand, attribute)));
    }

    @Inject(method = "getEffectiveSpellProperty(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;FFLnet/minecraft/core/Holder;)F", at = @At("HEAD"), cancellable = true)
    private static void apprenticecodex$multiply(LivingEntity caster, ItemStack wand, float cost, float value,
                                                 Holder<Attribute> attribute, CallbackInfoReturnable<Float> cir) {
        if (MagiEngineersStaffCompat.isStaff(wand))
            cir.setReturnValue((float) (value * MagiEngineersStaffCompat.coefficient(caster, wand, attribute)));
    }
}
