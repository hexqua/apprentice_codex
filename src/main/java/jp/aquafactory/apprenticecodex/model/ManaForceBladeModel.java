package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ManaForceBladeModel extends GeoModel<ManaForceBlade> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/mana_force_blade.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/mana_force_blade.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/mana_force_blade.animation.json");

    @Override
    public ResourceLocation getModelResource(ManaForceBlade animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ManaForceBlade animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(ManaForceBlade animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(ManaForceBlade animatable, long instanceId, AnimationState<ManaForceBlade> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        var orb = getBone("orb");
        if (orb.isEmpty()) {
            return;
        }
        if (isStaticPerspective(perspective)) {
            resetToInitialTransform(orb.get());
            return;
        }

        var tickData = animationState.getData(DataTickets.TICK);
        float tick = tickData == null ? animationState.getPartialTick() : tickData.floatValue() + animationState.getPartialTick();
        applyOrbRotation(orb.get(), tick);
    }

    private static boolean isStaticPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.GUI
                || perspective == ItemDisplayContext.GROUND
                || perspective == ItemDisplayContext.FIXED;
    }

    private static void applyOrbRotation(GeoBone bone, float tick) {
        var initial = bone.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0F : initial.getRotX();
        float baseRotY = initial == null ? 0.0F : initial.getRotY();
        float baseRotZ = initial == null ? 0.0F : initial.getRotZ();

        // Arcanum in a Jar と同じく時間だけで回し、アイテム状態に依存しない常時発光コアにする。
        bone.setRotX(baseRotX + tick * 4.0F * Mth.DEG_TO_RAD);
        bone.setRotY(baseRotY + tick * 7.0F * Mth.DEG_TO_RAD);
        bone.setRotZ(baseRotZ + tick * 5.0F * Mth.DEG_TO_RAD);
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
}
