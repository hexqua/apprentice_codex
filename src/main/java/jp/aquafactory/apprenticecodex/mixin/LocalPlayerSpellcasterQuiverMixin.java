package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
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
public abstract class LocalPlayerSpellcasterQuiverMixin {
    @Shadow
    public Input input;

    @Unique
    private boolean apprenticecodex$restoreBowDrawInput;
    @Unique
    private float apprenticecodex$bowDrawLeftImpulse;
    @Unique
    private float apprenticecodex$bowDrawForwardImpulse;

    @Inject(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/tutorial/Tutorial;onInput(Lnet/minecraft/client/player/Input;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void apprenticecodex$captureBowDrawInput(CallbackInfo ci) {
        apprenticecodex$restoreBowDrawInput = false;
        if (!SpellcasterQuiver.shouldIgnoreBowSlowdown((LocalPlayer) (Object) this)) {
            return;
        }

        apprenticecodex$restoreBowDrawInput = true;
        apprenticecodex$bowDrawLeftImpulse = input.leftImpulse;
        apprenticecodex$bowDrawForwardImpulse = input.forwardImpulse;
    }

    @Inject(
            method = "aiStep",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;autoJumpTime:I",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            )
    )
    private void apprenticecodex$restoreBowDrawInput(CallbackInfo ci) {
        if (!apprenticecodex$restoreBowDrawInput) {
            return;
        }

        apprenticecodex$restoreBowDrawInput = false;
        // 外部 MOD も同じ 0.2F 定数を変更するため、定数変更ではなく減速後の入力値だけを戻す。
        input.leftImpulse = apprenticecodex$bowDrawLeftImpulse;
        input.forwardImpulse = apprenticecodex$bowDrawForwardImpulse;
    }
}
