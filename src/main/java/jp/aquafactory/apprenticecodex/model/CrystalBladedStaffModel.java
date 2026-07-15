package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrystalBladedStaffModel extends GeoModel<CrystalBladedStaff> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/crystal_bladed_staff.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/crystal_bladed_staff.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/crystal_bladed_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(CrystalBladedStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CrystalBladedStaff animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(CrystalBladedStaff animatable) {
        return ANIM;
    }
}
