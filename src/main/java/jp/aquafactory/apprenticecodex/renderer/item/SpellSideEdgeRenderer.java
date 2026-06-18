package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdge;
import jp.aquafactory.apprenticecodex.model.SpellSideEdgeModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SpellSideEdgeRenderer extends GeoItemRenderer<SpellSideEdge> {
    public SpellSideEdgeRenderer() {
        super(new SpellSideEdgeModel());
    }
}
