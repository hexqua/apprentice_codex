package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerRemoteOwnerCastMixin {
    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastPlaySound(
            SoundEvent sound,
            float volume,
            float pitch,
            CallbackInfo ci
    ) {
        var self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var context = RemoteOwnerCastContext.get(serverPlayer);
        if (context == null) {
            return;
        }

        // Player.playSound は発音元の本人を除外するため、手動 remote cast では所有者だけ無音になる。
        // 実座標は動かさず、remote context 中の詠唱音だけ remote 座標から全員へ送る。
        var feetPosition = context.eyePosition().subtract(0.0D, self.getEyeHeight(), 0.0D);
        self.level().playSound(
                null,
                feetPosition.x,
                feetPosition.y,
                feetPosition.z,
                sound,
                self.getSoundSource(),
                volume,
                pitch
        );
        ci.cancel();
    }
}
