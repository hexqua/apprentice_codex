package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellgun.IronSpellcasterGun;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class IronSpellcasterGunModel extends GeoModel<IronSpellcasterGun> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/spellcaster_gun_common.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/iron_spellcaster_gun.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/spellcaster_gun_common.animation.json");

    @Override
    public ResourceLocation getModelResource(IronSpellcasterGun animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(IronSpellcasterGun animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(IronSpellcasterGun animatable) {
        return ANIMATION;
    }
}
