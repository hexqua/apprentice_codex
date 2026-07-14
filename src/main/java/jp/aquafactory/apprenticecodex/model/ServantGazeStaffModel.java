package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeStaffEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ServantGazeStaffModel extends GeoModel<ServantGazeStaffEntity> {
    private static final ResourceLocation MODEL = id("geo/servant_gaze_staff.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/geo/servant_gaze_staff.png");
    private static final ResourceLocation ANIMATION = id("animations/servant_gaze_staff.animation.json");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path);
    }

    @Override public ResourceLocation getModelResource(ServantGazeStaffEntity animatable) { return MODEL; }
    @Override public ResourceLocation getTextureResource(ServantGazeStaffEntity animatable) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(ServantGazeStaffEntity animatable) { return ANIMATION; }
}
