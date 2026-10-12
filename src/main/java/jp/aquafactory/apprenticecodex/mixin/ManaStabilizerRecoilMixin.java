package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.irons_artifice.gun.ShotProfile;
import jp.aquafactory.apprenticecodex.event.client.ManaStabilizerClientEvents;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
    @Inject(method = "onClientTick", at = @At(value = "INVOKE", target = "Lio/redspace/irons_artifice/client/RecoilManager;updateCameraCursor()V"), require = 0)
    private static void stabilize(ClientTickEvent.Post event, CallbackInfo ci) {
        var result = ManaStabilizerClientEvents.recover(pitch, yaw);
        pitch = result.x;
        yaw = result.y;
    }

    @Inject(method = "applyRecoil", at = @At("RETURN"), require = 0)
    private static void confirmShot(ShotProfile shotProfile, CallbackInfo ci) {
        ManaStabilizerClientEvents.fired();
    }
}
