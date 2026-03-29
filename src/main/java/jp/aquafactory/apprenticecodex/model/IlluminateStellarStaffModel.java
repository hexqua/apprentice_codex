package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.IlluminateStellarStaff;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class IlluminateStellarStaffModel extends GeoModel<IlluminateStellarStaff> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/illuminate_stellar_staff.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/illuminate_stellar_staff.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/illuminate_stellar_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(IlluminateStellarStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(IlluminateStellarStaff animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(IlluminateStellarStaff animatable) {
        return ANIM;
    }
}
