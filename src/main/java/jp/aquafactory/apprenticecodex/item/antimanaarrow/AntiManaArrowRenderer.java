package jp.aquafactory.apprenticecodex.item.antimanaarrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class AntiManaArrowRenderer extends ArrowRenderer<AntiManaArrowEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/entity/anti_mana_arrow.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    public AntiManaArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AntiManaArrowEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        var shake = (float) entity.shakeTime - partialTicks;
        if (shake > 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(0.05625F, 0.05625F, 0.05625F);
        poseStack.translate(-4.0F, 0.0F, 0.0F);

        var consumer = bufferSource.getBuffer(RENDER_TYPE);
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        vertex(poseMatrix, normalMatrix, consumer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, LightTexture.FULL_BRIGHT);
        vertex(poseMatrix, normalMatrix, consumer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, LightTexture.FULL_BRIGHT);
        vertex(poseMatrix, normalMatrix, consumer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, LightTexture.FULL_BRIGHT);
        vertex(poseMatrix, normalMatrix, consumer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, LightTexture.FULL_BRIGHT);
        vertex(poseMatrix, normalMatrix, consumer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, LightTexture.FULL_BRIGHT);
        vertex(poseMatrix, normalMatrix, consumer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, LightTexture.FULL_BRIGHT);
        vertex(poseMatrix, normalMatrix, consumer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, LightTexture.FULL_BRIGHT);
        vertex(poseMatrix, normalMatrix, consumer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, LightTexture.FULL_BRIGHT);
        for (var i = 0; i < 4; ++i) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            vertex(poseMatrix, normalMatrix, consumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, LightTexture.FULL_BRIGHT);
            vertex(poseMatrix, normalMatrix, consumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, LightTexture.FULL_BRIGHT);
            vertex(poseMatrix, normalMatrix, consumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, LightTexture.FULL_BRIGHT);
            vertex(poseMatrix, normalMatrix, consumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AntiManaArrowEntity entity) {
        return TEXTURE;
    }
}
