package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedShieldPassage;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntitySupportedShieldCollisionMixin {
    // 移動主体側だけを変更し、盾の投射物・攻撃判定を残す。
    @ModifyReturnValue(method = "canCollideWith", at = @At("RETURN"))
    private boolean allowSupportedShieldPassage(boolean original, Entity obstacle) {
        //noinspection ConstantValue
        return original && !SupportedShieldPassage.canPass((Entity) (Object) this, obstacle);
    }
}
