package jp.aquafactory.apprenticecodex.spell.demicreatorwings;

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

public class DemicreatorWingsWingRenderer extends EntityRenderer<DemicreatorWingsWingEntity> {
    public DemicreatorWingsWingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull DemicreatorWingsWingEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        var animationTick = entity.tickCount + partialTicks;
        var open = 32.0f + (float) Math.sin(animationTick * 0.42f) * 18.0f;

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
    public @NotNull ResourceLocation getTextureLocation(@NotNull DemicreatorWingsWingEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/demicreator_wings_wing.png");
    }
}
