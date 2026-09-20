package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class FullautoRapidcastSpellrifleModel extends GeoModel<FullautoRapidcastSpellrifle> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/fullauto_rapidcast_spellrifle.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/fullauto_rapidcast_spellrifle.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/fullauto_rapidcast_spellrifle.animation.json");

    @Override
    public ResourceLocation getModelResource(FullautoRapidcastSpellrifle animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FullautoRapidcastSpellrifle animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FullautoRapidcastSpellrifle animatable) {
        return ANIMATION;
    }
}
