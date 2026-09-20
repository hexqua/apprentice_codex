package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellReaperScytheCompat;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.BasicAttack;

@Mixin(value = BasicAttack.class, remap = false)
public abstract class EpicFightScytheComboAttacksMixin {
    // COMBO_ATTACKイベントは空中・ダッシュのスタミナ判定前。実際の攻撃開始にだけ帰還を結びつける。
    // 1.20.1 の Animator は汎用 AssetAccessor を受け取る。実際の bytecode の呼び出し先へ接続する。
    @Inject(method = "executeOnServer", at = @At(value = "INVOKE",
            target = "Lyesman/epicfight/api/animation/Animator;playAnimation(Lyesman/epicfight/api/asset/AssetAccessor;F)V"))
    private void apprenticecodex$recallBeforeAcceptedAttack(SkillContainer container, FriendlyByteBuf args, CallbackInfo ci) {
        EpicFightSpellReaperScytheCompat.onAcceptedAttack(container.getServerExecutor());
    }
}
