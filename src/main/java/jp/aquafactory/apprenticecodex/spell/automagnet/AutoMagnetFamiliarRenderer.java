package jp.aquafactory.apprenticecodex.spell.automagnet;

import jp.aquafactory.apprenticecodex.model.AutoMagnetFamiliarModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AutoMagnetFamiliarRenderer extends GeoEntityRenderer<AutoMagnetFamiliarEntity> {
    public AutoMagnetFamiliarRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new AutoMagnetFamiliarModel<>());
    }
}
