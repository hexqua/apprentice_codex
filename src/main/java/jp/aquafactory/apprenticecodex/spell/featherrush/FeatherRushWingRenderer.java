package jp.aquafactory.apprenticecodex.spell.featherrush;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class FeatherRushWingRenderer extends EntityRenderer<FeatherRushWingEntity> {
    public FeatherRushWingRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(@NotNull FeatherRushWingEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        float t = entity.tickCount + partialTicks;
        float open = 30.0f + (float) Math.sin(t * 0.85f) * 20.0f;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        {
            poseStack.pushPose();
            poseStack.translate(0.15, -0.5, -(1.0f / 16.0f) * 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(+open));
            ExtrudedSpriteRenderer.render(poseStack, buffer, packedLight, getTextureLocation(entity));
            poseStack.popPose();
        }
        {
            poseStack.pushPose();
            poseStack.translate(-0.15, -0.5, -(1.0f / 16.0f) * 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180 - open));
            ExtrudedSpriteRenderer.render(poseStack, buffer, packedLight, getTextureLocation(entity));
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FeatherRushWingEntity pEntity) {
        // 現状はAssist Wingsと同じ見た目を利用.
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/feather_rush_wing.png");
    }
}
