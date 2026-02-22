package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PastelStaffModel extends GeoModel<PastelStaff> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/pastel_staff.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/pastel_staff.png");
    // GeckoLibのGeoModel契約で必須。現段階はコントローラ未登録のため実際には参照されない。
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/pastel_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(PastelStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PastelStaff animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(PastelStaff animatable) {
        return ANIM;
    }
}
