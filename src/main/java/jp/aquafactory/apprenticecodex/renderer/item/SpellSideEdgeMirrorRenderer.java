package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeMirror;
import jp.aquafactory.apprenticecodex.model.SpellSideEdgeMirrorModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SpellSideEdgeMirrorRenderer extends GeoItemRenderer<SpellSideEdgeMirror> {
    public SpellSideEdgeMirrorRenderer() {
        super(new SpellSideEdgeMirrorModel());
    }
}
