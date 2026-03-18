package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ReflectcastShieldModel extends GeoModel<ReflectcastShield> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/reflectcast_shield.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/reflectcast_shield.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/reflectcast_shield.animation.json");

    @Override
    public ResourceLocation getModelResource(ReflectcastShield animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ReflectcastShield animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ReflectcastShield animatable) {
        return ANIMATION;
    }
}
