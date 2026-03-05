package jp.aquafactory.apprenticecodex.spell.precisionjack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.PrecisionJackKnifeModel;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.renderer.SwordTrailLayer;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PrecisionJackKnifeRenderer extends GeoEntityRenderer<PrecisionJackKnifeEntity> {
    private static final String BLADE_TOP_BONE = "blade_top";
    private static final String BLADE_ROOT_BONE = "blade_root";

    private Vec3 bladeTopBonePosition;
    private Vec3 bladeRootBonePosition;

    public PrecisionJackKnifeRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new PrecisionJackKnifeModel<>());
        addRenderLayer(new SwordTrailLayer<>(this));
    }

    @Override
    public void render(@NotNull PrecisionJackKnifeEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        bladeTopBonePosition = null;
        bladeRootBonePosition = null;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();

        if (bladeTopBonePosition != null && bladeRootBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    PrecisionJackKnifeEntity.TRAIL_CACHE_KEY,
                    bladeTopBonePosition,
                    bladeRootBonePosition,
                    entity.level().getGameTime()
            );
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, PrecisionJackKnifeEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        switch (bone.getName()) {
            case BLADE_TOP_BONE -> bladeTopBonePosition = boneWorldPosition(bone);
            case BLADE_ROOT_BONE -> bladeRootBonePosition = boneWorldPosition(bone);
            default -> {
            }
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    private static Vec3 boneWorldPosition(GeoBone bone) {
        var position = bone.getWorldPosition();
        return new Vec3(position.x(), position.y(), position.z());
    }
}

