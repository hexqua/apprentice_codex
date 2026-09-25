package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleBlink;
import jp.aquafactory.apprenticecodex.renderer.MantleBlinkRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMantleBlinkMixin {
    // Epic FightはRenderLivingEvent内で描画するため、その外側で全装備の座標系を包む。
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void apprenticecodex$blink(EntityRenderer<Entity> renderer, Entity entity, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light) {
        if (!MantleBlinkRenderer.active(entity, partialTick)) {
            renderer.render(entity, yaw, partialTick, pose, buffers, light);
            return;
        }
        double age = MantleBlinkRenderer.elapsed(entity, partialTick);
        if (MantleBlink.moving(age)) return;
        float amount = MantleBlinkRenderer.distortion(age);
        pose.pushPose();
        try {
            MantleBlinkRenderer.transform(entity, pose, amount);
            renderer.render(entity, yaw, partialTick, pose, buffers, light);
            MantleBlinkRenderer.drawLight(entity, pose, buffers, amount);
        } finally {
            // Pre eventがキャンセルされても、他entityへ伸縮を漏らさない。
            pose.popPose();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;displayFireAnimation()Z"))
    private boolean apprenticecodex$hideFlame(Entity entity) {
        return !MantleBlinkRenderer.active(entity, Minecraft.getInstance().getFrameTime()) && entity.displayFireAnimation();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInvisible()Z"))
    private boolean apprenticecodex$hideShadow(Entity entity) {
        return MantleBlinkRenderer.active(entity, Minecraft.getInstance().getFrameTime()) || entity.isInvisible();
    }
}
