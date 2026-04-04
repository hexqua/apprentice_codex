package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HealingBloomModel extends GeoModel<HealingBloomEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/healing_bloom.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/healing_bloom.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/healing_bloom.animation.json");

    @Override
    public ResourceLocation getModelResource(HealingBloomEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HealingBloomEntity animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(HealingBloomEntity animatable) {
        return ANIM;
    }
}
