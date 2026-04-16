package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ElementalBowModel extends GeoModel<ElementalBow> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/elemental_bow.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/elemental_bow.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/elemental_bow.animation.json");

    @Override
    public ResourceLocation getModelResource(ElementalBow animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ElementalBow animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ElementalBow animatable) {
        return ANIMATION;
    }
}
