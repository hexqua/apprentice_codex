package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.irons_artifice.gun.ShotProfile;
import jp.aquafactory.apprenticecodex.event.client.ManaStabilizerClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.redspace.irons_artifice.client.RecoilManager", remap = false)
public abstract class ManaStabilizerRecoilMixin {
    @Shadow private static float pitch;
    @Shadow private static float yaw;

    // 通常減衰後・表示補間前にだけ介入し、永続的な視線変更は巻き戻さない。
    // 呼出しを包むことでForge/NeoForgeのtick event型をhandlerへ持ち込まず、他のwrapperも維持する。
    @WrapOperation(method = "onClientTick", at = @At(value = "INVOKE", target = "Lio/redspace/irons_artifice/client/RecoilManager;updateCameraCursor()V"), require = 0)
    private static void stabilize(Operation<Void> original) {
        var result = ManaStabilizerClientEvents.recover(pitch, yaw);
        pitch = result.x;
        yaw = result.y;
        original.call();
    }

    @Inject(method = "applyRecoil", at = @At("RETURN"), require = 0)
    private static void confirmShot(ShotProfile shotProfile, CallbackInfo ci) {
        ManaStabilizerClientEvents.fired();
    }
}
