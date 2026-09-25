package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlinkRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMantleFloatingMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void apprenticecodex$cancelBlinkOnTeleport(CallbackInfo ci) {
        if (QuickBlinkRuntime.active(player)) QuickBlinkRuntime.clear(player);
        var state = ShootingStarMantleRuntime.state(player);
        state.elemental.cancel(player);
        if (state.blink.start() >= 0) {
            state.blink.cancel();
            state.lastPosition = null;
            state.movingTicks = 0;
            ShootingStarMantleRuntime.sync(player, false);
        }
    }

    // 浮遊禁止によるkickだけを正規の浮遊中に除外し、速度・衝突の検査は残す。
    @ModifyExpressionValue(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isFlightAllowed()Z"))
    private boolean apprenticecodex$allowAuthorizedHover(boolean original) {
        return original || ShootingStarMantleRuntime.isHovering(player);
    }
}
