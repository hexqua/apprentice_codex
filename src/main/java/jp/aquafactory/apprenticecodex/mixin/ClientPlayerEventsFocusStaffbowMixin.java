package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.player.ClientPlayerEvents;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayerEvents.class, remap = false)
public abstract class ClientPlayerEventsFocusStaffbowMixin {

    @Inject(method = "onCalculatePlayerSpeed", at = @At("HEAD"), cancellable = true)
    private static void apprenticecodex$skipFocusStaffbowCastingMovePenalty(MovementInputUpdateEvent event, CallbackInfo ci) {
        if (!FocusStaffbow.isBowDrawUse(event.getEntity())) {
            return;
        }

        // CONTINUOUS は Iron's の cast state を維持して tick/HUD/cancel を使う。
        // ただし右クリック中の移動低下だけは弓の引き絞り由来に限定したいので、
        // CASTING_MOVESPEED を参照する client 側補正はここで通さない。
        ci.cancel();
    }
}
