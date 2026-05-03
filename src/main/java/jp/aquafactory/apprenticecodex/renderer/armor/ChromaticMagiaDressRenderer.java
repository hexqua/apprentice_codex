package jp.aquafactory.apprenticecodex.renderer.armor;

import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressItem;
import jp.aquafactory.apprenticecodex.model.ChromaticMagiaDressModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ChromaticMagiaDressRenderer extends GeoArmorRenderer<ChromaticMagiaDressItem> {
    public ChromaticMagiaDressRenderer() {
        super(new ChromaticMagiaDressModel());
    }
}
