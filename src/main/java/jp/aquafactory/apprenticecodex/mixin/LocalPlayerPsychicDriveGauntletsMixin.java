package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsCharge;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
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
public abstract class LocalPlayerPsychicDriveGauntletsMixin {
    @Shadow public Input input;
    @Unique private boolean apprenticecodex$gauntletsSlowdown;
    @Unique private float apprenticecodex$gauntletsLeft;
    @Unique private float apprenticecodex$gauntletsForward;

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/tutorial/Tutorial;onInput(Lnet/minecraft/client/player/Input;)V",
            shift = At.Shift.AFTER))
    private void apprenticecodex$captureGauntletsInput(CallbackInfo ci) {
        var player = (LocalPlayer) (Object) this;
        apprenticecodex$gauntletsSlowdown = player.isUsingItem()
                && PsychicDriveGauntletsSelection.isGauntlets(player.getUseItem())
                && !PsychicDriveGauntletsSelection.duplicate(player);
        apprenticecodex$gauntletsLeft = input.leftImpulse;
        apprenticecodex$gauntletsForward = input.forwardImpulse;
    }

    @Inject(method = "aiStep", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/LocalPlayer;autoJumpTime:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private void apprenticecodex$applyGauntletsSlowdown(CallbackInfo ci) {
        if (!apprenticecodex$gauntletsSlowdown) return;
        // バニラの使用中減速との二重適用を避け、他アイテムの減速定数には干渉しない。
        input.leftImpulse = apprenticecodex$gauntletsLeft * PsychicDriveGauntletsCharge.MOVEMENT_FACTOR;
        input.forwardImpulse = apprenticecodex$gauntletsForward * PsychicDriveGauntletsCharge.MOVEMENT_FACTOR;
    }
}
