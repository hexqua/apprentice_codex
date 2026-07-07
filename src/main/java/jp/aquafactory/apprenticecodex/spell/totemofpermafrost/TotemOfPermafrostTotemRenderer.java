package jp.aquafactory.apprenticecodex.spell.totemofpermafrost;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.model.TotemOfPermafrostTotemModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TotemOfPermafrostTotemRenderer extends GeoEntityRenderer<TotemOfPermafrostTotemEntity> {
    private static final String ICE_BONE = "ice";
    private float iceGlowStrength = 0.0f;

    public TotemOfPermafrostTotemRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TotemOfPermafrostTotemModel());
        shadowRadius = 0.35f;
    }

    @Override
    public void render(@NotNull TotemOfPermafrostTotemEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        iceGlowStrength = entity.getIceGlowStrength(partialTicks);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        iceGlowStrength = 0.0f;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, TotemOfPermafrostTotemEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (isBoneOrChildOf(bone, ICE_BONE)) {
            var glow = Mth.clamp(iceGlowStrength, 0.0f, 1.0f);
            if (glow <= 0.0f) {
                return;
            }

            var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            super.renderRecursively(
                    poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    red * glow, green * glow, blue * glow, alpha * glow
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }
}
