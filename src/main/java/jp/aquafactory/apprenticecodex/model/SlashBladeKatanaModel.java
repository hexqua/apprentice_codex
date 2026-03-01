package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class SlashBladeKatanaModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/slash_blade_katana.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/slash_blade_katana.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/slash_blade_katana.animation.json");

    @Override
    public ResourceLocation getModelResource(T animatable) { return MODEL; }

    @Override
    public ResourceLocation getTextureResource(T animatable) { return TEX; }

    @Override
    public ResourceLocation getAnimationResource(T animatable) { return ANIM; }
}
