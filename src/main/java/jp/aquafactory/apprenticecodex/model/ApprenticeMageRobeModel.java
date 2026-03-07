package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.ApprenticeMageRobeItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ApprenticeMageRobeModel extends GeoModel<ApprenticeMageRobeItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/apprentice_mage.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/apprentice_mage.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/apprentice_mage.animation.json");

    @Override
    public ResourceLocation getModelResource(ApprenticeMageRobeItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ApprenticeMageRobeItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ApprenticeMageRobeItem animatable) {
        return ANIMATION;
    }
}
