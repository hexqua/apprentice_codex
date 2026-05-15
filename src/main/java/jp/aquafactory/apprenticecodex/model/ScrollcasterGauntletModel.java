package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ScrollcasterGauntletModel extends GeoModel<ScrollcasterGauntlet> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/scrollcaster_gauntlet.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/scrollcaster_gauntlet.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/scrollcaster_gauntlet.animation.json");

    @Override
    public ResourceLocation getModelResource(ScrollcasterGauntlet animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ScrollcasterGauntlet animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ScrollcasterGauntlet animatable) {
        return ANIMATION;
    }
}
