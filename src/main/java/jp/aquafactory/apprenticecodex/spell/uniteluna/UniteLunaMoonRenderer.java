package jp.aquafactory.apprenticecodex.spell.uniteluna;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import static jp.aquafactory.apprenticecodex.renderer.LunarCubeRenderer.drawCube;

public class UniteLunaMoonRenderer extends EntityRenderer<UniteLunaMoonEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/unite_luna_moon.png");
    private static final RenderType SOLID_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType FADE_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("unite_luna_moon_translucent", TEXTURE);
    private static final RenderType ADDITIVE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("unite_luna_moon_additive", TEXTURE);

    public UniteLunaMoonRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull UniteLunaMoonEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSpinDegrees(partialTicks)));

        var mainAlpha = entity.getMainCubeAlpha(partialTicks);
        if (mainAlpha > 0.001f) {
            var mainRenderType = mainAlpha >= 0.995f ? SOLID_RENDER_TYPE : FADE_RENDER_TYPE;
            drawCube(poseStack, bufferSource.getBuffer(mainRenderType), UniteLunaMoonEntity.CUBE_SIZE, mainAlpha, 1.0f, 1.0f, 1.0f, false);
        }

        if (entity.getBurstKind() == UniteLunaMoonEntity.BURST_KIND_EXPLOSION) {
            var burstAlpha = entity.getBurstCubeAlpha(partialTicks);
            if (burstAlpha > 0.001f) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(entity.getBurstSpinDegrees(partialTicks)));
                drawCube(
                        poseStack,
                        bufferSource.getBuffer(ADDITIVE_RENDER_TYPE),
                        entity.getBurstCubeScale(partialTicks),
                        burstAlpha,
                        0.88f,
                        0.96f,
                        1.0f,
                        true
                );
                poseStack.popPose();
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull UniteLunaMoonEntity entity) {
        return TEXTURE;
    }

}
