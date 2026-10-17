package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsMotion.SinglePose;
import jp.aquafactory.apprenticecodex.renderer.PsychicDriveGauntletsFloatingRenderer.Visual;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class PsychicDriveGauntletsModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "geo/psychic_drive_gauntlets.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/geo/psychic_drive_gauntlets.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "animations/psychic_drive_gauntlets.animation.json");

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        var visual = animatable instanceof Visual floating ? floating : null;
        var pose = visual != null && visual.singleMotion() ? visual.singlePose() : SinglePose.REST;
        // rootは既存clipが触らないため、最大溜めへ切り替わった時も単発のひねりを必ず消す。
        getBone("root").ifPresent(root -> root.setRotZ(root.getInitialSnapshot().getRotZ()
                + pose.twist() * Mth.DEG_TO_RAD));
        if (visual != null && !visual.singleMotion()) return;
        // 同じgeoを使うitem描画へ、直前に描いた浮遊拳の追加変形を漏らさない。
        float power = pose.power();
        getBone("drive_ring").ifPresent(ring -> {
            var initial = ring.getInitialSnapshot();
            ring.setRotZ(initial.getRotZ() + 90 * Mth.DEG_TO_RAD * power);
            ring.setPosZ(initial.getOffsetZ() + 0.6F * power);
            float scale = 1 + 0.04F * power;
            ring.setScaleX(initial.getScaleX() * scale);
            ring.setScaleY(initial.getScaleY() * scale);
            ring.setScaleZ(initial.getScaleZ() * scale);
        });
        getBone("orbit_drive").ifPresent(orbit -> orbit.setRotZ(orbit.getInitialSnapshot().getRotZ()
                - 180 * Mth.DEG_TO_RAD * power));
        getBone("core_main").ifPresent(core -> {
            var initial = core.getInitialSnapshot();
            float scale = 1 + 0.14F * power;
            core.setScaleX(initial.getScaleX() * scale);
            core.setScaleY(initial.getScaleY() * scale);
            core.setScaleZ(initial.getScaleZ() * scale);
        });
    }
}
