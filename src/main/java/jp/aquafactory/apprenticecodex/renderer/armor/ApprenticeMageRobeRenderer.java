package jp.aquafactory.apprenticecodex.renderer.armor;

import jp.aquafactory.apprenticecodex.item.armor.ApprenticeMageRobeItem;
import jp.aquafactory.apprenticecodex.model.ApprenticeMageRobeModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ApprenticeMageRobeRenderer extends GeoArmorRenderer<ApprenticeMageRobeItem> {
    public ApprenticeMageRobeRenderer() {
        super(new ApprenticeMageRobeModel());
    }
}
