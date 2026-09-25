package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlinkRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMantleTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$hoverTravel(Vec3 input, CallbackInfo ci) {
        //noinspection ConstantValue
        if (!((Object) this instanceof Player player)) return;
        if (QuickBlinkRuntime.active(player)) {
            if (player.isControlledByLocalInstance()) QuickBlinkRuntime.state(player).blink.travel(player);
            ci.cancel();
            return;
        }
        if (ShootingStarMantleRuntime.isHovering(player)) {
            player.fallDistance = 0;
            // 通常のプレイヤー同様、移動は操作clientが予測しserverはpacketで受け取る。
            if (player.isControlledByLocalInstance()) MantleMovement.travel(player, input, ShootingStarMantleRuntime.state(player));
            ci.cancel();
        }
    }
}
