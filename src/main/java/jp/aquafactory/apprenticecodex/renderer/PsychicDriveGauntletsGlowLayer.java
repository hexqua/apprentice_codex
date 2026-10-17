package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsVisualState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class PsychicDriveGauntletsGlowLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    public static final RenderType GLOW = ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly(
            "psychic_drive_gauntlets_glow", ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID, "textures/geo/psychic_drive_gauntlets.png"));
    private static final float IDLE_STRENGTH = 0.08F;

    public PsychicDriveGauntletsGlowLayer(GeoRenderer<T> renderer) { super(renderer); }

    public static boolean glowing(GeoBone bone) {
        return glowRoot(bone) != null;
    }

    private static String glowRoot(GeoBone bone) {
        for (var current = bone; current != null; current = current.getParent()) {
            if (current.getName().startsWith("glow_")) return current.getName();
        }
        return null;
    }

    public static int baseColour(int colour) {
        // 暗所でも紫の模様を読ませつつ、加算パルスの明るさを残しておく。
        return colour & 0xFF000000
                | Math.round(((colour >> 16) & 255) * 0.65F) << 16
                | Math.round(((colour >> 8) & 255) * 0.65F) << 8
                | Math.round((colour & 255) * 0.65F);
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource buffers, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        var root = glowRoot(bone);
        if (root == null || bone.isHidden()) return;
        float strength = IDLE_STRENGTH;
        if (animatable instanceof PsychicDriveGauntletsFloatingRenderer.Visual visual) {
            strength = visual.glowStrength(root);
        }
        int value = Mth.clamp(Math.round(strength * 255), 0, 255);
        // SRC_ALPHA加算なのでRGBまで弱めず、透明度だけで発光量を制御する。
        int colour = value << 24 | 0xFFFFFF;
        getRenderer().renderCubesOfBone(poseStack, bone, buffers.getBuffer(GLOW),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
        // ボーンごとの別buffer描画後はGeckoLibが続ける元のbufferを復元する。
        buffers.getBuffer(renderType);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType,
                       MultiBufferSource buffers, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        if (!(animatable instanceof PsychicDriveGauntletsFloatingRenderer.Visual visual)
                || visual.auraStrength() <= 0) return;
        float strength = visual.auraStrength();
        int value = Math.round(strength * 255);
        // 薄い全体光膜だけを重ねる。再描画ではrenderForBoneが呼ばれず、発光の再帰を防げる。
        poseStack.pushPose();
        float scale = PsychicDriveGauntletsVisualState.AURA_SCALE;
        poseStack.scale(scale, scale, scale);
        getRenderer().reRender(model, poseStack, buffers, animatable, GLOW, buffers.getBuffer(GLOW),
                partialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                value << 24 | 0xFFFFFF);
        poseStack.popPose();
    }
}
