package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.renderer.MantleBlinkRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMantleBlinkNameMixin {
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$hideName(Entity entity, Component name, PoseStack pose,
            MultiBufferSource buffers, int light, float partialTick, CallbackInfo ci) {
        if (MantleBlinkRenderer.active(entity, partialTick)) ci.cancel();
    }
}
