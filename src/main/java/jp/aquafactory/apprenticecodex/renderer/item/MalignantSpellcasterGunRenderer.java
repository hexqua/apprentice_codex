package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.spellgun.MalignantSpellcasterGun;
import jp.aquafactory.apprenticecodex.model.MalignantSpellcasterGunModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MalignantSpellcasterGunRenderer extends GeoItemRenderer<MalignantSpellcasterGun> {
    public MalignantSpellcasterGunRenderer() {
        super(new MalignantSpellcasterGunModel());
    }
}
