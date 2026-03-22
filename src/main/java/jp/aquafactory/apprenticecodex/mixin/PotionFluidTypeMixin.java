package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.fluids.PotionFluidType;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotion;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectUtil;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionFluidType.class)
public abstract class PotionFluidTypeMixin {
    @Inject(
            method = "getDescription(Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/network/chat/Component;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void apprenticecodex$overrideSchoolAffinityPotionFluidName(
            FluidStack stack,
            CallbackInfoReturnable<Component> cir
    ) {
        var potionStack = PotionFluid.from(stack);
        if (potionStack.isEmpty()) {
            return;
        }

        var potion = PotionContentsHelper.getPotion(potionStack);
        if (!(potion instanceof SchoolAffinityPotion)) {
            return;
        }

        var effects = PotionContentsHelper.getMobEffects(potionStack);
        if (effects.isEmpty()) {
            cir.setReturnValue(potionStack.getHoverName());
            return;
        }

        var primary = effects.get(0);
        var description = potionStack.getHoverName().copy();

        if (primary.getAmplifier() > 0) {
            description = Component.translatable(
                    "potion.withAmplifier",
                    description,
                    Component.translatable("potion.potency." + primary.getAmplifier())
            );
        }

        if (!primary.endsWithin(20)) {
            description = Component.translatable(
                    "potion.withDuration",
                    description,
                    MobEffectUtil.formatDuration(primary, 1.0F, 1.0F)
            );
        }

        cir.setReturnValue(description.withStyle(primary.getEffect().value().getCategory().getTooltipFormatting()));
    }
}
