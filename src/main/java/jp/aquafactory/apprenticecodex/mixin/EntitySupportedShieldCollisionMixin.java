package jp.aquafactory.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedShieldPassage;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntitySupportedShieldCollisionMixin {
    // 移動主体側だけを変更し、盾の投射物・攻撃判定を残す。
    @Inject(method = "canCollideWith", at = @At("RETURN"), cancellable = true)
    private void allowSupportedShieldPassage(Entity obstacle, CallbackInfoReturnable<Boolean> cir) {
        //noinspection ConstantValue
        cir.setReturnValue(cir.getReturnValue() && !SupportedShieldPassage.canPass((Entity) (Object) this, obstacle));
    }
}
