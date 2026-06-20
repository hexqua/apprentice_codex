package jp.aquafactory.apprenticecodex.renderer.armor;

import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem;
import jp.aquafactory.apprenticecodex.model.MagiAgentSuitModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class MagiAgentSuitRenderer extends GeoArmorRenderer<MagiAgentSuitItem> {
    public MagiAgentSuitRenderer() {
        super(new MagiAgentSuitModel());
    }
}
