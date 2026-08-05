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
    private static final float BLOCKED_RED = 0x88 / 255.0F;
    private static final float BLOCKED_GREEN = 0x44 / 255.0F;
    private static final float BLOCKED_BLUE = 0x44 / 255.0F;

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
            renderWing(entity, poseStack, buffer, packedLight);
            poseStack.popPose();
        }
        {
            poseStack.pushPose();
            poseStack.translate(-0.15, -0.5, -(1.0f/16.0f) * 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180-open));
            renderWing(entity, poseStack, buffer, packedLight);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderWing(AssistWingsWingEntity entity, PoseStack poseStack,
                            MultiBufferSource buffer, int packedLight) {
        if (entity.isFallProtectionBlocked()) {
            ExtrudedSpriteRenderer.render(
                    poseStack,
                    buffer,
                    packedLight,
                    getTextureLocation(entity),
                    ExtrudedSpriteRenderer.RenderMode.DEFAULT,
                    BLOCKED_RED,
                    BLOCKED_GREEN,
                    BLOCKED_BLUE,
                    1.0F
            );
            return;
        }

        ExtrudedSpriteRenderer.render(poseStack, buffer, packedLight, getTextureLocation(entity));
    }


    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AssistWingsWingEntity pEntity) {
        // 特殊パス.
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/assist_wings_wing.png");
    }
}
