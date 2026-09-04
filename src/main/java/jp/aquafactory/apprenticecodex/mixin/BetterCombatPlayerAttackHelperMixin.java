package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatDualWieldingPolicyCompat;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatSpellReaperScytheCompat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAttackHelper.class)
public abstract class BetterCombatPlayerAttackHelperMixin {
    @WrapOperation(
            method = "getCurrentAttack(Lnet/minecraft/world/entity/player/Player;I)Lnet/bettercombat/api/AttackHand;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/bettercombat/logic/WeaponRegistry;getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;"
            ),
            remap = false
    )
    private static WeaponAttributes apprenticecodex$useSpellReaperScytheNoSweepAttributes(
            ItemStack stack,
            Operation<WeaponAttributes> original,
            Player player,
            int comboCount
    ) {
        var originalAttributes = original.call(stack);
        // ItemStackだけを見るWeaponRegistry層では装備中Curioを判定できないため、Playerを持つ攻撃選択時に差し替える。
        return BetterCombatSpellReaperScytheCompat.resolveAttackAttributes(player, stack, originalAttributes);
    }

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
