package jp.aquafactory.apprenticecodex.spell.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.MoonLightKatanaModel;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.renderer.SwordTrailLayer;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;

public class MoonLightKatanaRenderer extends GeoEntityRenderer<MoonLightKatanaEntity> {
    private static final String TRAIL_TIP_BONE = "trail_tip";
    private static final String TRAIL_ROOT_BONE = "trail_root";
    private static final String SCABBARD_TOP_BONE = "scabbard_top";
    private static final String SCABBARD_BOTTOM_BONE = "scabbard_bottom";
    private static final DustParticleOptions CHARGING_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.1f), 1.0f);
    private static final DustParticleOptions FULLY_CHARGED_PARTICLE =
            new DustParticleOptions(new Vector3f(0.35f, 0.9f, 1.0f), 1.0f);
    private static final int CHARGE_PARTICLE_SEGMENTS = 5;

    private Vec3 trailTipBonePosition;
    private Vec3 trailRootBonePosition;
    private Vec3 scabbardTopBonePosition;
    private Vec3 scabbardBottomBonePosition;
    private final Map<java.util.UUID, Long> lastChargeParticleTick = new HashMap<>();

    public MoonLightKatanaRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new MoonLightKatanaModel<>());
        addRenderLayer(new SwordTrailLayer<>(this));
    }

    @Override
    public void render(@NotNull MoonLightKatanaEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        trailTipBonePosition = null;
        trailRootBonePosition = null;
        scabbardTopBonePosition = null;
        scabbardBottomBonePosition = null;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
        spawnChargingBladeParticles(entity, partialTicks);

        if (trailTipBonePosition != null && trailRootBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    trailTipBonePosition,
                    trailRootBonePosition,
                    entity.level().getGameTime()
            );
        }

        if (scabbardTopBonePosition != null && scabbardBottomBonePosition != null) {
            GeoBonePoseCache.put(
                    entity.getUUID(),
                    MoonLightKatanaEntity.SCABBARD_CACHE_KEY,
                    scabbardTopBonePosition,
                    scabbardBottomBonePosition,
                    entity.level().getGameTime()
            );
        }
    }

    private void spawnChargingBladeParticles(MoonLightKatanaEntity entity, float partialTicks) {
        if (!entity.isChargingEffectActive()) {
            lastChargeParticleTick.remove(entity.getUUID());
            return;
        }

        var gameTime = entity.level().getGameTime();
        var lastGameTime = lastChargeParticleTick.get(entity.getUUID());
        if (lastGameTime != null && lastGameTime == gameTime) {
            return;
        }

        if (trailRootBonePosition == null || trailTipBonePosition == null) {
            return;
        }

        lastChargeParticleTick.put(entity.getUUID(), gameTime);

        var rootWorld = convertCachedBonePositionToWorld(entity, trailRootBonePosition, partialTicks);
        var tipWorld = convertCachedBonePositionToWorld(entity, trailTipBonePosition, partialTicks);
        var particle = entity.isFullyChargedEffect() ? FULLY_CHARGED_PARTICLE : CHARGING_PARTICLE;
        for (var i = 0; i < CHARGE_PARTICLE_SEGMENTS; i++) {
            var ratio = i / (double) (CHARGE_PARTICLE_SEGMENTS - 1);
            var point = rootWorld.lerp(tipWorld, ratio);
            entity.level().addParticle(particle, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static Vec3 convertCachedBonePositionToWorld(MoonLightKatanaEntity entity, Vec3 cachedPosition, float partialTicks) {
        var yawDeg = RotationTools.calculateYawPitchByEntity(entity, partialTicks).yaw();
        var yawRad = -yawDeg * Mth.DEG_TO_RAD;
        var local = cachedPosition.subtract(entity.position());
        return local.yRot(yawRad).add(entity.position());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, MoonLightKatanaEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        switch (bone.getName()) {
            case TRAIL_TIP_BONE -> trailTipBonePosition = boneWorldPosition(bone);
            case TRAIL_ROOT_BONE -> trailRootBonePosition = boneWorldPosition(bone);
            case SCABBARD_TOP_BONE -> scabbardTopBonePosition = boneWorldPosition(bone);
            case SCABBARD_BOTTOM_BONE -> scabbardBottomBonePosition = boneWorldPosition(bone);
            default -> {
            }
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    private static Vec3 boneWorldPosition(GeoBone bone) {
        var position = bone.getWorldPosition();
        return new Vec3(position.x(), position.y(), position.z());
    }
}
