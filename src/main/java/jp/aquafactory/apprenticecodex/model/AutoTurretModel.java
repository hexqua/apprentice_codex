package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class AutoTurretModel extends GeoModel<AutoTurretEntity> {
    private static final String CROSSBOW_BONE = "corssbow";
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/auto_turret_crossbow.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/auto_turret_crossbow_base.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/auto_turret_crossbow.animation.json");

    @Override
    public ResourceLocation getModelResource(AutoTurretEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AutoTurretEntity animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(AutoTurretEntity animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(AutoTurretEntity animatable, long instanceId, AnimationState<AutoTurretEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        var crossbow = getBone(CROSSBOW_BONE).orElse(null);
        if (crossbow == null) {
            return;
        }

        var initial = crossbow.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0f : initial.getRotX();
        crossbow.setRotX(baseRotX + animatable.getAimPitch() * Mth.DEG_TO_RAD);
    }
}
