package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BulwarkGreatshieldModel extends GeoModel<BulwarkGreatshield> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "geo/bulwark_greatshield.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/geo/bulwark_greatshield.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "animations/bulwark_greatshield.animation.json");

    @Override
    public ResourceLocation getModelResource(BulwarkGreatshield animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BulwarkGreatshield animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BulwarkGreatshield animatable) {
        return ANIMATION;
    }
}
