package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.model.ChargedTwinBladeStaffModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ChargedTwinBladeStaffRenderer extends GeoItemRenderer<ChargedTwinBladeStaff> {
    public ChargedTwinBladeStaffRenderer() {
        super(new ChargedTwinBladeStaffModel());
    }
}
