package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatDualWieldingPolicyCompat;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAttackHelper.class)
public abstract class BetterCombatPlayerAttackHelperMixin {
    @Inject(method = "isDualWielding", at = @At("HEAD"), cancellable = true, remap = false)
    private static void apprenticecodex$suppressPolicyOffhandDualWielding(
            Player player,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (BetterCombatDualWieldingPolicyCompat.shouldSuppressDualWielding(player)) {
            // weapon_attributes はメインハンド運用向けに残しつつ、offhand 補助具運用だけは
            // Better Combat の交互攻撃へ参加させない。1.21.1 側では攻撃手選択の実装差分を再確認する。
            callback.setReturnValue(false);
        }
    }
}
