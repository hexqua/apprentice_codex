package jp.aquafactory.apprenticecodex.renderer.armor;

import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import jp.aquafactory.apprenticecodex.model.ElementMaidenRobeModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ElementMaidenRobeRenderer extends GeoArmorRenderer<ElementMaidenRobeItem> {
    public ElementMaidenRobeRenderer() {
        super(new ElementMaidenRobeModel());
    }
}
