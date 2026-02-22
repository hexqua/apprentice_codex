package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.model.PastelStaffModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PastelStaffRenderer extends GeoItemRenderer<PastelStaff> {
    public PastelStaffRenderer() {
        super(new PastelStaffModel());
    }
}
