package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.stln.magitech.content.network.SpellEndPayLoadHandler;
import net.stln.magitech.content.network.SpellEndPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpellEndPayLoadHandler.class, remap = false)
public abstract class MagitechStaffEndSyncMixin {
    @Inject(method = "handleDataOnMainC2S", at = @At("HEAD"), cancellable = true)
    private static void apprenticecodex$rejectClientStaffEnd(SpellEndPayload payload, IPayloadContext context, CallbackInfo ci) {
        // 終了・消費もserverが保持した開始情報だけから処理する。
        if (payload.wand().filter(MagiEngineersStaffCompat::isStaff).isPresent()) ci.cancel();
    }
}
