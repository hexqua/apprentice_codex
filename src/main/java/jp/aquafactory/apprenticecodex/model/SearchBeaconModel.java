package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SearchBeaconModel extends GeoModel<SearchBeaconEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/search_beacon_brazier.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/search_beacon_brazier.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/search_beacon_brazier.animation.json");

    @Override
    public ResourceLocation getModelResource(SearchBeaconEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SearchBeaconEntity animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(SearchBeaconEntity animatable) {
        return ANIM;
    }
}
