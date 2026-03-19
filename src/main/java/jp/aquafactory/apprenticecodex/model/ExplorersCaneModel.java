package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
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
        if (level == null || cameraEntity == null) {
            resetToInitialTransform(compassInner);
            return;
        }

        var initial = compassInner.getInitialSnapshot();
        float baseRotY = initial == null ? 0.0f : initial.getRotY();
        float cameraYaw = cameraEntity.getViewYRot(animationState.getPartialTick());
        compassInner.setRotY(
                baseRotY + resolveCompassAngle(level, cameraEntity.getX(), cameraEntity.getZ(), cameraYaw)
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

    private static float resolveCompassAngle(Level level, double sourceX, double sourceZ, float sourceYaw) {
        var sharedSpawn = level.getSharedSpawnPos();
        double dx = sharedSpawn.getX() + 0.5D - sourceX;
        double dz = sharedSpawn.getZ() + 0.5D - sourceZ;
        double distanceSq = dx * dx + dz * dz;

        if (distanceSq < 1.0e-6D) {
            return 0.0f;
        }

        double yawRad = sourceYaw * Mth.DEG_TO_RAD;
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);

        // 目標ベクトルをプレイヤー基準へ回し、そのローカル平面上で針角へ変換する.
        double localX = dx * cosYaw + dz * sinYaw;
        double localZ = -dx * sinYaw + dz * cosYaw;
        return (float) Math.atan2(-localZ, localX);
    }
}
