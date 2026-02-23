package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.PhalanxWeaponryModel;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PhalanxWeaponryRenderer extends GeoEntityRenderer<PhalanxWeaponryEntity> {
    private static final String PLATE_FLASH_BONE = "plate_flash";
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;

    private float plateFlashStrength = 0.0f;

    public PhalanxWeaponryRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new PhalanxWeaponryModel<>());
    }

    @Override
    public void render(@NotNull PhalanxWeaponryEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        plateFlashStrength = entity.getGuardFlashStrength(partialTicks);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
        plateFlashStrength = 0.0f;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, PhalanxWeaponryEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (!PLATE_FLASH_BONE.equals(bone.getName())) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (plateFlashStrength <= 0.0f) {
            return;
        }

        var flashStrength = Math.min(1.0f, plateFlashStrength);
        var flashRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
        var flashBuffer = bufferSource.getBuffer(flashRenderType);
        super.renderRecursively(
                poseStack, animatable, bone, flashRenderType, bufferSource, flashBuffer, isReRender, partialTick,
                FULL_BRIGHT_LIGHT, packedOverlay, 1.0f, 1.0f, 1.0f, flashStrength
        );
    }
}
