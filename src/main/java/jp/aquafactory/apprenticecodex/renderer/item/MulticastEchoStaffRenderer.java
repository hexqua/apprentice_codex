package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.model.MulticastEchoStaffModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MulticastEchoStaffRenderer extends GeoItemRenderer<MulticastEchoStaff> {
    public MulticastEchoStaffRenderer() {
        super(new MulticastEchoStaffModel());
    }
}
