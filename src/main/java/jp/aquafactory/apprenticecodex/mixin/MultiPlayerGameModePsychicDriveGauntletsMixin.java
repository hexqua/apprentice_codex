package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsUseInput;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModePsychicDriveGauntletsMixin {
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$blockRepeatedGauntletsUse(Player player, InteractionHand hand,
                                                          CallbackInfoReturnable<InteractionResult> cir) {
        if (PsychicDriveGauntletsUseInput.blockRepeatedUse(player, hand)) cir.setReturnValue(InteractionResult.CONSUME);
    }
}
