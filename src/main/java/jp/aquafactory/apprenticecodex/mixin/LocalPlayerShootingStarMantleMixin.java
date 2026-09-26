package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerShootingStarMantleMixin {
    @Inject(method = "aiStep", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private void apprenticecodex$startMantleFlight(CallbackInfo ci) {
        var player = (LocalPlayer) (Object) this;
        // 1.20.1のclientは胸装備の判定で背中枠の外套を弾くため、同じ入力条件内から開始packetを送る。
        if (ShootingStarMantleRuntime.canFly(player) && player.tryToStartFallFlying()) {
            player.connection.send(new ServerboundPlayerCommandPacket(player,
                    ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }
}
