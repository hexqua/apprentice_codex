package jp.aquafactory.apprenticecodex.spell.lunaraim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.renderer.LunarCubeRenderer;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class LunarAimArrowRenderer extends EntityRenderer<LunarAimArrowEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/entity/lunar_aim_arrow.png");
    private static final RenderType BURST = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
            "unite_luna_moon_additive", ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/unite_luna_moon.png"));

    public LunarAimArrowRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }

    @Override public void render(LunarAimArrowEntity entity, float yaw, float partialTick, @NotNull PoseStack pose,
                                 @NotNull MultiBufferSource buffers, int light) {
        pose.pushPose();
        if (entity.isBursting()) {
            float age = entity.getBurstAge(partialTick);
            float progress = Mth.clamp(age / 5, 0, 1);
            float scale = Mth.lerp(1 - (float) Math.pow(1 - progress, 3), 1, entity.getRadius() * 2);
            float fade = Mth.clamp((age - 10) / 10, 0, 1);
            float alpha = 1 - fade * fade * fade;
            pose.mulPose(Axis.YP.rotationDegrees(age * -27));
            LunarCubeRenderer.drawCube(pose, buffers.getBuffer(BURST), scale, alpha, 0.88f, 0.96f, 1, true);
        } else {
            var direction = entity.getDeltaMovement();
            pose.mulPose(Axis.YP.rotationDegrees((float) (-Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG + 90)));
            pose.mulPose(Axis.XP.rotationDegrees((float) (Mth.atan2(direction.horizontalDistance(), direction.y) * Mth.RAD_TO_DEG - 90)));
            // 共有モデルの矢先は-Z。手元の向きを変えず飛行描画だけ反転する。
            pose.mulPose(Axis.YP.rotationDegrees(180));
            LightningArrowRenderer.renderModel(pose, buffers, TEXTURE);
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    public static void renderCharge(PoseStack pose, MultiBufferSource buffers) {
        for (int i = 0; i < 4; i++) {
            pose.pushPose();
            pose.translate((i - 1.5) * 0.10, 0, 0);
            LightningArrowRenderer.renderModel(pose, buffers, TEXTURE);
            pose.popPose();
        }
    }

    @Override public @NotNull ResourceLocation getTextureLocation(@NotNull LunarAimArrowEntity entity) { return TEXTURE; }
}
