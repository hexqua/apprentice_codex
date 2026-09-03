package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScythe;
import jp.aquafactory.apprenticecodex.model.SpellReaperScytheModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SpellReaperScytheRenderer extends GeoItemRenderer<SpellReaperScythe> {
    public SpellReaperScytheRenderer() {
        super(new SpellReaperScytheModel());
    }
}
