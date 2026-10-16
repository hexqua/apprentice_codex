package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAnimation;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsMotion;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsMotion.SinglePose;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsPose;
import jp.aquafactory.apprenticecodex.model.PsychicDriveGauntletsModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
    // Blockbenchのbracer中心を基準に回転させ、片側のモデルだけ位置が偏るのを防ぐ。
    private static final float MODEL_CENTER_X = -0.25F / 16.0F;
    private static final float MODEL_CENTER_Y = 2.5F / 16.0F;

    public PsychicDriveGauntletsFloatingRenderer() {
        super(new PsychicDriveGauntletsModel<>());
    }

    public RenderType renderType(Visual visual) {
        // 負スケールで面の向きが反転するため、左右ともカリングしない。
        return RenderType.entityCutoutNoCull(getTextureLocation(visual));
    }

    public static Vec3 offset(float drawnAmount, boolean mirrored) {
        return PsychicDriveGauntletsPose.offset(drawnAmount, mirrored);
    }

    public void renderSide(PoseStack poseStack, MultiBufferSource buffers, Visual visual,
                           float drawnAmount, boolean mirrored, int packedLight, float partialTick) {
        var storedAmount = 1.0F - drawnAmount;
        poseStack.pushPose();
        // GeckoLibはBlockbenchのXを反転して読む。正スケール側の内側は描画座標の+Xになる。
        // 新モデルは親指がX側にあるため、横回転を加えず内側を身体へ向けたまま拳を下げる。
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F * storedAmount));
        poseStack.scale(mirrored ? -1.0F : 1.0F, 1.0F, 1.0F);
        poseStack.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, 0.0F);
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
        private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
        private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("attack_windup");
        private static final RawAnimation STRIKE = RawAnimation.begin().thenPlayAndHold("attack_strike");
        private static final RawAnimation RECOVER = RawAnimation.begin().thenPlay("attack_recover").thenLoop("idle");
        private final long instanceId;
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        private final PsychicDriveGauntletsAnimation animation = new PsychicDriveGauntletsAnimation();
        private long appliedRevision = -1;
        private double tick;
        private boolean singleMotion = true;
        private SinglePose singlePose = SinglePose.REST;
        private RawAnimation appliedAnimation;

        public Visual(long instanceId) {
            this.instanceId = instanceId;
        }

        public void setTick(double tick) {
            this.tick = tick;
        }

        public void updateAnimation(@Nullable Snapshot arm, double time) {
            if (arm != null && arm.phase() != Phase.IDLE) {
                singleMotion = arm.phase() == Phase.CHARGING || arm.charge() < 1;
            }
            if (singleMotion) {
                singlePose = arm == null ? SinglePose.REST : PsychicDriveGauntletsMotion.singlePose(arm, time);
            } else {
                animation.update(arm, time);
                singlePose = SinglePose.REST;
            }
        }

        public boolean singleMotion() { return singleMotion; }
        public SinglePose singlePose() { return singlePose; }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "pose", 0, state -> {
                // 単発はidleを止めず、server時刻から求めた追加姿勢だけをモデルへ重ねる。
                var next = singleMotion ? IDLE : switch (animation.stage()) {
                    case IDLE -> IDLE;
                    case WINDUP -> WINDUP;
                    case STRIKE -> STRIKE;
                    case RECOVER -> RECOVER;
                };
                if (appliedAnimation != next || (!singleMotion && appliedRevision != animation.revision())) {
                    state.getController().forceAnimationReset();
                }
                appliedAnimation = next;
                appliedRevision = animation.revision();
                return state.setAndContinue(next);
            }));
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
