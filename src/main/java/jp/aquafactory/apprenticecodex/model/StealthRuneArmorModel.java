package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StealthRuneArmorModel extends GeoModel<StealthRuneArmorItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/stealth_rune_armor.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/stealth_rune_armor.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/stealth_rune_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(StealthRuneArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(StealthRuneArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(StealthRuneArmorItem animatable) {
        return ANIMATION;
    }
}
