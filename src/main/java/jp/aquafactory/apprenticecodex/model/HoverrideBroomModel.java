package jp.aquafactory.apprenticecodex.model;

import software.bernie.geckolib.animatable.GeoAnimatable;

public final class HoverrideBroomModel<T extends GeoAnimatable> extends BroomModel<T> {
    public HoverrideBroomModel() {
        super("hoverride_broom");
    }
}
