package jp.aquafactory.apprenticecodex.spell.companiontrunk;

import jp.aquafactory.apprenticecodex.model.CompanionTrunkModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CompanionTrunkRenderer extends GeoEntityRenderer<CompanionTrunkEntity> {
    public CompanionTrunkRenderer(EntityRendererProvider.Context context) {
        super(context, new CompanionTrunkModel());
        shadowRadius = 0.5f;
    }
}
