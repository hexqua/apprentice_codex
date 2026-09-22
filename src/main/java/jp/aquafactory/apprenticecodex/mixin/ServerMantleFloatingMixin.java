package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMantleFloatingMixin {
    @Shadow public ServerPlayer player;

    // 浮遊禁止によるkickだけを正規の浮遊中に除外し、速度・衝突の検査は残す。
    @ModifyExpressionValue(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isFlightAllowed()Z"))
    private boolean apprenticecodex$allowAuthorizedHover(boolean original) {
        return original || ShootingStarMantleRuntime.isHovering(player);
    }
}
