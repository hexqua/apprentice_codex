package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellgunCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;

@Mixin(value = GuardSkill.class, remap = false)
public abstract class EpicFightGuardSkillMixin {
    @Inject(method = "canExecute", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$prioritizeValidOffhandSpellgun(
            SkillContainer container,
            CallbackInfoReturnable<Boolean> callback
    ) {
        // 同じ右入力でガードと使用を同時実行せず、有効なオフハンドSpellgunを優先する。
        if (EpicFightSpellgunCompat.shouldPrioritizeOffhandSpellgun(container.getExecutor())) {
            callback.setReturnValue(false);
        }
    }
}
