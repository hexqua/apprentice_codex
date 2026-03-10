package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.spellgun.IronSpellcasterGun;
import jp.aquafactory.apprenticecodex.model.IronSpellcasterGunModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class IronSpellcasterGunRenderer extends GeoItemRenderer<IronSpellcasterGun> {
    public IronSpellcasterGunRenderer() {
        super(new IronSpellcasterGunModel());
    }
}
