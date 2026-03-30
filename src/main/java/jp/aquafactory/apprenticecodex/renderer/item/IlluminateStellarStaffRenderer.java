package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.IlluminateStellarStaff;
import jp.aquafactory.apprenticecodex.model.IlluminateStellarStaffModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class IlluminateStellarStaffRenderer extends GeoItemRenderer<IlluminateStellarStaff> {
    private static final String BLADE_BONE = "blade";
    private static final String GLOW_CORE_BONE = "glow_core";
    private static final ResourceLocation STAFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/illuminate_stellar_staff.png");
    private static final RenderType ORB_ADDITIVE_RENDER_TYPE =
            ApprenticeRenderTypes.additiveEntityNoCull("illuminate_stellar_staff_orb_additive", STAFF_TEXTURE);

    private float bladeGlowAlpha = 0.0f;
    private float glowCoreBrightness = 1.0f;

    public IlluminateStellarStaffRenderer() {
        super(new IlluminateStellarStaffModel());
    }

    @Override
    public void preRender(PoseStack poseStack, IlluminateStellarStaff animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        this.bladeGlowAlpha = resolveBladeGlowAlpha(partialTick);
        this.glowCoreBrightness = resolveGlowCoreBrightness(partialTick);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, IlluminateStellarStaff animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (isBoneOrChildOf(bone, GLOW_CORE_BONE)) {
            var emissiveRenderType = RenderType.entityTranslucent(STAFF_TEXTURE);
            float emissiveBrightness = this.glowCoreBrightness;
            super.renderRecursively(
                    poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    red * emissiveBrightness, green * emissiveBrightness, blue * emissiveBrightness, alpha
            );
            return;
        }

        if (isBoneOrChildOf(bone, BLADE_BONE)) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );

            if (this.bladeGlowAlpha > 0.0f) {
                // blade 本体の glint は base pass に残し、発光だけを別パスで薄く重ねる。
                var emissiveRenderType = RenderType.entityTranslucent(STAFF_TEXTURE);
                super.renderRecursively(
                        poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                        isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                        red, green, blue, alpha * this.bladeGlowAlpha
                );
            }
            return;
        }

        if (isOrbBoneOrChild(bone)) {
            super.renderRecursively(
                    poseStack, animatable, bone, ORB_ADDITIVE_RENDER_TYPE, bufferSource, bufferSource.getBuffer(ORB_ADDITIVE_RENDER_TYPE),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.bladeGlowAlpha = 0.0f;
        this.glowCoreBrightness = 1.0f;
    }

    private float resolveBladeGlowAlpha(float partialTick) {
        if (isStaticPerspective(this.renderPerspective)) {
            return 0.0f;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            return 0.0f;
        }

        float time = level.getGameTime() + partialTick;
        return 0.14f + 0.06f * (0.5f + 0.5f * Mth.sin(time * 0.09f + 0.6f));
    }

    private float resolveGlowCoreBrightness(float partialTick) {
        if (isStaticPerspective(this.renderPerspective)) {
            return 1.0f;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            return 1.0f;
        }

        float time = level.getGameTime() + partialTick;
        return 0.90f + 0.10f * (0.5f + 0.5f * Mth.sin(time * 0.16f));
    }

    private static boolean isStaticPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.GUI
                || perspective == ItemDisplayContext.GROUND
                || perspective == ItemDisplayContext.FIXED;
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isOrbBoneOrChild(GeoBone bone) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (isOrbBoneName(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isOrbBoneName(String boneName) {
        return boneName.length() == 4
                && boneName.startsWith("orb")
                && Character.isDigit(boneName.charAt(3));
    }
}
