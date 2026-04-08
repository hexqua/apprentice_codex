package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.model.SearchBeaconModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SearchBeaconRenderer extends GeoEntityRenderer<SearchBeaconEntity> {
    public SearchBeaconRenderer(EntityRendererProvider.Context context) {
        super(context, new SearchBeaconModel());
        shadowRadius = 0.35f;
    }
}
