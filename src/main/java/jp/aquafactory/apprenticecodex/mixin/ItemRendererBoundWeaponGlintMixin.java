package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import jp.aquafactory.apprenticecodex.item.boundweapon.BoundBowItem;
import jp.aquafactory.apprenticecodex.item.boundweapon.BoundSwordItem;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemRenderer.class, priority = 900)
public abstract class ItemRendererBoundWeaponGlintMixin {
    @Redirect(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            require = 0
    )
    private VertexConsumer apprentice_codex$getBoundWeaponFoilBuffer(MultiBufferSource bufferSource,
                                                                     RenderType renderType,
                                                                     boolean isItem,
                                                                     boolean glint,
                                                                     ItemStack stack,
                                                                     ItemDisplayContext displayContext,
                                                                     boolean leftHand,
                                                                     PoseStack poseStack,
                                                                     MultiBufferSource renderBufferSource,
                                                                     int combinedLight,
                                                                     int combinedOverlay,
                                                                     BakedModel model) {
        if (glint && (BoundBowItem.isBoundBow(stack) || BoundSwordItem.isBoundSword(stack))
                && ApprenticeRenderTypes.areBoundSpellWeaponGlintBuffersRegistered()) {
            return VertexMultiConsumer.create(
                    bufferSource.getBuffer(ApprenticeRenderTypes.boundSpellWeaponGlint()),
                    bufferSource.getBuffer(renderType)
            );
        }
        return ItemRenderer.getFoilBuffer(bufferSource, renderType, isItem, glint);
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            require = 0
    )
    private VertexConsumer apprentice_codex$getBoundWeaponFoilBufferDirect(MultiBufferSource bufferSource,
                                                                           RenderType renderType,
                                                                           boolean isItem,
                                                                           boolean glint,
                                                                           ItemStack stack,
                                                                           ItemDisplayContext displayContext,
                                                                           boolean leftHand,
                                                                           PoseStack poseStack,
                                                                           MultiBufferSource renderBufferSource,
                                                                           int combinedLight,
                                                                           int combinedOverlay,
                                                                           BakedModel model) {
        if (glint && (BoundBowItem.isBoundBow(stack) || BoundSwordItem.isBoundSword(stack))
                && ApprenticeRenderTypes.areBoundSpellWeaponGlintBuffersRegistered()) {
            return VertexMultiConsumer.create(
                    bufferSource.getBuffer(ApprenticeRenderTypes.boundSpellWeaponGlintDirect()),
                    bufferSource.getBuffer(renderType)
            );
        }
        return ItemRenderer.getFoilBufferDirect(bufferSource, renderType, isItem, glint);
    }
}
