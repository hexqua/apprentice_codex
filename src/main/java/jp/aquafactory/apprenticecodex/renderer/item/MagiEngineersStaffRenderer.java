package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import jp.aquafactory.apprenticecodex.model.MagiEngineersStaffModel;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class MagiEngineersStaffRenderer extends GeoItemRenderer<MagiEngineersStaff> {
    private static final long CATALYST_PULSE_PERIOD_MILLIS = 4000;

    private boolean renderingCatalyst;

    public MagiEngineersStaffRenderer() {
        super(new MagiEngineersStaffModel());
    }

    @Override
    public void postRender(PoseStack poseStack, MagiEngineersStaff animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        if (isReRender) {
            return;
        }

        // 通常パスから分離し、テクスチャの半透明と発光を維持しつつ Glint を重ねない。
        RenderType renderType = RenderType.entityTranslucentEmissive(getTextureLocation(animatable));
        double phase = (Util.getMillis() % CATALYST_PULSE_PERIOD_MILLIS)
                * (Math.PI * 2 / CATALYST_PULSE_PERIOD_MILLIS);
        int intensity = (int) Math.round(255 * (0.975 + 0.025 * Math.cos(phase)));
        int pulseColour = FastColor.ARGB32.multiply(colour,
                FastColor.ARGB32.color(255, intensity, intensity, intensity));

        this.renderingCatalyst = true;
        try {
            reRender(model, poseStack, bufferSource, animatable, renderType, bufferSource.getBuffer(renderType),
                    partialTick, LightTexture.FULL_BRIGHT, packedOverlay, pulseColour);
        } finally {
            this.renderingCatalyst = false;
        }
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                  int packedLight, int packedOverlay, int colour) {
        boolean catalyst = isBoneOrChildOf(bone, "catalyst");
        if (catalyst != this.renderingCatalyst) {
            return;
        }

        // cube の描画だけを選別し、親ボーンの変換と子ボーンへの再帰は GeckoLib に任せる。
        if (catalyst || isBoneOrChildOf(bone, "pommel_core") || isBoneOrChildOf(bone, "conductor_core")) {
            packedLight = LightTexture.FULL_BRIGHT;
        }
        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
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
