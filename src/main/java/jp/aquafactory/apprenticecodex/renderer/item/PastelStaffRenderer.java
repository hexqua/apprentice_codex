package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.model.PastelStaffModel;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PastelStaffRenderer extends GeoItemRenderer<PastelStaff> {
    private static final String STONE_TINT_BONE = "stone_tint";

    private float stoneRed = 1.0f;
    private float stoneGreen = 1.0f;
    private float stoneBlue = 1.0f;

    public PastelStaffRenderer() {
        super(new PastelStaffModel());
    }

    @Override
    public void preRender(PoseStack poseStack, PastelStaff animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        int stoneColor = animatable.getStoneTintColor(getCurrentItemStack());
        this.stoneRed = ((stoneColor >> 16) & 0xFF) / 255.0f;
        this.stoneGreen = ((stoneColor >> 8) & 0xFF) / 255.0f;
        this.stoneBlue = (stoneColor & 0xFF) / 255.0f;
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, int colour) {
        if (STONE_TINT_BONE.equals(bone.getName())) {
            var alpha = (colour >>> 24) & 0xFF;
            var red = (colour >>> 16) & 0xFF;
            var green = (colour >>> 8) & 0xFF;
            var blue = colour & 0xFF;
            red = Math.round(red * this.stoneRed);
            green = Math.round(green * this.stoneGreen);
            blue = Math.round(blue * this.stoneBlue);
            colour = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.stoneRed = 1.0f;
        this.stoneGreen = 1.0f;
        this.stoneBlue = 1.0f;
    }
}
