package jp.aquafactory.apprenticecodex.spell.bloodyarrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class BloodyArrowRenderer extends EntityRenderer<BloodyArrowEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/entity/bloody_arrow_arrow.png");

    public BloodyArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(BloodyArrowEntity entity, float yaw, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light) {
        var direction = entity.getDeltaMovement();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) (-Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG + 90)));
        pose.mulPose(Axis.XP.rotationDegrees((float) (Mth.atan2(direction.horizontalDistance(), direction.y) * Mth.RAD_TO_DEG - 90)));
        // 手元描画の向きを維持し、共有モデルの矢先(-Z)を飛行時だけ反転する。
        pose.mulPose(Axis.YP.rotationDegrees(180));
        renderModel(pose, buffers);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    public static void renderModel(PoseStack pose, MultiBufferSource buffers) {
        LightningArrowRenderer.renderModel(pose, buffers, TEXTURE);
    }

    @Override public @NotNull ResourceLocation getTextureLocation(@NotNull BloodyArrowEntity entity) { return TEXTURE; }
}
