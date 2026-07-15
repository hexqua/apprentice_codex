package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MithrilFreecastStaffModel extends GeoModel<MithrilFreecastStaff> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/mithril_freecast_staff.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/mithril_freecast_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(MithrilFreecastStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MithrilFreecastStaff animatable) {
        return animatable.getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(MithrilFreecastStaff animatable) {
        return ANIMATION;
    }
}
