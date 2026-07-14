package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class FocusStaffbowModel extends GeoModel<FocusStaffbow> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/focus_staffbow.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/focus_staffbow.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/focus_staffbow.animation.json");

    @Override
    public ResourceLocation getModelResource(FocusStaffbow animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FocusStaffbow animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(FocusStaffbow animatable) {
        return ANIM;
    }
}
