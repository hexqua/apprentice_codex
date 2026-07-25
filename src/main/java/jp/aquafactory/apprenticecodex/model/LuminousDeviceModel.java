package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class LuminousDeviceModel extends GeoModel<LuminousDevice> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/luminous_device.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/luminous_device.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/luminous_device.animation.json");

    @Override
    public ResourceLocation getModelResource(LuminousDevice animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(LuminousDevice animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(LuminousDevice animatable) {
        return ANIMATION;
    }
}
