package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.broom.FloatmountBroomItem;
import jp.aquafactory.apprenticecodex.model.FloatmountBroomModel;

public final class FloatmountBroomItemRenderer extends BroomItemRenderer<FloatmountBroomItem> {
    public FloatmountBroomItemRenderer() {
        super(new FloatmountBroomModel<>());
    }
}
