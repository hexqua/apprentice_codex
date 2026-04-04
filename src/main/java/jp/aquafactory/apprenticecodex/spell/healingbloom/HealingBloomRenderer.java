package jp.aquafactory.apprenticecodex.spell.healingbloom;

import jp.aquafactory.apprenticecodex.model.HealingBloomModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HealingBloomRenderer extends GeoEntityRenderer<HealingBloomEntity> {
    public HealingBloomRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HealingBloomModel());
        shadowRadius = 0.5f;
    }
}
