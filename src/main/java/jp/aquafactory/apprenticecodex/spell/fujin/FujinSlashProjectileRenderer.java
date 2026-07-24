package jp.aquafactory.apprenticecodex.spell.fujin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class FujinSlashProjectileRenderer extends EntityRenderer<FujinSlashProjectileEntity> {
    private static final float PRIMARY_INTENSITY = 0.8F;
    private static final float SECONDARY_INTENSITY = 0.5F;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/entity/fujin_slash.png");
    private static final net.minecraft.client.renderer.RenderType RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("fujin_slash_additive", TEXTURE);

    public FujinSlashProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FujinSlashProjectileEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        var motion = entity.getDeltaMovement();
        var yawPitch = motion.lengthSqr() > 1.0E-6D
                ? RotationTools.calculateYawPitchByDirection(motion)
                : RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        var depthScale = 1.0F - entity.getSquashProgress(partialTicks);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        var buffer = bufferSource.getBuffer(RENDER_TYPE);
        // Blood Slash と同様、進行方向を含む2枚の面を浅く交差させて奥行きを見せる。
        drawSlashLayer(poseStack, buffer, -15.0F, -10.0F, PRIMARY_INTENSITY, depthScale);
        drawSlashLayer(poseStack, buffer, 15.0F, 10.0F, SECONDARY_INTENSITY, depthScale);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void drawSlashLayer(PoseStack poseStack, VertexConsumer buffer,
                                       float yawOffset, float rollOffset,
                                       float intensity, float depthScale) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yawOffset));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollOffset));

        var pose = poseStack.last();
        var matrix = pose.pose();
        var halfSize = FujinSlashProjectileEntity.SLASH_WIDTH * 0.5F;
        var halfDepth = halfSize * depthScale;

        buffer.addVertex(matrix, -halfSize, -0.1F, -halfDepth)
                .setColor(intensity, intensity, intensity, 1.0F)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        buffer.addVertex(matrix, halfSize, -0.1F, -halfDepth)
                .setColor(intensity, intensity, intensity, 1.0F)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        buffer.addVertex(matrix, halfSize, -0.1F, halfDepth)
                .setColor(intensity, intensity, intensity, 1.0F)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        buffer.addVertex(matrix, -halfSize, -0.1F, halfDepth)
                .setColor(intensity, intensity, intensity, 1.0F)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);

        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FujinSlashProjectileEntity entity) {
        return TEXTURE;
    }
}
