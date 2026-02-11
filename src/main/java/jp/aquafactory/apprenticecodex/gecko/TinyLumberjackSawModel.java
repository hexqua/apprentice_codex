package jp.aquafactory.apprenticecodex.gecko;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class TinyLumberjackSawModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "geo/tiny_lumberjack_saw.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "textures/item/tiny_lumberjack_saw.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "animations/tiny_lumberjack_saw.animation.json");

    @Override
    public ResourceLocation getModelResource(T animatable) { return MODEL; }

    @Override
    public ResourceLocation getTextureResource(T animatable) { return TEX; }

    @Override
    public ResourceLocation getAnimationResource(T animatable) { return ANIM; }
}
