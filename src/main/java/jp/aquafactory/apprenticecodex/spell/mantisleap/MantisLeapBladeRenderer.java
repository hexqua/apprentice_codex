package jp.aquafactory.apprenticecodex.spell.mantisleap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.MantisLeapBladeModel;
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

public class MantisLeapBladeRenderer extends GeoEntityRenderer<MantisLeapBladeEntity> {
    private static final String TRAIL_1_TIP_BONE = "trail_tip1";
    private static final String TRAIL_1_ROOT_BONE = "trail_root1";
    private static final String TRAIL_2_TIP_BONE = "trail_tip2";
    private static final String TRAIL_2_ROOT_BONE = "trail_root2";

    private Vec3 trail1TipBonePosition;
    private Vec3 trail1RootBonePosition;
    private Vec3 trail2TipBonePosition;
    private Vec3 trail2RootBonePosition;

    public MantisLeapBladeRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new MantisLeapBladeModel<>());
        addRenderLayer(new SwordTrailLayer<>(this));
    }

    @Override
    public void render(@NotNull MantisLeapBladeEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        trail1TipBonePosition = null;
        trail1RootBonePosition = null;
        trail2TipBonePosition = null;
        trail2RootBonePosition = null;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();

        //noinspection resource
        var gameTime = entity.level().getGameTime();
        if (trail1TipBonePosition != null && trail1RootBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    MantisLeapBladeEntity.TRAIL_1_CACHE_KEY,
                    trail1TipBonePosition,
                    trail1RootBonePosition,
                    gameTime
            );
        }

        if (trail2TipBonePosition != null && trail2RootBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    MantisLeapBladeEntity.TRAIL_2_CACHE_KEY,
                    trail2TipBonePosition,
                    trail2RootBonePosition,
                    gameTime
            );
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, MantisLeapBladeEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        switch (bone.getName()) {
            case TRAIL_1_TIP_BONE -> trail1TipBonePosition = boneWorldPosition(bone);
            case TRAIL_1_ROOT_BONE -> trail1RootBonePosition = boneWorldPosition(bone);
            case TRAIL_2_TIP_BONE -> trail2TipBonePosition = boneWorldPosition(bone);
            case TRAIL_2_ROOT_BONE -> trail2RootBonePosition = boneWorldPosition(bone);
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
