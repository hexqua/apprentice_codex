package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MultipurposeStaffrifleModel extends GeoModel<MultipurposeStaffrifle> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/multipurpose_staffrifle.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/multipurpose_staffrifle.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/multipurpose_staffrifle.animation.json");

    @Override
    public ResourceLocation getModelResource(MultipurposeStaffrifle animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MultipurposeStaffrifle animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MultipurposeStaffrifle animatable) {
        return ANIMATION;
    }
}
