package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@Pseudo
@Mixin(targets = "com.yukami.efiscompat.player.PlayerAnimationEvents", remap = false)
public abstract class EfisCompatPlayerAnimationEventsMixin {
    @Inject(method = "beforeSpellCast", at = @At("HEAD"), cancellable = true, require = 0)
    private static void apprenticecodex$skipSwingcastActionGate(SpellPreCastEvent event, CallbackInfo callback) {
        var player = event.getEntity();
        if (player == null) {
            return;
        }

        // Swingcast は Epic Fight の攻撃判定から詠唱を開始するため、efiscompat の攻撃直後キャンセルだけを通過させる。
        // スタン中の詠唱キャンセルは efiscompat 側の制御を維持する。
        if (SwingcastStaffCastContext.matches(player.getUUID(), event.getSpellId()) && !apprenticecodex$isStunned(player)) {
            callback.cancel();
        }
    }

    @Unique
    private static boolean apprenticecodex$isStunned(net.minecraft.world.entity.player.Player player) {
        return EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class)
                .map(ServerPlayerPatch::isStunned)
                .orElse(false);
    }
}
