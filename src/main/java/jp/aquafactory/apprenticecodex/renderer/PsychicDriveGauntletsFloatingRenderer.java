package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.PsychicDriveGauntletsModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PsychicDriveGauntletsFloatingRenderer
        extends GeoObjectRenderer<PsychicDriveGauntletsFloatingRenderer.Visual> {
    private static final float DRAWN_SIDE = 0.8F;
    private static final float DRAWN_FORWARD = -0.1F;
    private static final float DRAWN_HEIGHT = -0.5F;
    private static final float SHEATHED_SIDE = 0.35F;
    private static final float SHEATHED_BACK = 0.65F;
    private static final float SHEATHED_HEIGHT = -0.65F;
    // Blockbenchのbracer中心を基準に回転させ、片側のモデルだけ位置が偏るのを防ぐ。
    private static final float MODEL_CENTER_Y = 4.25F / 16.0F;
    private static final float MODEL_CENTER_Z = 0.25F / 16.0F;

    public PsychicDriveGauntletsFloatingRenderer() {
        super(new PsychicDriveGauntletsModel<>());
    }

    public RenderType renderType(Visual visual) {
        // 負スケールで面の向きが反転するため、左右ともカリングしない。
        return RenderType.entityCutoutNoCull(getTextureLocation(visual));
    }

    public static Vec3 offset(float drawnAmount, boolean mirrored) {
        var side = mirrored ? 1.0F : -1.0F;
        return new Vec3(side * Mth.lerp(drawnAmount, SHEATHED_SIDE, DRAWN_SIDE),
                Mth.lerp(drawnAmount, SHEATHED_HEIGHT, DRAWN_HEIGHT),
                Mth.lerp(drawnAmount, SHEATHED_BACK, DRAWN_FORWARD));
    }

    public void renderSide(PoseStack poseStack, MultiBufferSource buffers, Visual visual,
                           float drawnAmount, boolean mirrored, int packedLight, float partialTick) {
        var side = mirrored ? 1.0F : -1.0F;
        var storedAmount = 1.0F - drawnAmount;
        poseStack.pushPose();
        // GeckoLibはBlockbenchのXを反転して読む。正スケール側の内側は描画座標の+Xになる。
        // 納刀では内側を身体へ向け、-Zの拳側を下へ向ける。
        poseStack.mulPose(Axis.YP.rotationDegrees(-side * 90.0F * storedAmount));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F * storedAmount));
        poseStack.scale(mirrored ? -1.0F : 1.0F, 1.0F, 1.0F);
        poseStack.translate(0.0F, -MODEL_CENTER_Y, -MODEL_CENTER_Z);
        var type = renderType(visual);
        render(poseStack, visual, buffers, type, buffers.getBuffer(type), packedLight, partialTick);
        poseStack.popPose();
    }

    @Override
    public long getInstanceId(Visual visual) {
        return visual.instanceId;
    }

    @Override
    public void preRender(PoseStack poseStack, Visual visual, BakedGeoModel model,
                          @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, visual, model, buffers, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
        // GeoObjectRendererのブロック中央への既定移動を相殺し、指定した浮遊位置を原点にする。
        poseStack.translate(-0.5F, -0.51F, -0.5F);
    }

    public static final class Visual implements GeoAnimatable {
        private final long instanceId;
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        private double tick;

        public Visual(long instanceId) {
            this.instanceId = instanceId;
        }

        public void setTick(double tick) {
            this.tick = tick;
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "idle", 0,
                    state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object relatedObject) {
            return tick;
        }
    }
}
