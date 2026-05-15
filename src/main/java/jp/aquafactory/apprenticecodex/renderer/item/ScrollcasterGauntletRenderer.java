package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.model.ScrollcasterGauntletModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ScrollcasterGauntletRenderer extends GeoItemRenderer<ScrollcasterGauntlet> {
    public ScrollcasterGauntletRenderer() {
        super(new ScrollcasterGauntletModel());
    }
}
