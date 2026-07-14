package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FieldOverseerStaffModel extends GeoModel<FieldOverseerStaffEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "geo/field_overseer_staff.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/geo/field_overseer_staff.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "animations/field_overseer_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(FieldOverseerStaffEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FieldOverseerStaffEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FieldOverseerStaffEntity animatable) {
        return ANIMATION;
    }
}
