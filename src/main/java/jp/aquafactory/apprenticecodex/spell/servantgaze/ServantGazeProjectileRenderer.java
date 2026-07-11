package jp.aquafactory.apprenticecodex.spell.servantgaze;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.spells.fireball.FireballRenderer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class ServantGazeProjectileRenderer extends EntityRenderer<ServantGazeProjectileEntity> {
    private static final ResourceLocation TEXTURE = IronsSpellbooks.id("textures/entity/magic_missile/magic_missile.png");
    private static final ResourceLocation FLARE = IronsSpellbooks.id("textures/entity/lens_flare.png");
    private final ModelPart body;

    public ServantGazeProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        body = context.bakeLayer(FireballRenderer.MODEL_LAYER_LOCATION).getChild("body");
    }

    @Override
    public void render(ServantGazeProjectileEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        var motion = entity.getDeltaMovement();
        var xRot = -((float) (Mth.atan2(motion.horizontalDistance(), motion.y) * 180.0 / Math.PI) - 90.0F);
        var yRot = -((float) (Mth.atan2(motion.z, motion.x) * 180.0 / Math.PI) + 90.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        poseStack.scale(0.35F, 0.35F, 0.35F);
        body.render(poseStack, buffers.getBuffer(RenderType.energySwirl(TEXTURE, 0, 0)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.8F, 0.8F, 0.8F, 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(0.5F + Mth.sin(entity.tickCount + partialTick) * 0.125F,
                0.5F + Mth.sin(entity.tickCount + partialTick) * 0.125F,
                0.5F + Mth.sin(entity.tickCount + partialTick) * 0.125F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees((entity.tickCount + partialTick) * 15.0F));
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(FLARE));
        vertex(consumer, matrix, -1, -1, 0, 1);
        vertex(consumer, matrix, 1, -1, 0, 0);
        vertex(consumer, matrix, 1, 1, 1, 0);
        vertex(consumer, matrix, -1, 1, 1, 1);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float y, float z, float u, float v) {
        consumer.vertex(matrix, 0, y, z).color(255, 180, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 1, 0).endVertex();
    }

    @Override public @NotNull ResourceLocation getTextureLocation(@NotNull ServantGazeProjectileEntity entity) { return TEXTURE; }
}
