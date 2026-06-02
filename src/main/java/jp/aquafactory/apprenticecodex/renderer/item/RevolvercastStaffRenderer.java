package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.model.RevolvercastStaffModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class RevolvercastStaffRenderer extends GeoItemRenderer<RevolvercastStaff> {
    public RevolvercastStaffRenderer() {
        super(new RevolvercastStaffModel());
    }

    @Override
    public RenderType getRenderType(RevolvercastStaff animatable, net.minecraft.resources.ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }
}
