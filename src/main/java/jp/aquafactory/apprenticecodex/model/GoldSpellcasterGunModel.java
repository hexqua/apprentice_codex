package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellgun.GoldSpellcasterGun;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GoldSpellcasterGunModel extends GeoModel<GoldSpellcasterGun> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/spellcaster_gun_common.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/gold_spellcaster_gun.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/spellcaster_gun_common.animation.json");

    @Override
    public ResourceLocation getModelResource(GoldSpellcasterGun animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GoldSpellcasterGun animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GoldSpellcasterGun animatable) {
        return ANIMATION;
    }
}
