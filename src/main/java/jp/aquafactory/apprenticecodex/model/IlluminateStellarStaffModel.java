package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.IlluminateStellarStaff;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class IlluminateStellarStaffModel extends GeoModel<IlluminateStellarStaff> {
    private static final OrbRotation[] ORB_ROTATIONS = {
            new OrbRotation("orb1", 4.8f, 6.9f, 3.4f, 0.15f),
            new OrbRotation("orb2", 6.2f, 4.4f, 5.7f, 1.05f),
            new OrbRotation("orb3", 3.9f, 7.8f, 4.8f, 2.10f),
            new OrbRotation("orb4", 5.6f, 5.1f, 6.4f, 2.95f),
            new OrbRotation("orb5", 4.2f, 6.3f, 5.0f, 3.70f),
            new OrbRotation("orb6", 7.1f, 4.9f, 3.8f, 4.45f),
            new OrbRotation("orb7", 5.3f, 7.4f, 4.5f, 5.10f),
            new OrbRotation("orb8", 4.7f, 5.8f, 6.8f, 5.85f)
    };
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

    @Override
    public void setCustomAnimations(IlluminateStellarStaff animatable, long instanceId,
                                    AnimationState<IlluminateStellarStaff> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (isStaticPerspective(perspective)) {
            for (var orbRotation : ORB_ROTATIONS) {
                getBone(orbRotation.boneName()).ifPresent(IlluminateStellarStaffModel::resetToInitialTransform);
            }
            return;
        }

        var tickData = animationState.getData(DataTickets.TICK);
        float tick = tickData == null ? animationState.getPartialTick() : tickData.floatValue() + animationState.getPartialTick();
        for (var orbRotation : ORB_ROTATIONS) {
            getBone(orbRotation.boneName()).ifPresent(bone -> applyOrbRotation(bone, tick, orbRotation));
        }
    }

    private static boolean isStaticPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.GUI
                || perspective == ItemDisplayContext.GROUND
                || perspective == ItemDisplayContext.FIXED;
    }

    private static void applyOrbRotation(GeoBone bone, float tick, OrbRotation rotation) {
        var initial = bone.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0f : initial.getRotX();
        float baseRotY = initial == null ? 0.0f : initial.getRotY();
        float baseRotZ = initial == null ? 0.0f : initial.getRotZ();
        float phase = tick + rotation.phaseOffset();

        // 親 orbit の公転は animation 側に任せ、各 orb のローカル自転だけを上乗せする。
        bone.setRotX(baseRotX + phase * rotation.rotXSpeedRad());
        bone.setRotY(baseRotY + phase * rotation.rotYSpeedRad());
        bone.setRotZ(baseRotZ + phase * rotation.rotZSpeedRad());
    }

    private static void resetToInitialTransform(GeoBone bone) {
        var initial = bone.getInitialSnapshot();
        if (initial == null) {
            return;
        }

        bone.setRotX(initial.getRotX());
        bone.setRotY(initial.getRotY());
        bone.setRotZ(initial.getRotZ());
        bone.setPosX(initial.getOffsetX());
        bone.setPosY(initial.getOffsetY());
        bone.setPosZ(initial.getOffsetZ());
    }

    private record OrbRotation(String boneName, float rotXSpeedDeg, float rotYSpeedDeg, float rotZSpeedDeg, float phaseOffset) {
        private float rotXSpeedRad() {
            return rotXSpeedDeg * Mth.DEG_TO_RAD;
        }

        private float rotYSpeedRad() {
            return rotYSpeedDeg * Mth.DEG_TO_RAD;
        }

        private float rotZSpeedRad() {
            return rotZSpeedDeg * Mth.DEG_TO_RAD;
        }
    }
}
