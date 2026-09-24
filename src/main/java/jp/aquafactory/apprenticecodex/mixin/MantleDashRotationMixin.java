package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MantleDashRotationMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$rotateHoverDash(LivingEntity entity, PoseStack pose, float bob,
                                               float bodyYaw, float partialTick, float scale, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        if (!dash.shouldRenderHoverDashSpin(player)) return;
        var direction = dash.motion();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        // 視点やentityの照準は変更せず、体・翼・回転レイヤーを同じ入力方向へ向ける。
        pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees((player.tickCount + partialTick) * -75));
        ci.cancel();
    }
}
