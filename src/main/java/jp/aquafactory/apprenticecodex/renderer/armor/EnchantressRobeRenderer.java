package jp.aquafactory.apprenticecodex.renderer.armor;

import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.model.EnchantressRobeModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class EnchantressRobeRenderer extends GeoArmorRenderer<EnchantressRobeItem> {
    public EnchantressRobeRenderer() {
        super(new EnchantressRobeModel());
    }
}
