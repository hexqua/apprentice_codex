package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.effect.MistFormEffect;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.spell.longstride.LongStrideFluidMovement;
import jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMistFormMixin {
    @Inject(method = "getJumpBoostPower", at = @At("RETURN"), cancellable = true)
    private void apprenticecodex$addMistFormJumpPower(CallbackInfoReturnable<Float> cir) {
        // mixinはIDEが型を誤認するケースがある.
        //noinspection ConstantValue
        if ((Object) this instanceof LivingEntity livingEntity && livingEntity.hasEffect(EffectRegistry.MIST_FORM)) {
            cir.setReturnValue(cir.getReturnValue() + MistFormEffect.JUMP_POWER_ADDITION);
        }
    }

    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$standOnFluidInMistForm(FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if (fluidState.isEmpty()) {
            return;
        }

        // mixinはIDEが型を誤認するケースがある.
        //noinspection ConstantValue
        if ((Object) this instanceof Player player
                && (MistFormEvents.canStandOnFluid(player)
                || LongStrideFluidMovement.canStandOnFluid(player, fluidState))) {
            cir.setReturnValue(true);
        }
    }
}
