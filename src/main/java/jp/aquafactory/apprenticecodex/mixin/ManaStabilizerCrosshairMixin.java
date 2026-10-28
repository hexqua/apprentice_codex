package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.apprenticecodex.event.client.ManaStabilizerClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "io.redspace.irons_artifice.client.gui.CrosshairRenderer", remap = false)
public abstract class ManaStabilizerCrosshairMixin {
    // 目標幅は変更せず、回復支援で縮む表示だけを速く追従させる。拡大時は上流の補間を維持する。
    // Forgeの起動時にArgs生成クラスを解決できないため、引数を直接受けて元の呼出しへ渡す。
    @WrapOperation(method = "updateCrosshairCursor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;lerp(FFF)F", remap = true), require = 0)
    private static float closeFaster(float delta, float current, float target, Operation<Float> original) {
        if (target < current && ManaStabilizerClientEvents.accelerateCrosshairClosing()) {
            delta = 0.75F;
        }
        return original.call(delta, current, target);
    }
}
