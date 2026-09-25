package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
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
    private void apprenticecodex$resetMovementOnTeleport(CallbackInfo ci) {
        var state = ShootingStarMantleRuntime.state(player);
        state.elemental.cancel(player);
        // 座標補正後の位置差を浮遊高度制御へ持ち込まず、ブリンクの水平移動は続ける。
        state.lastPosition = null;
        state.movingTicks = 0;
    }

    // 浮遊禁止によるkickだけを正規の浮遊中に除外し、速度・衝突の検査は残す。
    @ModifyExpressionValue(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isFlightAllowed()Z"))
    private boolean apprenticecodex$allowAuthorizedHover(boolean original) {
        return original || ShootingStarMantleRuntime.isHovering(player);
    }
}
