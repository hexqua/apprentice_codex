package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientMantlePositionMixin {
    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void apprenticecodex$resetHoverPosition(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            // 転送・server補正の座標差を通常のXZ移動として高度低下へ持ち込まない。
            var state = ShootingStarMantleRuntime.state(player);
            state.blink.cancel();
            state.lastPosition = null;
            state.movingTicks = 0;
        }
    }
}
