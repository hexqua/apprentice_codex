package jp.aquafactory.apprenticecodex.renderer.armor;

import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.model.StealthRuneArmorModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class StealthRuneArmorRenderer extends GeoArmorRenderer<StealthRuneArmorItem> {
    public StealthRuneArmorRenderer() {
        super(new StealthRuneArmorModel());
    }
}
