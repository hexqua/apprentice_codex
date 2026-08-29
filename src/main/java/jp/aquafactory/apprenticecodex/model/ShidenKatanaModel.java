package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class ShidenKatanaModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/shiden_katana.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/shiden_katana.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/shiden_katana.animation.json");

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATION;
    }
}
