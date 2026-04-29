package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowItemRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererFocusStaffbowContextMixin {
    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("HEAD")
    )
    private void apprentice_codex$pushFocusStaffbowRenderEntity(@Nullable LivingEntity entity, ItemStack stack,
                                                                ItemDisplayContext displayContext, boolean leftHand,
                                                                PoseStack poseStack, MultiBufferSource buffer,
                                                                @Nullable Level level, int combinedLight,
                                                                int combinedOverlay, int seed, CallbackInfo ci) {
        FocusStaffbowItemRenderContext.push(entity);
    }

    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("RETURN")
    )
    private void apprentice_codex$popFocusStaffbowRenderEntity(@Nullable LivingEntity entity, ItemStack stack,
                                                               ItemDisplayContext displayContext, boolean leftHand,
                                                               PoseStack poseStack, MultiBufferSource buffer,
                                                               @Nullable Level level, int combinedLight,
                                                               int combinedOverlay, int seed, CallbackInfo ci) {
        FocusStaffbowItemRenderContext.pop();
    }
}
