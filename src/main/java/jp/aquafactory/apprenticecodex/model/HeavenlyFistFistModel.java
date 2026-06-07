package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistFistEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HeavenlyFistFistModel extends GeoModel<HeavenlyFistFistEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/heavenly_fist_fist.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/heavenly_fist_fist.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/heavenly_fist_fist.animation.json");

    @Override
    public ResourceLocation getModelResource(HeavenlyFistFistEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HeavenlyFistFistEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HeavenlyFistFistEntity animatable) {
        return ANIMATION;
    }
}
