package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.UniteLunaStaff;
import jp.aquafactory.apprenticecodex.model.UniteLunaStaffModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class UniteLunaStaffRenderer extends GeoItemRenderer<UniteLunaStaff> {
    private static final String BLADE_BONE = "blade";
    private static final String MOON_BONE = "moon";
    private static final String SUN_BONE = "sun";
    private static final ResourceLocation STAFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/unite_luna_staff.png");

    private float bladeGlowAlpha = 0.0f;
    private float moonBrightness = 1.0f;

    public UniteLunaStaffRenderer() {
        super(new UniteLunaStaffModel());
    }

    @Override
    public void preRender(PoseStack poseStack, UniteLunaStaff animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        this.bladeGlowAlpha = resolveBladeGlowAlpha(partialTick);
        this.moonBrightness = resolveMoonBrightness();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, UniteLunaStaff animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (isBoneOrChildOf(bone, MOON_BONE)) {
            var emissiveRenderType = RenderType.entityTranslucent(STAFF_TEXTURE);
            super.renderRecursively(
                    poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    red * this.moonBrightness, green * this.moonBrightness, blue * this.moonBrightness, alpha
            );
            return;
        }

        if (isBoneOrChildOf(bone, SUN_BONE)) {
            var emissiveRenderType = RenderType.entityTranslucent(STAFF_TEXTURE);
            super.renderRecursively(
                    poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (isBoneOrChildOf(bone, BLADE_BONE)) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );

            if (this.bladeGlowAlpha > 0.0f) {
                // blade 本体は通常描画を残し、薄い発光だけを別パスで重ねる。
                var emissiveRenderType = RenderType.entityTranslucent(STAFF_TEXTURE);
                super.renderRecursively(
                        poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                        isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                        red, green, blue, alpha * this.bladeGlowAlpha
                );
            }
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
        this.moonBrightness = 1.0f;
    }

    private float resolveBladeGlowAlpha(float partialTick) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 0.0f;
        }

        float time = level.getGameTime() + partialTick;
        return 0.14f + 0.06f * (0.5f + 0.5f * Mth.sin(time * 0.09f + 0.6f));
    }

    private float resolveMoonBrightness() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 1.0f;
        }

        int moonPhase = level.getMoonPhase();
        int distanceFromFull = Math.min(moonPhase, 8 - moonPhase);
        return Mth.lerp(distanceFromFull / 4.0f, 1.0f, 0.25f);
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
