package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.PhalanxWeaponryModel;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PhalanxWeaponryRenderer extends GeoEntityRenderer<PhalanxWeaponryEntity> {
    private static final String PLATE_FLASH_BONE = "plate_flash";
    private static final String SHIELD_CENTER_BONE = "shield_center";
    private static final String SPEAR_BOTTOM_BONE = "spear_bottom";
    private static final String SPEAR_TOP_BONE = "spear_top";
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;

    private float plateFlashStrength = 0.0f;
    private Vec3 shieldCenterBonePosition;
    private Vec3 spearBottomBonePosition;
    private Vec3 spearTopBonePosition;

    public PhalanxWeaponryRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new PhalanxWeaponryModel<>());
    }

    @Override
    public void render(@NotNull PhalanxWeaponryEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        plateFlashStrength = entity.getGuardFlashStrength(partialTicks);
        shieldCenterBonePosition = null;
        spearBottomBonePosition = null;
        spearTopBonePosition = null;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();

        //noinspection resource
        var gameTime = entity.level().getGameTime();
        if (shieldCenterBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    PhalanxWeaponryEntity.SHIELD_CENTER_CACHE_KEY,
                    shieldCenterBonePosition,
                    shieldCenterBonePosition,
                    gameTime
            );
        }

        if (spearBottomBonePosition != null && spearTopBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    PhalanxWeaponryEntity.SPEAR_LINE_CACHE_KEY,
                    spearTopBonePosition,
                    spearBottomBonePosition,
                    gameTime
            );
        }

        plateFlashStrength = 0.0f;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, PhalanxWeaponryEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        switch (bone.getName()) {
            case SHIELD_CENTER_BONE -> shieldCenterBonePosition = boneWorldPosition(bone);
            case SPEAR_BOTTOM_BONE -> spearBottomBonePosition = boneWorldPosition(bone);
            case SPEAR_TOP_BONE -> spearTopBonePosition = boneWorldPosition(bone);
            default -> {
            }
        }

        if (!PLATE_FLASH_BONE.equals(bone.getName())) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (plateFlashStrength <= 0.0f) {
            return;
        }

        var flashStrength = Math.min(1.0f, plateFlashStrength);
        var flashRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
        var flashBuffer = bufferSource.getBuffer(flashRenderType);
        var flashAlpha = Mth.clamp((int) (flashStrength * 255.0f), 0, 255);
        var flashColour = (flashAlpha << 24) | 0xFFFFFF;
        super.renderRecursively(
                poseStack, animatable, bone, flashRenderType, bufferSource, flashBuffer, isReRender, partialTick,
                FULL_BRIGHT_LIGHT, packedOverlay, flashColour
        );
    }

    private static Vec3 boneWorldPosition(GeoBone bone) {
        var position = bone.getWorldPosition();
        return new Vec3(position.x(), position.y(), position.z());
    }
}
