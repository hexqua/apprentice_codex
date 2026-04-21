package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.model.FocusStaffbowModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class FocusStaffbowRenderer extends GeoItemRenderer<FocusStaffbow> {
    public FocusStaffbowRenderer() {
        super(new FocusStaffbowModel());
    }

    @Override
    public RenderType getRenderType(FocusStaffbow animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
