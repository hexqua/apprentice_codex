package jp.aquafactory.apprenticecodex.spell.bloodbrand;

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

public class BloodBrandKunaiRenderer extends EntityRenderer<BloodBrandKunai> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/blood_brand_kunai.png");

    public BloodBrandKunaiRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull BloodBrandKunai entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        var motion = entity.getDeltaMovement();
        var yawPitch = motion.lengthSqr() > 1.0E-6D
                ? RotationTools.calculateYawPitchByDirection(motion.normalize())
                : RotationTools.calculateYawPitchByEntity(entity, partialTicks);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(
                poseStack,
                bufferSource,
                packedLight,
                TEXTURE,
                ExtrudedSpriteRenderer.RenderMode.DEFAULT
        );
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BloodBrandKunai entity) {
        return TEXTURE;
    }
}
