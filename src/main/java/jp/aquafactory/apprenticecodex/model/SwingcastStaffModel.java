package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SwingcastStaffModel extends GeoModel<AbstractSwingcastStaffItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/swingcast_staff.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/swingcast_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(AbstractSwingcastStaffItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AbstractSwingcastStaffItem animatable) {
        return animatable.getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractSwingcastStaffItem animatable) {
        return ANIMATION;
    }
}
