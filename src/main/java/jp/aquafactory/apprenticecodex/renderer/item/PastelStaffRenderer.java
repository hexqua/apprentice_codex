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
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        int stoneColor = animatable.getStoneTintColor(getCurrentItemStack());
        this.stoneRed = ((stoneColor >> 16) & 0xFF) / 255.0f;
        this.stoneGreen = ((stoneColor >> 8) & 0xFF) / 255.0f;
        this.stoneBlue = (stoneColor & 0xFF) / 255.0f;
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        if (STONE_TINT_BONE.equals(bone.getName())) {
            red *= this.stoneRed;
            green *= this.stoneGreen;
            blue *= this.stoneBlue;
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.stoneRed = 1.0f;
        this.stoneGreen = 1.0f;
        this.stoneBlue = 1.0f;
    }
}
