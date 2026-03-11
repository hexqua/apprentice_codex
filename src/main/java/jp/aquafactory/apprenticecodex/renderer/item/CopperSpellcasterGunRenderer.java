package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.spellgun.CopperSpellcasterGun;
import jp.aquafactory.apprenticecodex.model.CopperSpellcasterGunModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CopperSpellcasterGunRenderer extends GeoItemRenderer<CopperSpellcasterGun> {
    public CopperSpellcasterGunRenderer() {
        super(new CopperSpellcasterGunModel());
    }
}
