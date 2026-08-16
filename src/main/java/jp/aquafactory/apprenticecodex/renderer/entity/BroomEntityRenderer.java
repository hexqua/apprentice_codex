package jp.aquafactory.apprenticecodex.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.model.BroomModel;
import jp.aquafactory.apprenticecodex.renderer.BroomRenderSupport;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class BroomEntityRenderer<T extends AbstractBroomEntity> extends GeoEntityRenderer<T> {
    protected BroomEntityRenderer(EntityRendererProvider.Context context, BroomModel<T> model) {
        super(context, model);
        shadowRadius = 0.35F;
    }

    @Override
    public boolean shouldShowName(T broom) {
        return !broom.isVehicle() && super.shouldShowName(broom);
    }

    @Override
    protected void applyRotations(T broom, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        // GeoEntityRendererは非LivingEntityのbody yawを0として扱うため、車体自身の補間yawを明示する。
        var broomYaw = Mth.rotLerp(partialTick, broom.yRotO, broom.getYRot());
        super.applyRotations(broom, poseStack, ageInTicks, broomYaw, partialTick, nativeScale);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (BroomRenderSupport.isBoneOrChildOf(bone, BroomRenderSupport.STAR_BONE)) {
            renderEmissiveBone(poseStack, animatable, bone, bufferSource, isReRender, partialTick, packedOverlay,
                    BroomRenderSupport.resolveStarColour(partialTick));
            return;
        }

        if (BroomRenderSupport.isBoneOrChildOf(bone, BroomRenderSupport.CORE_BONE)) {
            renderEmissiveBone(poseStack, animatable, bone, bufferSource, isReRender, partialTick, packedOverlay,
                    BroomRenderSupport.resolveEntityCoreColour(animatable, partialTick));
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    private void renderEmissiveBone(PoseStack poseStack, T animatable, GeoBone bone,
                                    MultiBufferSource bufferSource, boolean isReRender, float partialTick,
                                    int packedOverlay, int colour) {
        var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
        super.renderRecursively(
                poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, colour
        );
    }

    @Override
    public Color getRenderColor(T broom, float partialTick, int packedLight) {
        var base = super.getRenderColor(broom, partialTick, packedLight);
        var multipliers = switch (broom.getDamageStage()) {
            case 1 -> new float[]{0.90F, 0.78F, 0.78F};
            case 2 -> new float[]{0.78F, 0.55F, 0.55F};
            case 3 -> new float[]{0.66F, 0.34F, 0.34F};
            case 4 -> new float[]{0.52F, 0.18F, 0.18F};
            default -> new float[]{1.0F, 1.0F, 1.0F};
        };
        return Color.ofARGB(
                base.getAlpha(),
                Mth.clamp(Math.round(base.getRed() * multipliers[0]), 0, 255),
                Mth.clamp(Math.round(base.getGreen() * multipliers[1]), 0, 255),
                Mth.clamp(Math.round(base.getBlue() * multipliers[2]), 0, 255)
        );
    }
}
