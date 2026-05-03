package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ChromaticMagiaDressModel extends GeoModel<ChromaticMagiaDressItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/chromatic_magia_dress.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/chromatic_magia_dress.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/chromatic_magia_dress.animation.json");

    @Override
    public ResourceLocation getModelResource(ChromaticMagiaDressItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ChromaticMagiaDressItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ChromaticMagiaDressItem animatable) {
        return ANIMATION;
    }
}
