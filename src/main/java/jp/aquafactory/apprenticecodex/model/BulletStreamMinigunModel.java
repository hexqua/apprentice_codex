package jp.aquafactory.apprenticecodex.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class BulletStreamMinigunModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "geo/bullet_stream_minigun.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "textures/item/bullet_stream_minigun.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "animations/bullet_stream_minigun.animation.json");

    @Override
    public ResourceLocation getModelResource(T animatable) { return MODEL; }

    @Override
    public ResourceLocation getTextureResource(T animatable) { return TEX; }

    @Override
    public ResourceLocation getAnimationResource(T animatable) { return ANIM; }
}
