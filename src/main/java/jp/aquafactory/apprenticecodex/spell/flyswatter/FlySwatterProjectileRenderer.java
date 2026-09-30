package jp.aquafactory.apprenticecodex.spell.flyswatter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.client.render.ColorCubeRenderTools;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class FlySwatterProjectileRenderer extends EntityRenderer<FlySwatterProjectileEntity> {
    private static final ResourceLocation MISSILE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/fly_swatter_missile.png");

    private static final RenderType BURST_RENDER_TYPE =
            ApprenticeRenderTypes.additiveColorNoCull("fly_swatter_burst_additive");

    public FlySwatterProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull FlySwatterProjectileEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        if (entity.isBursting()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getBurstSpinDegrees(partialTicks)));
            float alpha = entity.getBurstCubeAlpha(partialTicks);
            ColorCubeRenderTools.drawCube(poseStack, buffer.getBuffer(BURST_RENDER_TYPE),
                    entity.getBurstCubeScale(partialTicks), Math.round(255 * alpha),
                    Math.round(122 * alpha), Math.round(31 * alpha), Math.round(255 * alpha), false);
            poseStack.popPose();
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        var motion = entity.getDeltaMovement();
        var hasMotion = motion.lengthSqr() > 1.0e-6;
        var yawPitch = hasMotion
                ? RotationTools.calculateYawPitchByDirection(motion)
                : RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        // ProjectileUtilのyawは+Z基準より180度ずれるため、速度ゼロ時の同期角だけ補正する。
        var yaw = hasMotion ? yawPitch.yaw() : yawPitch.yaw() - 180.0f;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        // 画像右上(+X,+Y)を+Zへ向けてから、進行方向のyaw/pitchを適用する。
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f));
        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(poseStack, buffer, packedLight, MISSILE_TEXTURE);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FlySwatterProjectileEntity entity) {
        return MISSILE_TEXTURE;
    }
}
