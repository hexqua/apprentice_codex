package jp.aquafactory.apprenticecodex.spell.manaslash;

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
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ManaSlashProjectileRenderer extends EntityRenderer<ManaSlashProjectileEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/entity/mana_slash.png");
    private static final net.minecraft.client.renderer.RenderType RENDER_TYPE =
            ApprenticeRenderTypes.additiveEntityNoCull("mana_slash_additive", TEXTURE);

    private static final float BASE_RED = 0.30f;
    private static final float BASE_GREEN = 0.78f;
    private static final float BASE_BLUE = 1.00f;
    private static final float BASE_ALPHA = 0.85f;

    public ManaSlashProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ManaSlashProjectileEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        var motion = entity.getDeltaMovement();
        var yawPitch = motion.lengthSqr() > 1.0e-6
                ? RotationTools.calculateYawPitchByDirection(motion)
                : RotationTools.calculateYawPitchByEntity(entity, partialTicks);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        entity.animationTime++;
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                ((entity.animationSeed % 30) - 15) * (float) Math.sin(entity.animationTime * 0.015f)
        ));

        var oldWidth = (float) entity.oldBB.getXsize();
        var width = Mth.lerp(Math.min(partialTicks, 1.0f), oldWidth, entity.getBbWidth());
        var alphaScale = getFadeAlpha(entity, partialTicks);
        var buffer = bufferSource.getBuffer(RENDER_TYPE);

        drawSlashLayer(poseStack, buffer, width, 0.0f, 0.0f, 1.00f, alphaScale);
        drawSlashLayer(poseStack, buffer, width, -14.0f, -10.0f, 0.84f, alphaScale);
        drawSlashLayer(poseStack, buffer, width, 16.0f, 12.0f, 0.70f, alphaScale);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ManaSlashProjectileEntity entity) {
        return TEXTURE;
    }

    private static float getFadeAlpha(ManaSlashProjectileEntity entity, float partialTicks) {
        var fadeStartTick = ManaSlashProjectileEntity.EXPIRE_TIME_TICKS - ManaSlashProjectileEntity.FADE_DURATION_TICKS;
        var age = entity.tickCount + Math.min(partialTicks, 1.0f);
        if (age <= fadeStartTick) {
            return 1.0f;
        }

        // 消滅直前だけ減衰させ、中距離弾としての視認性は維持する。
        var fadeProgress = Mth.clamp((age - fadeStartTick) / ManaSlashProjectileEntity.FADE_DURATION_TICKS, 0.0f, 1.0f);
        return 1.0f - easeOutCubic(fadeProgress);
    }

    private static float easeOutCubic(float value) {
        var clamped = Mth.clamp(value, 0.0f, 1.0f);
        var inverse = 1.0f - clamped;
        return 1.0f - inverse * inverse * inverse;
    }

    private static void drawSlashLayer(PoseStack poseStack, VertexConsumer buffer, float width,
                                       float yawOffset, float rollOffset, float intensity, float alphaScale) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yawOffset));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollOffset));

        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        var halfWidth = width * 0.5f;
        var alpha = BASE_ALPHA * intensity * alphaScale;

        // 加算合成では alpha だけだと減衰しづらいため、RGB 側にも同じ比率を掛ける。
        var red = BASE_RED * alpha;
        var green = BASE_GREEN * alpha;
        var blue = BASE_BLUE * alpha;

        buffer.vertex(poseMatrix, -halfWidth, -0.1f, -halfWidth)
                .color(red, green, blue, alpha)
                .uv(0.0f, 1.0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .endVertex();
        buffer.vertex(poseMatrix, halfWidth, -0.1f, -halfWidth)
                .color(red, green, blue, alpha)
                .uv(1.0f, 1.0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .endVertex();
        buffer.vertex(poseMatrix, halfWidth, -0.1f, halfWidth)
                .color(red, green, blue, alpha)
                .uv(1.0f, 0.0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .endVertex();
        buffer.vertex(poseMatrix, -halfWidth, -0.1f, halfWidth)
                .color(red, green, blue, alpha)
                .uv(0.0f, 0.0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .endVertex();

        poseStack.popPose();
    }
}
