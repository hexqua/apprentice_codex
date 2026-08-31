package jp.aquafactory.apprenticecodex.spell.thermalslice;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import jp.aquafactory.apprenticecodex.model.ThermalSliceKatanaModel;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.renderer.SwordTrailLayer;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Map;
import java.util.WeakHashMap;

public class ThermalSliceKatanaRenderer extends GeoEntityRenderer<ThermalSliceKatanaEntity> {
    private static final String TRAIL_TIP_BONE = "trail_tip";
    private static final String TRAIL_ROOT_BONE = "trail_root";
    private static final String THERMAL_BONE = "thermal";
    private static final double FLAME_BLADE_MIN_RATIO = 0.55D;
    private static final double FLAME_SAMPLE_SPACING = 0.45D;
    private static final double FLAME_RANDOM_SPEED = 0.04D;
    private static final int MAX_FLAMES_PER_TICK = 6;

    private Vec3 trailTipBonePosition;
    private Vec3 trailRootBonePosition;
    private final Map<ThermalSliceKatanaEntity, BladeParticlePose> previousBladeParticlePose = new WeakHashMap<>();

    public ThermalSliceKatanaRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new ThermalSliceKatanaModel<>());
        addRenderLayer(new SwordTrailLayer<>(this));
    }

    @Override
    public void render(@NotNull ThermalSliceKatanaEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        trailTipBonePosition = null;
        trailRootBonePosition = null;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
        spawnSlashFlames(entity, partialTicks);

        if (trailTipBonePosition != null && trailRootBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    trailTipBonePosition,
                    trailRootBonePosition,
                    entity.level().getGameTime()
            );
        }
    }

    private void spawnSlashFlames(ThermalSliceKatanaEntity entity, float partialTick) {
        if (!entity.isTrailActive()) {
            previousBladeParticlePose.remove(entity);
            return;
        }

        var gameTime = entity.level().getGameTime();
        var previousPose = previousBladeParticlePose.get(entity);
        if (previousPose != null && previousPose.gameTime() == gameTime) {
            return;
        }

        if (trailRootBonePosition == null || trailTipBonePosition == null) {
            return;
        }

        var rootWorld = convertBonePositionToWorld(entity, trailRootBonePosition, partialTick);
        var tipWorld = convertBonePositionToWorld(entity, trailTipBonePosition, partialTick);
        previousBladeParticlePose.put(entity, new BladeParticlePose(gameTime, rootWorld, tipWorld));

        var flameCount = 1;
        if (previousPose != null) {
            var rootTravel = previousPose.root().distanceTo(rootWorld);
            var tipTravel = previousPose.tip().distanceTo(tipWorld);
            var maxTravel = Math.max(rootTravel, tipTravel);
            flameCount = Mth.clamp(Mth.ceil(maxTravel / FLAME_SAMPLE_SPACING), 1, MAX_FLAMES_PER_TICK);
        }

        var random = entity.getRandom();
        for (var i = 0; i < flameCount; ++i) {
            var sweepRatio = (i + random.nextDouble()) / flameCount;
            var sweptRoot = previousPose == null ? rootWorld : previousPose.root().lerp(rootWorld, sweepRatio);
            var sweptTip = previousPose == null ? tipWorld : previousPose.tip().lerp(tipWorld, sweepRatio);
            var bladeRatio = Mth.lerp(random.nextDouble(), FLAME_BLADE_MIN_RATIO, 1.0D);
            spawnFlameParticle(entity, sweptRoot.lerp(sweptTip, bladeRatio));
        }
    }

    private static void spawnFlameParticle(ThermalSliceKatanaEntity entity, Vec3 position) {
        var random = entity.getRandom();
        entity.level().addParticle(
                ParticleHelper.FIRE,
                true,
                position.x,
                position.y,
                position.z,
                (random.nextDouble() - 0.5D) * FLAME_RANDOM_SPEED,
                (random.nextDouble() - 0.5D) * FLAME_RANDOM_SPEED,
                (random.nextDouble() - 0.5D) * FLAME_RANDOM_SPEED
        );
    }

    private static Vec3 convertBonePositionToWorld(
            ThermalSliceKatanaEntity entity,
            Vec3 bonePosition,
            float partialTick
    ) {
        var yawDegrees = RotationTools.calculateYawPitchByEntity(entity, partialTick).yaw();
        var yawRadians = -yawDegrees * Mth.DEG_TO_RAD;
        return bonePosition.subtract(entity.position()).yRot(yawRadians).add(entity.position());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ThermalSliceKatanaEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        switch (bone.getName()) {
            case TRAIL_TIP_BONE -> trailTipBonePosition = boneWorldPosition(bone);
            case TRAIL_ROOT_BONE -> trailRootBonePosition = boneWorldPosition(bone);
            default -> {
            }
        }

        if (isBoneOrChildOf(bone, THERMAL_BONE)) {
            var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            super.renderRecursively(
                    poseStack, animatable, bone, emissiveRenderType, bufferSource,
                    bufferSource.getBuffer(emissiveRenderType), isReRender, partialTick,
                    LightTexture.FULL_BRIGHT, packedOverlay, colour
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static Vec3 boneWorldPosition(GeoBone bone) {
        var position = bone.getWorldPosition();
        return new Vec3(position.x(), position.y(), position.z());
    }

    private record BladeParticlePose(long gameTime, Vec3 root, Vec3 tip) {
    }
}
