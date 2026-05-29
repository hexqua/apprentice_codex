package jp.aquafactory.apprenticecodex.spell.inscribeice;

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

public class InscribeIceDaggerRenderer extends EntityRenderer<InscribeIceDaggerEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/inscribe_ice_dagger.png");

    public InscribeIceDaggerRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull InscribeIceDaggerEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        var motion = entity.getDeltaMovement();
        var yawPitch = motion.lengthSqr() > 1.0E-6D
                ? RotationTools.calculateYawPitchByDirection(motion.normalize())
                : RotationTools.calculateYawPitchByEntity(entity, partialTicks);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(calculateRoll(entity, partialTicks)));
        // テクスチャは左下から右上へ伸びているため、先端が進行方向を向くよう補正する.
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(
                poseStack,
                bufferSource,
                packedLight,
                TEXTURE,
                ExtrudedSpriteRenderer.RenderMode.TRANSLUCENT
        );
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull InscribeIceDaggerEntity entity) {
        return TEXTURE;
    }

    private static float calculateRoll(InscribeIceDaggerEntity entity, float partialTicks) {
        var base = (entity.getId() * 73) % 360;
        var speed = 18.0F + (entity.getId() & 3) * 7.0F;
        if ((entity.getId() & 4) != 0) {
            speed = -speed;
        }
        return base + (entity.tickCount + partialTicks) * speed;
    }
}
