package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ParrycastBucklerModel extends GeoModel<ParrycastBuckler> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/parrycast_buckler.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/parrycast_buckler.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/parrycast_buckler.animation.json");
    @Override public ResourceLocation getModelResource(ParrycastBuckler animatable) { return MODEL; }
    @Override public ResourceLocation getTextureResource(ParrycastBuckler animatable) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(ParrycastBuckler animatable) { return ANIMATION; }
}
