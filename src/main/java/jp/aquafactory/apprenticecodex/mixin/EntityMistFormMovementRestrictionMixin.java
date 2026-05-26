package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.spell.mistform.MistFormMovementRestrictionHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMistFormMovementRestrictionMixin {
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$ignoreMistFormMovementRestriction(
            BlockState state,
            Vec3 motionMultiplier,
            CallbackInfo ci
    ) {
        if (MistFormMovementRestrictionHelper.ignoresMovementRestriction((Entity) (Object) this, state)) {
            ci.cancel();
        }
    }
}
