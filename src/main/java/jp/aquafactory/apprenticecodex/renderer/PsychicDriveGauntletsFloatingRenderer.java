package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAnimation;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsMotion.SinglePose;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsMotion;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsPose;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsVisualState;
import jp.aquafactory.apprenticecodex.model.PsychicDriveGauntletsModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PsychicDriveGauntletsFloatingRenderer
        extends GeoObjectRenderer<PsychicDriveGauntletsFloatingRenderer.Visual> {
    // Blockbenchのbracer中心を基準に回転させ、片側のモデルだけ位置が偏るのを防ぐ。
    private static final float MODEL_CENTER_X = -0.25F / 16.0F;
    private static final float MODEL_CENTER_Y = 2.5F / 16.0F;

    public PsychicDriveGauntletsFloatingRenderer() {
        super(new PsychicDriveGauntletsModel<>());
        addRenderLayer(new PsychicDriveGauntletsGlowLayer<>(this));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Visual visual, GeoBone bone, RenderType type,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean glow = PsychicDriveGauntletsGlowLayer.glowing(bone);
        boolean dim = glow && !isReRender && (bone.getParent() == null || !PsychicDriveGauntletsGlowLayer.glowing(bone.getParent()));
        float brightness = dim ? 0.65F : 1;
        super.renderRecursively(poseStack, visual, bone, type, buffers, buffer, isReRender, partialTick,
                glow ? LightTexture.FULL_BRIGHT : packedLight, packedOverlay,
                red * brightness, green * brightness, blue * brightness, alpha);
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
        if (mirrored) {
            // 1.20.1のPoseStack.scaleは負の行列式で法線の近似補正が破綻するため、
            // 単位反転を位置・法線へ直接適用し、1.21.1と同じ法線の長さを保つ。
            poseStack.last().pose().scale(-1.0F, 1.0F, 1.0F);
            poseStack.last().normal().scale(-1.0F, 1.0F, 1.0F);
        }
        poseStack.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, 0.0F);
        var type = renderType(visual);
        this.animatable = visual;
        defaultRender(poseStack, visual, buffers, type, buffers.getBuffer(type), 0, partialTick, packedLight);
        poseStack.popPose();
    }

    @Override
    public long getInstanceId(Visual visual) {
        return visual.instanceId;
    }

    @Override
    public void preRender(PoseStack poseStack, Visual visual, BakedGeoModel model,
                          @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, visual, model, buffers, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
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
        private Snapshot arm;
        private double time;
        private float glowPulse;
        private final PsychicDriveGauntletsVisualState effects = new PsychicDriveGauntletsVisualState();
        private float auraStrength;

        public float auraStrength() { return auraStrength; }
        public void setGlowPulse(float value) { glowPulse = value; }

        public float glowStrength(String root) {
            if (arm == null) return 0.08F;
            float strength = switch (arm.phase()) {
                case CHARGING -> {
                    float threshold = switch (root) {
                        case "glow_core" -> 0;
                        case "glow_drive_ring", "glow_orbit_plates" -> 0.25F;
                        default -> 0.65F;
                    };
                    yield 0.08F + 0.72F * Mth.clamp(
                            (arm.charge() - threshold) / (1 - threshold), 0, 1);
                }
                case OUTBOUND -> arm.charge() >= 1 ? 0.9F : 0.65F;
                case HOLDING, MINING -> arm.charge() >= 1 ? 0.7F : 0.35F;
                case RETURNING -> 0.08F + 0.3F * (1 - PsychicDriveGauntletsMotion.returnProgress(
                        time - arm.motion().phaseStartedTick()));
                default -> 0.08F;
            };
            return Math.min(1, strength + glowPulse * 0.35F);
        }

        public Visual(long instanceId) {
            this.instanceId = instanceId;
        }

        public void setTick(double tick) {
            this.tick = tick;
        }

        public void updateAnimation(@Nullable Snapshot arm, double time) {
            this.arm = arm;
            this.time = time;
            auraStrength = effects.aura(arm, time);
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
