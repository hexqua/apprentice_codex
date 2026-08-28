package jp.aquafactory.apprenticecodex.spell.shiden;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.ShidenKatanaModel;
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

public class ShidenKatanaRenderer extends GeoEntityRenderer<ShidenKatanaEntity> {
    private static final String TRAIL_TIP_BONE = "trail_tip";
    private static final String TRAIL_ROOT_BONE = "trail_root";

    private Vec3 trailTipBonePosition;
    private Vec3 trailRootBonePosition;

    public ShidenKatanaRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new ShidenKatanaModel<>());
        addRenderLayer(new SwordTrailLayer<>(this));
    }

    @Override
    public void render(@NotNull ShidenKatanaEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        trailTipBonePosition = null;
        trailRootBonePosition = null;

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
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ShidenKatanaEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        switch (bone.getName()) {
            case TRAIL_TIP_BONE -> trailTipBonePosition = boneWorldPosition(bone);
            case TRAIL_ROOT_BONE -> trailRootBonePosition = boneWorldPosition(bone);
            default -> {
            }
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    private static Vec3 boneWorldPosition(GeoBone bone) {
        var position = bone.getWorldPosition();
        return new Vec3(position.x(), position.y(), position.z());
    }
}
