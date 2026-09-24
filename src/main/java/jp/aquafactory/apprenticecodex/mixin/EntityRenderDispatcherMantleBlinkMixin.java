package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleBlink;
import jp.aquafactory.apprenticecodex.renderer.MantleBlinkRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMantleBlinkMixin {
    // Epic FightはRenderLivingEvent内で描画するため、その外側で全装備の座標系を包む。
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void apprenticecodex$blink(EntityRenderer<Entity> renderer, Entity entity, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light, Operation<Void> original) {
        if (!MantleBlinkRenderer.active(entity, partialTick)) {
            original.call(renderer, entity, yaw, partialTick, pose, buffers, light);
            return;
        }
        double age = MantleBlinkRenderer.elapsed(entity, partialTick);
        if (MantleBlink.moving(age)) return;
        float amount = MantleBlinkRenderer.distortion(age);
        pose.pushPose();
        try {
            MantleBlinkRenderer.transform(entity, pose, amount);
            original.call(renderer, entity, yaw, partialTick, pose, buffers, light);
            MantleBlinkRenderer.drawLight(entity, pose, buffers, amount);
        } finally {
            // Pre eventがキャンセルされても、他entityへ伸縮を漏らさない。
            pose.popPose();
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;displayFireAnimation()Z"))
    private boolean apprenticecodex$hideFlame(Entity entity, Operation<Boolean> original,
            @Local(argsOnly = true, ordinal = 1) float partialTick) {
        return !MantleBlinkRenderer.active(entity, partialTick) && original.call(entity);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInvisible()Z"))
    private boolean apprenticecodex$hideShadow(Entity entity, Operation<Boolean> original,
            @Local(argsOnly = true, ordinal = 1) float partialTick) {
        return MantleBlinkRenderer.active(entity, partialTick) || original.call(entity);
    }
}
