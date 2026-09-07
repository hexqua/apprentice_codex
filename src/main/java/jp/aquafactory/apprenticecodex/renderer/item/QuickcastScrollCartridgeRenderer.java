package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastScrollCartridge;
import jp.aquafactory.apprenticecodex.model.QuickcastScrollCartridgeModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class QuickcastScrollCartridgeRenderer extends GeoItemRenderer<QuickcastScrollCartridge> {
    public QuickcastScrollCartridgeRenderer() { super(new QuickcastScrollCartridgeModel()); }
}
