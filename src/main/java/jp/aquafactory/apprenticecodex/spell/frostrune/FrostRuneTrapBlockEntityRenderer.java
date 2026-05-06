package jp.aquafactory.apprenticecodex.spell.frostrune;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class FrostRuneTrapBlockEntityRenderer extends GeoBlockRenderer<FrostRuneTrapBlockEntity> {
    private static final String RUNE_1_BONE = "rune_1";
    private static final String RUNE_2_BONE = "rune_2";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/frost_rune_trap.png");
    private static final RenderType ADDITIVE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("frost_rune_trap_additive", TEXTURE);
    private static final float ICE_RED = 0.58F;
    private static final float ICE_GREEN = 0.86F;
    private static final float ICE_BLUE = 1.0F;
    private static final float DISTANCE_FADE_START = 2.0F;
    private static final float DISTANCE_FADE_PER_BLOCK = 0.3F;

    public FrostRuneTrapBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new FrostRuneTrapModel());
    }

    @Override
    public void actuallyRender(PoseStack poseStack, FrostRuneTrapBlockEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        if (!isReRender) {
            var animationState = new AnimationState<>(animatable, 0, 0, partialTick, false);
            var instanceId = getInstanceId(animatable);
            GeoModel<FrostRuneTrapBlockEntity> currentModel = getGeoModel();

            animationState.setData(DataTickets.TICK, animatable.getTick(animatable));
            animationState.setData(DataTickets.BLOCK_ENTITY, animatable);
            currentModel.addAdditionalStateData(animatable, instanceId, animationState::setData);
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(createOrientation(animatable));
            poseStack.translate(0.0D, -0.5D, 0.0D);
            currentModel.setCustomAnimations(animatable, instanceId, animationState);
        }

        this.modelRenderTranslations = new org.joml.Matrix4f(poseStack.last().pose());
        updateAnimatedTextureFrame(animatable);
        for (var group : model.topLevelBones()) {
            renderRecursively(
                    poseStack,
                    animatable,
                    group,
                    renderType,
                    bufferSource,
                    buffer,
                    isReRender,
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    red,
                    green,
                    blue,
                    alpha
            );
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FrostRuneTrapBlockEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        var boneName = bone.getName();
        if (RUNE_1_BONE.equals(boneName)) {
            var opacity = rune1Opacity(animatable, partialTick) * distanceOpacity(animatable, partialTick);
            if (opacity <= 0.0F) {
                return;
            }
            // 加算合成では頂点alphaが効きづらいため、透明度相当は色へ乗算する。
            var colorBlend = 0.5F + 0.5F * Mth.sin(animatable.getRenderAge(partialTick) * Mth.TWO_PI / 200.0F);
            super.renderRecursively(
                    poseStack,
                    animatable,
                    bone,
                    ADDITIVE_RENDER_TYPE,
                    bufferSource,
                    bufferSource.getBuffer(ADDITIVE_RENDER_TYPE),
                    isReRender,
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    Mth.lerp(colorBlend, ICE_RED, 1.0F) * opacity,
                    Mth.lerp(colorBlend, ICE_GREEN, 1.0F) * opacity,
                    Mth.lerp(colorBlend, ICE_BLUE, 1.0F) * opacity,
                    1.0F
            );
            return;
        }

        if (RUNE_2_BONE.equals(boneName)) {
            var opacity = rune2Opacity(animatable, partialTick) * distanceOpacity(animatable, partialTick);
            if (opacity <= 0.0F) {
                return;
            }
            super.renderRecursively(
                    poseStack,
                    animatable,
                    bone,
                    ADDITIVE_RENDER_TYPE,
                    bufferSource,
                    bufferSource.getBuffer(ADDITIVE_RENDER_TYPE),
                    isReRender,
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    opacity,
                    opacity,
                    opacity,
                    1.0F
            );
            return;
        }

        super.renderRecursively(
                poseStack,
                animatable,
                bone,
                ADDITIVE_RENDER_TYPE,
                bufferSource,
                bufferSource.getBuffer(ADDITIVE_RENDER_TYPE),
                isReRender,
                partialTick,
                LightTexture.FULL_BRIGHT,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );
    }

    @Override
    public boolean shouldRender(@NotNull FrostRuneTrapBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return cameraPos.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0D * 64.0D;
    }

    private static float rune1Opacity(FrostRuneTrapBlockEntity blockEntity, float partialTick) {
        if (blockEntity.isDetonating()) {
            return 1.0F - sineEaseOut(Mth.clamp(blockEntity.getRenderDetonateAge(partialTick) / 5.0F, 0.0F, 1.0F));
        }
        return sineEaseOut(Mth.clamp(blockEntity.getRenderAge(partialTick) / 10.0F, 0.0F, 1.0F));
    }

    private static float rune2Opacity(FrostRuneTrapBlockEntity blockEntity, float partialTick) {
        if (blockEntity.isDetonating()) {
            return 1.0F - sineEaseOut(Mth.clamp(blockEntity.getRenderDetonateAge(partialTick) / 10.0F, 0.0F, 1.0F));
        }
        return blockEntity.getRenderAge(partialTick) < FrostRuneTrapBlockEntity.ARM_DELAY_TICKS ? 0.5F : 1.0F;
    }

    private static float distanceOpacity(FrostRuneTrapBlockEntity blockEntity, float partialTick) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return 1.0F;
        }
        var ownerUuid = blockEntity.getOwnerUuid();
        if (ownerUuid != null && ownerUuid.equals(player.getUUID())) {
            return 1.0F;
        }

        var playerX = Mth.lerp(partialTick, player.xOld, player.getX());
        var playerY = Mth.lerp(partialTick, player.yOld, player.getY());
        var playerZ = Mth.lerp(partialTick, player.zOld, player.getZ());
        var distance = Math.sqrt(blockEntity.getBlockPos().getCenter().distanceToSqr(playerX, playerY, playerZ));
        if (distance <= DISTANCE_FADE_START) {
            return 1.0F;
        }
        return Mth.clamp(1.0F - (float) (distance - DISTANCE_FADE_START) * DISTANCE_FADE_PER_BLOCK, 0.0F, 1.0F);
    }

    private static float sineEaseOut(float progress) {
        return Mth.sin(progress * Mth.HALF_PI);
    }

    private static Quaternionf createOrientation(FrostRuneTrapBlockEntity blockEntity) {
        var supportFacing = blockEntity.getBlockState().getValue(FrostRuneTrapBlock.FACING);
        var normal = Vec3.atLowerCornerOf(supportFacing.getOpposite().getNormal());
        var visualNorth = Vec3.atLowerCornerOf(blockEntity.getVisualNorth().getNormal());
        if (Math.abs(normal.dot(visualNorth)) > 0.99D) {
            visualNorth = fallbackVisualNorth(normal);
        }

        var normalVector = new Vector3f((float) normal.x, (float) normal.y, (float) normal.z);
        var targetNorth = new Vector3f((float) visualNorth.x, (float) visualNorth.y, (float) visualNorth.z);
        var rotation = new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, normalVector.x(), normalVector.y(), normalVector.z());
        var currentNorth = rotation.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        var twistAngle = currentNorth.angleSigned(targetNorth, normalVector);
        var twist = new Quaternionf().fromAxisAngleRad(normalVector, twistAngle);
        return twist.mul(rotation);
    }

    private static Vec3 fallbackVisualNorth(Vec3 normal) {
        return Math.abs(normal.y) > 0.5D ? new Vec3(0.0D, 0.0D, -1.0D) : new Vec3(0.0D, -1.0D, 0.0D);
    }
}
