package jp.aquafactory.apprenticecodex.spell.assistwings;

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

public class AssistWingsWingRenderer extends EntityRenderer<AssistWingsWingEntity> {
    public AssistWingsWingRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(@NotNull AssistWingsWingEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        float t = entity.tickCount + partialTicks;
        float open = 30.0f + (float)Math.sin(t * 0.35f) * 20.0f;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        {
            poseStack.pushPose();
            poseStack.translate(0.15, -0.5, -(1.0f/16.0f) * 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(+open));
            ExtrudedSpriteRenderer.render(poseStack, buffer, packedLight, getTextureLocation(entity));
            poseStack.popPose();
        }
        {
            poseStack.pushPose();
            poseStack.translate(-0.15, -0.5, -(1.0f/16.0f) * 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180-open));
            ExtrudedSpriteRenderer.render(poseStack, buffer, packedLight, getTextureLocation(entity));
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }


    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AssistWingsWingEntity pEntity) {
        // 特殊パス.
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/spell_wing.png");
    }
}
