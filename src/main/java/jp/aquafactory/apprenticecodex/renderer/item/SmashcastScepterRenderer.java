package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.SmashcastScepter;
import jp.aquafactory.apprenticecodex.model.SmashcastScepterModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SmashcastScepterRenderer extends GeoItemRenderer<SmashcastScepter> {
    public SmashcastScepterRenderer() {
        super(new SmashcastScepterModel());
    }
}
