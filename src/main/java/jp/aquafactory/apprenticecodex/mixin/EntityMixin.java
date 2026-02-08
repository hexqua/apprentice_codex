package jp.aquafactory.apprenticecodex.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static jp.aquafactory.apprenticecodex.client.ClientCastTargetHighlightHandler.getHighlightColor;
import static jp.aquafactory.apprenticecodex.client.ClientCastTargetHighlightHandler.getHighlightEntityId;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract int getId();

    // 発光エフェクトはチームカラー依存なので差し込めるようにする.
    @Inject(method = "getTeamColor", at = @At(value = "HEAD"), cancellable = true)
    public void changeGlowOutline(CallbackInfoReturnable<Integer> cir) {
        if (getId() == getHighlightEntityId()) {
            cir.setReturnValue(getHighlightColor());
        }
    }
}
