package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.model.CrystalBladedStaffModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CrystalBladedStaffRenderer extends GeoItemRenderer<CrystalBladedStaff> {
    private static final String BLADE_BONE = "blade";
    private static final String MANA_ORBIT_BONE = "mana_orbit";
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;

    public CrystalBladedStaffRenderer() {
        super(new CrystalBladedStaffModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, CrystalBladedStaff animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (isBoneOrChildOf(bone, MANA_ORBIT_BONE)) {
            // mana_orbit 系は暗さの影響を受けない半透明発光として描画する。
            var translucentRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            var translucentBuffer = getFoilAwareBuffer(bufferSource, translucentRenderType);
            super.renderRecursively(
                    poseStack, animatable, bone, translucentRenderType, bufferSource, translucentBuffer, isReRender, partialTick,
                    FULL_BRIGHT_LIGHT, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (isBoneOrChildOf(bone, BLADE_BONE)) {
            var translucentRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            var translucentBuffer = getFoilAwareBuffer(bufferSource, translucentRenderType);
            super.renderRecursively(
                    poseStack, animatable, bone, translucentRenderType, bufferSource, translucentBuffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    private VertexConsumer getFoilAwareBuffer(MultiBufferSource bufferSource, RenderType renderType) {
        return ItemRenderer.getFoilBufferDirect(
                bufferSource,
                renderType,
                this.renderPerspective == ItemDisplayContext.GUI,
                this.currentItemStack != null && this.currentItemStack.hasFoil()
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
