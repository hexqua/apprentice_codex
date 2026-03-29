package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.IlluminateStellarStaff;
import jp.aquafactory.apprenticecodex.model.IlluminateStellarStaffModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class IlluminateStellarStaffRenderer extends GeoItemRenderer<IlluminateStellarStaff> {
    public IlluminateStellarStaffRenderer() {
        super(new IlluminateStellarStaffModel());
    }
}
