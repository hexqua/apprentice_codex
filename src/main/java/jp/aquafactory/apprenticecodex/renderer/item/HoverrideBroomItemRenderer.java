package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.broom.HoverrideBroomItem;
import jp.aquafactory.apprenticecodex.model.HoverrideBroomModel;

public final class HoverrideBroomItemRenderer extends BroomItemRenderer<HoverrideBroomItem> {
    public HoverrideBroomItemRenderer() {
        super(new HoverrideBroomModel<>());
    }
}
