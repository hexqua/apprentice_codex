package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ExplorersCaneModel extends GeoModel<ExplorersCane> {
    private static final String COMPASS_INNER_BONE = "compass_inner";
    private static final String COMPASS_CUBE_BONE = "compass_cube";
    private static final float COMPASS_CUBE_ROT_X_SPEED = 0.55f * Mth.DEG_TO_RAD;
    private static final float COMPASS_CUBE_ROT_Y_SPEED = 0.85f * Mth.DEG_TO_RAD;
    private static final float COMPASS_CUBE_ROT_Z_SPEED = 0.70f * Mth.DEG_TO_RAD;
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/explorers_cane.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/explorers_cane.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/explorers_cane.animation.json");

    @Override
    public ResourceLocation getModelResource(ExplorersCane animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ExplorersCane animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(ExplorersCane animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(ExplorersCane animatable, long instanceId, AnimationState<ExplorersCane> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var compassInner = getBone(COMPASS_INNER_BONE).orElse(null);
        var compassCube = getBone(COMPASS_CUBE_BONE).orElse(null);
        if (compassInner == null || compassCube == null) {
            return;
        }

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (isStaticPerspective(perspective)) {
            hideCompass(compassInner, compassCube);
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var cameraEntity = minecraft.getCameraEntity();
        var player = cameraEntity instanceof Player viewingPlayer ? viewingPlayer : minecraft.player;
        var itemStack = animationState.getData(DataTickets.ITEMSTACK);
        if (level == null || player == null || itemStack == null || itemStack.isEmpty()) {
            hideCompass(compassInner, compassCube);
            return;
        }

        compassInner.setHidden(false);
        compassCube.setHidden(false);

        var initial = compassInner.getInitialSnapshot();
        float baseRotY = initial == null ? 0.0f : initial.getRotY();
        float cameraYaw = player.getViewYRot(animationState.getPartialTick());
        var tickData = animationState.getData(DataTickets.TICK);
        float tick = tickData == null ? 0.0f : tickData.floatValue() + animationState.getPartialTick();
        compassInner.setRotY(
                baseRotY + ExplorersCane.resolveCompassAngle(itemStack, level, player.getX(), player.getZ(), cameraYaw, tick)
        );
        applyCubeRotation(compassCube, level.getGameTime() + tick);
    }

    private static boolean isStaticPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.GUI
                || perspective == ItemDisplayContext.GROUND
                || perspective == ItemDisplayContext.FIXED;
    }

    private static void hideCompass(GeoBone compassInner, GeoBone compassCube) {
        compassInner.setHidden(true);
        compassCube.setHidden(true);
        resetToInitialTransform(compassInner);
        resetToInitialTransform(compassCube);
    }

    private static void applyCubeRotation(GeoBone bone, float time) {
        var initial = bone.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0f : initial.getRotX();
        float baseRotY = initial == null ? 0.0f : initial.getRotY();
        float baseRotZ = initial == null ? 0.0f : initial.getRotZ();

        bone.setRotX(baseRotX + (time * COMPASS_CUBE_ROT_X_SPEED) % Mth.TWO_PI);
        bone.setRotY(baseRotY + (time * COMPASS_CUBE_ROT_Y_SPEED) % Mth.TWO_PI);
        bone.setRotZ(baseRotZ + (time * COMPASS_CUBE_ROT_Z_SPEED) % Mth.TWO_PI);
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
