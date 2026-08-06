package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.FloatmountBroomItem;
import jp.aquafactory.apprenticecodex.model.FloatmountBroomModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class FloatmountBroomItemRenderer extends GeoItemRenderer<FloatmountBroomItem> {
    public FloatmountBroomItemRenderer() {
        super(new FloatmountBroomModel<>());
    }
}
