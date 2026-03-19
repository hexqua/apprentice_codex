package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import jp.aquafactory.apprenticecodex.model.ExplorersCaneModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ExplorersCaneRenderer extends GeoItemRenderer<ExplorersCane> {
    public ExplorersCaneRenderer() {
        super(new ExplorersCaneModel());
    }
}
