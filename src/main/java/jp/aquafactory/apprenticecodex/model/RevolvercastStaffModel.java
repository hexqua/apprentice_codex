package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.RevolvercastStaff;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RevolvercastStaffModel extends GeoModel<RevolvercastStaff> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/revolvercast_staff.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/revolvercast_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(RevolvercastStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RevolvercastStaff animatable) {
        return animatable.getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(RevolvercastStaff animatable) {
        return ANIMATION;
    }
}
