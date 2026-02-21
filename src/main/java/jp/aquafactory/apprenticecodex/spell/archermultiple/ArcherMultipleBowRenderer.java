package jp.aquafactory.apprenticecodex.spell.archermultiple;

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

public class ArcherMultipleBowRenderer extends EntityRenderer<ArcherMultipleBowEntity> {
    private static final ResourceLocation[] BOW_ANIM_TEX = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/archer_multiple_bow_0.png"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/archer_multiple_bow_1.png"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/archer_multiple_bow_2.png"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/archer_multiple_bow_3.png")
    };

    public ArcherMultipleBowRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(ArcherMultipleBowEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var stage = Math.max(0, Math.min(entity.getStage(), BOW_ANIM_TEX.length - 1));
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        // 左上に発射口が向いているアイテムが先端を向くように調整.
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(+45.0f));

        poseStack.translate(-0.5, -0.5, -(1.0f / 16.0f) * 0.5f);
        ExtrudedSpriteRenderer.render(poseStack, buffer, packedLight, BOW_ANIM_TEX[stage]);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ArcherMultipleBowEntity pEntity) {
        var stage = Math.max(0, Math.min(pEntity.getStage(), BOW_ANIM_TEX.length - 1));
        return BOW_ANIM_TEX[stage];
    }
}
