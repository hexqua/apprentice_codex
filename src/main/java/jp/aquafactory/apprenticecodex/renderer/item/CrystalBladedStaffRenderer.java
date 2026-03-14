package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.model.CrystalBladedStaffModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CrystalBladedStaffRenderer extends GeoItemRenderer<CrystalBladedStaff> {
    public CrystalBladedStaffRenderer() {
        super(new CrystalBladedStaffModel());
    }
}
