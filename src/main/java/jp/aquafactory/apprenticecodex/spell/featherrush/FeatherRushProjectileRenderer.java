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

public class FeatherRushProjectileRenderer extends EntityRenderer<FeatherRushProjectileEntity> {
    public FeatherRushProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull FeatherRushProjectileEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var motion = entity.getDeltaMovement();
        var yawPitch = motion.lengthSqr() > 1.0e-6
                ? RotationTools.calculateYawPitchByDirection(motion)
                : RotationTools.calculateYawPitchByEntity(entity, partialTicks);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        // 羽根をキリモミ回転させる.
        poseStack.mulPose(Axis.ZP.rotationDegrees(2 * 360 * (entity.tickCount + partialTicks) / 20.0f));

        // テクスチャ左下(羽根先端)が進行方向を向く補正.
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f));
        poseStack.translate(-0.5f, -0.5f, -(1.0f / 16.0f) * 0.5f);

        ExtrudedSpriteRenderer.render(poseStack, buffer, packedLight, getTextureLocation(entity));
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FeatherRushProjectileEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/feather_rush_feather.png");
    }
}
