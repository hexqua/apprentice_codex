package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MagiEngineersStaffModel extends GeoModel<MagiEngineersStaff> {
    @Override
    public ResourceLocation getModelResource(MagiEngineersStaff item) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/magi_engineers_staff.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MagiEngineersStaff item) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/magi_engineers_staff.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MagiEngineersStaff item) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/magi_engineers_staff.animation.json");
    }
}
