package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellReaperScytheCompat;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.common.ComboAttacks;

@Mixin(value = ComboAttacks.class, remap = false)
public abstract class EpicFightScytheComboAttacksMixin {
    // COMBO_ATTACKイベントは空中・ダッシュのスタミナ判定前。実際の攻撃開始にだけ帰還を結びつける。
    @Inject(method = "executeOnServer", at = @At(value = "INVOKE",
            target = "Lyesman/epicfight/api/animation/Animator;playAnimation(Lyesman/epicfight/api/asset/AssetAccessor;F)V"))
    private void apprenticecodex$recallBeforeAcceptedAttack(SkillContainer container, CompoundTag args, CallbackInfo ci) {
        EpicFightSpellReaperScytheCompat.onAcceptedAttack(container.getServerExecutor());
    }
}
