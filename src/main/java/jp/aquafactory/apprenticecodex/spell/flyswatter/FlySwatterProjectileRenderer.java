package jp.aquafactory.apprenticecodex.spell.flyswatter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ShulkerBulletModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class FlySwatterProjectileRenderer extends EntityRenderer<FlySwatterProjectileEntity> {
    // シュルカーを流用.
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/shulker/spark.png");

    private final ShulkerBulletModel<FlySwatterProjectileEntity> model;

    public FlySwatterProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        model = new ShulkerBulletModel<>(ctx.bakeLayer(ModelLayers.SHULKER_BULLET));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(FlySwatterProjectileEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        var yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        var xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        var roll = (entity.tickCount + partialTicks) * 20.0F;
        var vc = buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);
        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FlySwatterProjectileEntity pEntity) {
        return TEXTURE;
    }
}
