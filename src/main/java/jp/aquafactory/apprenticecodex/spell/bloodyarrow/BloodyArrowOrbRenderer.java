package jp.aquafactory.apprenticecodex.spell.bloodyarrow;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class BloodyArrowOrbRenderer extends EntityRenderer<BloodyArrowOrbEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/spell/bloody_arrow_orb.png");

    public BloodyArrowOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(BloodyArrowOrbEntity entity, float yaw, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light) {
        pose.pushPose();
        float phase = (entity.tickCount + partialTick) * 0.12F + entity.getId() * 1.7F;
        pose.translate(0, entity.getBbHeight() * 0.5 + Math.sin(phase) * 0.06, 0);
        // 回転するドロップ品ではなく、常に見える薄い魔法の光として描く。
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(0.3F, 0.3F, 0.3F);
        float brightness = 0.8F + 0.2F * (float) Math.sin(phase);
        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(pose, buffers, light, TEXTURE,
                ExtrudedSpriteRenderer.RenderMode.EMISSIVE, brightness, brightness, brightness,
                entity.opacity(partialTick));
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override public @NotNull ResourceLocation getTextureLocation(@NotNull BloodyArrowOrbEntity entity) { return TEXTURE; }
}
