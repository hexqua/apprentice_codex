package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.spellgun.DiamondSpellcasterGun;
import jp.aquafactory.apprenticecodex.model.DiamondSpellcasterGunModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DiamondSpellcasterGunRenderer extends GeoItemRenderer<DiamondSpellcasterGun> {
    public DiamondSpellcasterGunRenderer() {
        super(new DiamondSpellcasterGunModel());
    }
}
