package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaff;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SoulstainedSteelSwingcastStaffModel extends GeoModel<SoulstainedSteelSwingcastStaff> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/swingcast_staff.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/swingcast_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(SoulstainedSteelSwingcastStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SoulstainedSteelSwingcastStaff animatable) {
        return animatable.getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(SoulstainedSteelSwingcastStaff animatable) {
        return ANIMATION;
    }
}
