package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.model.ParrycastBucklerModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ParrycastBucklerRenderer extends GeoItemRenderer<ParrycastBuckler> {
    public ParrycastBucklerRenderer() { super(new ParrycastBucklerModel()); }
}
