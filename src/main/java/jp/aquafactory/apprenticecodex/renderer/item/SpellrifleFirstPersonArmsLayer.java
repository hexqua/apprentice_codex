package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.compat.emf.EmfCompat;
import jp.aquafactory.apprenticecodex.item.ClientItemRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.function.Supplier;
import java.util.stream.Stream;

final class SpellrifleFirstPersonArmsLayer<T extends Item & GeoItem> extends GeoRenderLayer<T> {
    private static final String GRIP_ANCHOR = "grip_hand_anchor";
    private static final String SUPPORT_ANCHOR = "support_hand_anchor";
    private static final float ARM_SCALE = 0.9F;
    private static final float GRIP_PITCH = -110.0F;
    private static final float SUPPORT_PITCH = -105.0F;
    private static final float GRIP_YAW = 40.0F;
    private static final float SUPPORT_YAW = -45.0F;
    private final Supplier<ItemDisplayContext> perspective;
    @Nullable private ArmTransform gripTransform;
    @Nullable private ArmTransform supportTransform;

    SpellrifleFirstPersonArmsLayer(GeoItemRenderer<T> renderer, Supplier<ItemDisplayContext> perspective) {
        super(renderer);
        this.perspective = perspective;
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable RenderType renderType,
                          MultiBufferSource buffers, @Nullable VertexConsumer buffer, float partialTick,
                          int packedLight, int packedOverlay) {
        gripTransform = null;
        supportTransform = null;
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource buffers, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!perspective.get().firstPerson()
                || bone.isHidden() || (!GRIP_ANCHOR.equals(bone.getName()) && !SUPPORT_ANCHOR.equals(bone.getName()))) {
            return;
        }
        // このフックには親と自身の変換が適用済み。空ボーンのpivotへは別途移動する。
        poseStack.pushPose();
        try {
            RenderUtils.translateToPivotPoint(poseStack, bone);
            var transform = new ArmTransform(new Matrix4f(poseStack.last().pose()), new Matrix3f(poseStack.last().normal()));
            if (GRIP_ANCHOR.equals(bone.getName())) {
                gripTransform = transform;
            } else {
                supportTransform = transform;
            }
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable RenderType renderType,
                       MultiBufferSource buffers, @Nullable VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (buffer == null || player == null || player != minecraft.getCameraEntity()
                || ClientItemRenderContext.getRenderingEntity() != player
                || !minecraft.options.getCameraType().isFirstPerson()
                || player.isInvisible() || player.isSpectator()
                || perspective.get() != (player.getMainArm() == HumanoidArm.RIGHT
                    ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                || SpellrifleCastAnimationVisibility.ownsArms(player, partialTick)) {
            return;
        }
        if (!(minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        // 通常パスの終了後に描くことで、発光再描画と銃の頂点バッファから腕を分離する。
        var mainArm = player.getMainArm();
        int direction = mainArm == HumanoidArm.RIGHT ? 1 : -1;
        // 両銃のアンカーは銃の中央にあり、左手用の表示変換も適用済み。腕と開き角度だけを左右交換する。
        renderArm(gripTransform, player, playerRenderer, mainArm, GRIP_PITCH, direction * GRIP_YAW, buffers, packedLight);
        // オフハンドのアイテム描画と支持手が重なり、腕が増えて見えるのを防ぐ。
        if (player.getOffhandItem().isEmpty()) {
            renderArm(supportTransform, player, playerRenderer, mainArm.getOpposite(), SUPPORT_PITCH,
                    direction * SUPPORT_YAW, buffers, packedLight);
        }
    }

    private static void renderArm(@Nullable ArmTransform transform, LocalPlayer player, PlayerRenderer renderer,
                                  HumanoidArm arm, float pitch, float yaw, MultiBufferSource buffers, int light) {
        if (transform == null) {
            return;
        }
        var model = renderer.getModel();
        var parts = Stream.of(model.head, model.hat, model.body, model.rightArm, model.leftArm,
                        model.rightLeg, model.leftLeg, model.rightSleeve, model.leftSleeve,
                        model.rightPants, model.leftPants, model.jacket)
                .flatMap(ModelPart::getAllParts).distinct().map(PartState::capture).toList();
        float attackTime = model.attackTime;
        float swimAmount = model.swimAmount;
        boolean crouching = model.crouching;
        boolean riding = model.riding;
        boolean young = model.young;
        var rightPose = model.rightArmPose;
        var leftPose = model.leftArmPose;
        try {
            var poseStack = new PoseStack();
            poseStack.last().pose().set(transform.pose());
            poseStack.last().normal().set(transform.normal());
            // 手中心はアンカーに残し、肩側を銃の左右下方へ開いてADSでも輪郭を見せる。
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
            boolean slim = "slim".equals(player.getModelName());
            // バニラの肩pivotと腕末端の手中心との差。左右で別のスキン領域を使う。
            float handX = (arm == HumanoidArm.RIGHT ? -1 : 1) * (slim ? 5.5F : 6.0F);
            float handY = slim ? 10.5F : 10.0F;
            poseStack.translate(-handX / 16.0F, -handY / 16.0F, 0);
            EmfCompat.renderSpellrifleHand(player, () -> {
                if (arm == HumanoidArm.RIGHT) {
                    renderer.renderRightHand(poseStack, buffers, light, player);
                } else {
                    renderer.renderLeftHand(poseStack, buffers, light, player);
                }
            });
        } finally {
            // PlayerRendererはsetupAnimで共有モデル全体を書き換えるため、後続の描画へ残さない。
            parts.forEach(PartState::restore);
            model.attackTime = attackTime;
            model.swimAmount = swimAmount;
            model.crouching = crouching;
            model.riding = riding;
            model.young = young;
            model.rightArmPose = rightPose;
            model.leftArmPose = leftPose;
        }
    }

    private record ArmTransform(Matrix4f pose, Matrix3f normal) {
    }

    private record PartState(ModelPart part, PartPose pose, float xScale, float yScale, float zScale,
                             boolean visible, boolean skipDraw) {
        static PartState capture(ModelPart part) {
            return new PartState(part, part.storePose(), part.xScale, part.yScale, part.zScale, part.visible, part.skipDraw);
        }

        void restore() {
            part.loadPose(pose);
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
            part.visible = visible;
            part.skipDraw = skipDraw;
        }
    }
}
