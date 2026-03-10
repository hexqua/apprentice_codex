package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.spellgun.GoldSpellcasterGun;
import jp.aquafactory.apprenticecodex.model.GoldSpellcasterGunModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GoldSpellcasterGunRenderer extends GeoItemRenderer<GoldSpellcasterGun> {
    public GoldSpellcasterGunRenderer() {
        super(new GoldSpellcasterGunModel());
    }
}
