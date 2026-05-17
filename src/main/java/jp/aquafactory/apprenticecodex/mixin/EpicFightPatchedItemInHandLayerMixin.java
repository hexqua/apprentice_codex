package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightScrollcasterGauntletOffhandBridge;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.renderer.patched.layer.PatchedItemInHandLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = PatchedItemInHandLayer.class, remap = false)
public abstract class EpicFightPatchedItemInHandLayerMixin {
    @Inject(
            method = "renderLayer(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;"
                    + "Lnet/minecraft/world/entity/LivingEntity;"
                    + "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "I[Lyesman/epicfight/api/utils/math/OpenMatrix4f;FFFF)V",
            at = @At("TAIL")
    )
    private void apprenticecodex$renderGauntletOffhandVisual(
            LivingEntityPatch<?> entitypatch,
            LivingEntity entityliving,
            RenderLayer<?, ?> vanillaLayer,
            PoseStack postStack,
            MultiBufferSource buffer,
            int packedLight,
            OpenMatrix4f[] poses,
            float bob,
            float yRot,
            float xRot,
            float partialTicks,
            CallbackInfo callback
    ) {
        // Epic Fight の描画レイヤーは getAdvancedHoldingItemStack を見ないため、表示だけここで補う.
        var offhandVisualStack = EpicFightScrollcasterGauntletOffhandBridge.getExtraRenderedOffhandStack(entitypatch);
        if (offhandVisualStack.isEmpty()) {
            return;
        }

        var renderEngine = ClientEngine.getInstance().renderEngine;
        renderEngine.getItemRenderer(offhandVisualStack).renderItemInHand(
                offhandVisualStack,
                entitypatch,
                InteractionHand.OFF_HAND,
                poses,
                buffer,
                postStack,
                packedLight,
                partialTicks
        );
    }
}
