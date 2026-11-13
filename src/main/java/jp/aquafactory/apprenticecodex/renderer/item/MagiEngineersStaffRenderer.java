package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import jp.aquafactory.apprenticecodex.model.MagiEngineersStaffModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class MagiEngineersStaffRenderer extends GeoItemRenderer<MagiEngineersStaff> {
    public MagiEngineersStaffRenderer() {
        super(new MagiEngineersStaffModel());
    }
}
