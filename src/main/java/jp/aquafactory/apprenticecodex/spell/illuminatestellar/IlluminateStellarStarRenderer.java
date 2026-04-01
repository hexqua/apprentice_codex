package jp.aquafactory.apprenticecodex.spell.illuminatestellar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class IlluminateStellarStarRenderer extends EntityRenderer<IlluminateStellarStarEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/illuminate_stellar_star.png");
    private static final float HALF_PIXEL = 1.0f / 32.0f;
    private static final float STAR_RENDER_SCALE = 0.33f;

    public IlluminateStellarStarRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull IlluminateStellarStarEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(STAR_RENDER_SCALE, STAR_RENDER_SCALE, STAR_RENDER_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSpinDegrees(partialTicks)));
        // 16x16 のキャンバスに 15x15 の絵が載っているため、半ピクセルだけ補正して回転軸の見た目を揃える。
        poseStack.translate(-HALF_PIXEL, -HALF_PIXEL, 0.0f);
        // 星は周囲照度に依存させず、spell の発光物として一定の明るさで見せる。
        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(
                poseStack,
                buffer,
                packedLight,
                getTextureLocation(entity),
                ExtrudedSpriteRenderer.RenderMode.EMISSIVE
        );
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull IlluminateStellarStarEntity entity) {
        return TEXTURE;
    }
}
