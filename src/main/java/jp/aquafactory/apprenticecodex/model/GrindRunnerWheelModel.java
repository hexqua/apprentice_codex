package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class GrindRunnerWheelModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/grind_runner_wheel.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/grind_runner_wheel.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/grind_runner_wheel.animation.json");

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIM;
    }
}
