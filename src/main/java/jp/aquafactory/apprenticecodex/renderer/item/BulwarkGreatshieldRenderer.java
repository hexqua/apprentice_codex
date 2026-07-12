package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import jp.aquafactory.apprenticecodex.model.BulwarkGreatshieldModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BulwarkGreatshieldRenderer extends GeoItemRenderer<BulwarkGreatshield> {
    public BulwarkGreatshieldRenderer() {
        super(new BulwarkGreatshieldModel());
    }
}
