package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceEvents;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Utils.class, remap = false)
public abstract class UtilsMixin {
    @Inject(method = "serverSideInitiateCast", at = @At("HEAD"), cancellable = true)
    private static void apprentice_codex$rejectMirageAvoidanceCastInput(
            ServerPlayer serverPlayer,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (serverPlayer != null && MirageAvoidanceEvents.rejectServerInputCastIfLocked(serverPlayer)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "serverSideInitiateQuickCast", at = @At("HEAD"), cancellable = true)
    private static void apprentice_codex$rejectMirageAvoidanceQuickCastInput(
            ServerPlayer serverPlayer,
            int slot,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (serverPlayer != null && MirageAvoidanceEvents.rejectServerInputCastIfLocked(serverPlayer)) {
            cir.setReturnValue(false);
        }
    }
}
