package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SmashcastScepter;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SmashcastScepterModel extends GeoModel<SmashcastScepter> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/smashcast_scepter.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/smashcast_scepter.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/smashcast_scepter.animation.json");

    @Override
    public ResourceLocation getModelResource(SmashcastScepter animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SmashcastScepter animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SmashcastScepter animatable) {
        return ANIMATION;
    }
}
