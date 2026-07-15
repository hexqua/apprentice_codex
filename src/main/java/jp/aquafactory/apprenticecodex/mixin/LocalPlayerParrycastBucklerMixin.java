package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerParrycastBucklerMixin {
    @Shadow public Input input;
    @Unique private boolean apprenticecodex$restoreParrycastInput;
    @Unique private float apprenticecodex$parrycastLeftImpulse;
    @Unique private float apprenticecodex$parrycastForwardImpulse;

    @Inject(method = "aiStep", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/tutorial/Tutorial;onInput(Lnet/minecraft/client/player/Input;)V",
            shift = At.Shift.AFTER))
    private void apprenticecodex$captureParrycastInput(CallbackInfo ci) {
        var player = (LocalPlayer) (Object) this;
        apprenticecodex$restoreParrycastInput = player.isUsingItem()
                && player.getUseItem().getItem() instanceof ParrycastBuckler;
        if (apprenticecodex$restoreParrycastInput) {
            apprenticecodex$parrycastLeftImpulse = input.leftImpulse;
            apprenticecodex$parrycastForwardImpulse = input.forwardImpulse;
        }
    }

    @Inject(method = "aiStep", require = 0, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/LocalPlayer;autoJumpTime:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private void apprenticecodex$restoreParrycastInput(CallbackInfo ci) {
        if (!apprenticecodex$restoreParrycastInput) return;
        apprenticecodex$restoreParrycastInput = false;
        // 外部 MOD が同じ減速定数を変更しても、その定数へ干渉せず入力だけを復元する。
        input.leftImpulse = apprenticecodex$parrycastLeftImpulse;
        input.forwardImpulse = apprenticecodex$parrycastForwardImpulse;
    }
}
