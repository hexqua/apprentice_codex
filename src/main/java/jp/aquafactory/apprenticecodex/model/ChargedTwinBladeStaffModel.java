package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ChargedTwinBladeStaffModel extends GeoModel<ChargedTwinBladeStaff> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/charged_twin_blade_staff.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/charged_twin_blade_staff.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/charged_twin_blade_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(ChargedTwinBladeStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ChargedTwinBladeStaff animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ChargedTwinBladeStaff animatable) {
        return ANIMATION;
    }
}
