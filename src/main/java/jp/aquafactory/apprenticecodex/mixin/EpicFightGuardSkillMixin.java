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
    private void apprenticecodex$disableGuardForValidOffhandSpellgun(
            SkillContainer container,
            CallbackInfoReturnable<Boolean> callback
    ) {
        // 遠距離攻撃手段をオフハンドに維持したままガードも併用できないよう、
        // 入力割り当てにかかわらず、使用可能なオフハンドSpellgun装備中は意図的にガードを無効化する。
        if (EpicFightSpellgunCompat.isGuardDisabledByOffhandSpellgun(container.getExecutor())) {
            callback.setReturnValue(false);
        }
    }
}
