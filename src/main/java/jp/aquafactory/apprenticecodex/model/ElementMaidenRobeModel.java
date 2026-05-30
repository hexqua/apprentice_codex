package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ElementMaidenRobeModel extends GeoModel<ElementMaidenRobeItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/element_maiden_robe.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/element_maiden_robe.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/element_maiden_robe.animation.json");

    @Override
    public ResourceLocation getModelResource(ElementMaidenRobeItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ElementMaidenRobeItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ElementMaidenRobeItem animatable) {
        return ANIMATION;
    }
}
