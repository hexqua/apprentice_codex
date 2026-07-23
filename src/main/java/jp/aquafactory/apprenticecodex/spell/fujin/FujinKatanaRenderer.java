package jp.aquafactory.apprenticecodex.spell.fujin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.FujinKatanaModel;
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

public class FujinKatanaRenderer extends GeoEntityRenderer<FujinKatanaEntity> {
    private static final String TRAIL_TIP_BONE = "trail_tip";
    private static final String TRAIL_ROOT_BONE = "trail_root";
    private static final String SCABBARD_TOP_BONE = "scabbard_top";
    private static final String SCABBARD_BOTTOM_BONE = "scabbard_bottom";

    private Vec3 trailTipBonePosition;
    private Vec3 trailRootBonePosition;
    private Vec3 scabbardTopBonePosition;
    private Vec3 scabbardBottomBonePosition;

    public FujinKatanaRenderer(EntityRendererProvider.Context context) {
        super(context, new FujinKatanaModel<>());
        addRenderLayer(new SwordTrailLayer<>(this));
    }

    @Override
    public void render(@NotNull FujinKatanaEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        trailTipBonePosition = null;
        trailRootBonePosition = null;
        scabbardTopBonePosition = null;
        scabbardBottomBonePosition = null;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();

        if (trailTipBonePosition != null && trailRootBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    trailTipBonePosition,
                    trailRootBonePosition,
                    entity.level().getGameTime()
            );
        }
        if (scabbardTopBonePosition != null && scabbardBottomBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    FujinKatanaEntity.SCABBARD_CACHE_KEY,
                    scabbardTopBonePosition,
                    scabbardBottomBonePosition,
                    entity.level().getGameTime()
            );
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FujinKatanaEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int colour) {
        switch (bone.getName()) {
            case TRAIL_TIP_BONE -> trailTipBonePosition = boneWorldPosition(bone);
            case TRAIL_ROOT_BONE -> trailRootBonePosition = boneWorldPosition(bone);
            case SCABBARD_TOP_BONE -> scabbardTopBonePosition = boneWorldPosition(bone);
            case SCABBARD_BOTTOM_BONE -> scabbardBottomBonePosition = boneWorldPosition(bone);
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
