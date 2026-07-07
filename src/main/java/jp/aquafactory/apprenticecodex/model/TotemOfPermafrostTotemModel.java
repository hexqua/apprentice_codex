package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.totemofpermafrost.TotemOfPermafrostTotemEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TotemOfPermafrostTotemModel extends GeoModel<TotemOfPermafrostTotemEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/totem_of_permafrost_totem.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/totem_of_permafrost_totem.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/totem_of_permafrost_totem.animation.json");

    @Override
    public ResourceLocation getModelResource(TotemOfPermafrostTotemEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TotemOfPermafrostTotemEntity animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(TotemOfPermafrostTotemEntity animatable) {
        return ANIM;
    }
}
