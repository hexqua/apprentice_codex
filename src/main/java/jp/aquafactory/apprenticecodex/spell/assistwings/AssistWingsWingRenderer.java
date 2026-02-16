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
        poseStack.pushPose();
        // todo:翼が羽ばたいているように見えるようにする.
        poseStack.translate(-0.5, -0.5, - (1.0/16.0) * 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        ExtrudedSpriteRenderer.render(poseStack, buffer, packedLight,getTextureLocation(entity));
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }


    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AssistWingsWingEntity pEntity) {
        // 特殊パス.
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/spell_wing.png");
    }
}
