package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ExplorersCaneModel extends GeoModel<ExplorersCane> {
    private static final String COMPASS_INNER_BONE = "compass_inner";
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
        if (compassInner == null) {
            return;
        }

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (isStaticPerspective(perspective)) {
            resetToInitialTransform(compassInner);
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var cameraEntity = minecraft.getCameraEntity();
        var player = cameraEntity instanceof Player viewingPlayer ? viewingPlayer : minecraft.player;
        var itemStack = animationState.getData(DataTickets.ITEMSTACK);
        if (level == null || player == null || itemStack == null || itemStack.isEmpty()) {
            resetToInitialTransform(compassInner);
            return;
        }

        var initial = compassInner.getInitialSnapshot();
        float baseRotY = initial == null ? 0.0f : initial.getRotY();
        float cameraYaw = player.getViewYRot(animationState.getPartialTick());
        var tickData = animationState.getData(DataTickets.TICK);
        float tick = tickData == null ? 0.0f : tickData.floatValue() + animationState.getPartialTick();
        compassInner.setRotY(
                baseRotY + ExplorersCane.resolveCompassAngle(itemStack, level, player.getX(), player.getZ(), cameraYaw, tick)
        );
    }

    private static boolean isStaticPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.GUI
                || perspective == ItemDisplayContext.GROUND
                || perspective == ItemDisplayContext.FIXED;
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
