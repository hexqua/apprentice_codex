package jp.aquafactory.apprenticecodex.spell.artisansmash;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ArtisanSmashTrajectoryPreviewRenderEvent {
    private static final ResourceLocation GUIDE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/artisan_smash_guide.png");
    private static final RenderType RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("artisan_smash_trajectory_preview", GUIDE_TEXTURE);
    private static final int MAX_PREDICTION_TICKS = 40;
    private static final double LAUNCHER_SEARCH_RADIUS = 6.0d;
    private static final double MAX_STANDBY_DISTANCE_SQR = 4.0d * 4.0d;
    private static final double DOT_SPACING = 0.95d;
    private static final double DOT_FLOW_SPEED = 0.18d;
    private static final float BASE_SCALE = 0.22f;
    private static final float HIT_SCALE = 0.34f;
    private static final float RED = 1.0f;
    private static final float GREEN = 0.55f;
    private static final float BLUE = 0.18f;

    private ArtisanSmashTrajectoryPreviewRenderEvent() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null || !isLocalArtisanSmashCasting(player)) {
            return;
        }

        var launcher = findLocalPreviewLauncher(level, player);
        if (launcher == null) {
            return;
        }

        var trajectory = buildTrajectory(level, player, launcher);
        if (trajectory.points().size() < 2) {
            return;
        }

        var poseStack = event.getPoseStack();
        var camera = event.getCamera();
        var cameraPosition = camera.getPosition();
        var cameraRotation = new Quaternionf(camera.rotation());
        var bufferSource = minecraft.renderBuffers().bufferSource();
        var buffer = bufferSource.getBuffer(RENDER_TYPE);
        var age = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true);

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        renderTrajectory(poseStack, buffer, trajectory, cameraPosition, cameraRotation, age);
        poseStack.popPose();

        bufferSource.endBatch(RENDER_TYPE);
    }

    private static boolean isLocalArtisanSmashCasting(Player player) {
        var spellData = ClientMagicData.getSyncedSpellData(player);
        return spellData.isCasting()
                && Objects.equals(spellData.getCastingSpellId(), SpellRegistry.ARTISAN_SMASH.get().getSpellId());
    }

    @Nullable
    private static ArtisanSmashLauncherEntity findLocalPreviewLauncher(ClientLevel level, Player player) {
        var expected = ArtisanSmashLauncherEntity.calculateStandbyPosition(player);
        var searchBox = player.getBoundingBox().inflate(LAUNCHER_SEARCH_RADIUS);
        ArtisanSmashLauncherEntity best = null;
        var bestDistanceSqr = Double.MAX_VALUE;

        for (var launcher : level.getEntitiesOfClass(ArtisanSmashLauncherEntity.class, searchBox,
                candidate -> !candidate.getIsReleased() && candidate.getProjectileSpeed() > 0.0f)) {
            var distanceSqr = launcher.distanceToSqr(expected);
            if (distanceSqr < bestDistanceSqr) {
                best = launcher;
                bestDistanceSqr = distanceSqr;
            }
        }

        return bestDistanceSqr <= MAX_STANDBY_DISTANCE_SQR ? best : null;
    }

    private static Trajectory buildTrajectory(ClientLevel level, Player player,
                                              ArtisanSmashLauncherEntity launcher) {
        var direction = launcher.getLookAngle();
        if (direction.lengthSqr() < 1.0e-6) {
            direction = player.getLookAngle();
        }
        if (direction.lengthSqr() < 1.0e-6) {
            return Trajectory.EMPTY;
        }

        direction = direction.normalize();
        var speed = launcher.getProjectileSpeed();
        if (speed <= 0.0f) {
            return Trajectory.EMPTY;
        }

        var points = new ArrayList<Vec3>();
        var position = launcher.position().add(direction.scale(ArtisanSmashLauncherEntity.FIRE_OFFSET));
        var velocity = direction.scale(speed);
        var gravityProbe = ArtisanSmashShellEntity.createPredictionProbe(level);
        var hitTerrain = false;
        points.add(position);

        for (int tick = 1; tick <= MAX_PREDICTION_TICKS; tick++) {
            var nextPosition = position.add(velocity);
            var hit = level.clip(new ClipContext(
                    position,
                    nextPosition,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    launcher
            ));
            if (hit.getType() == HitResult.Type.BLOCK) {
                points.add(hit.getLocation());
                hitTerrain = true;
                break;
            }

            position = nextPosition;
            points.add(position);

            var gravity = gravityProbe.getFlightGravityForPrediction(position);
            velocity = velocity.scale(ArtisanSmashShellEntity.FLIGHT_AIR_DRAG).add(0.0d, -gravity, 0.0d);
        }

        return new Trajectory(points, hitTerrain);
    }

    private static void renderTrajectory(PoseStack poseStack, VertexConsumer buffer, Trajectory trajectory,
                                         Vec3 cameraPosition, Quaternionf cameraRotation, float age) {
        var points = trajectory.points();
        var length = calculatePathLength(points);
        if (length <= 1.0e-4d) {
            return;
        }

        var offset = (age * DOT_FLOW_SPEED) % DOT_SPACING;
        var dotCount = Math.max(1, (int) Math.ceil(length / DOT_SPACING));
        for (int index = 0; index < dotCount; index++) {
            var distance = offset + index * DOT_SPACING;
            if (distance > length) {
                continue;
            }

            var progress = (float) (distance / length);
            var point = interpolatePathPoint(points, distance);
            var alpha = Mth.clamp(0.82f - progress * 0.46f, 0.18f, 0.82f);
            renderBillboard(poseStack, buffer, point, BASE_SCALE, alpha, cameraPosition, cameraRotation);
        }

        if (trajectory.hitTerrain()) {
            var endpoint = points.get(points.size() - 1);
            var pulse = 0.9f + 0.1f * (float) Math.sin(age * 0.45f);
            renderBillboard(poseStack, buffer, endpoint, HIT_SCALE * pulse, 0.72f, cameraPosition, cameraRotation);
        }
    }

    private static double calculatePathLength(List<Vec3> points) {
        var length = 0.0d;
        for (int index = 1; index < points.size(); index++) {
            length += points.get(index - 1).distanceTo(points.get(index));
        }
        return length;
    }

    private static Vec3 interpolatePathPoint(List<Vec3> points, double targetDistance) {
        var remaining = targetDistance;
        for (int index = 1; index < points.size(); index++) {
            var start = points.get(index - 1);
            var end = points.get(index);
            var segmentLength = start.distanceTo(end);
            if (segmentLength <= 1.0e-6d) {
                continue;
            }
            if (remaining <= segmentLength) {
                return start.lerp(end, remaining / segmentLength);
            }
            remaining -= segmentLength;
        }

        return points.get(points.size() - 1);
    }

    private static void renderBillboard(PoseStack poseStack, VertexConsumer buffer, Vec3 position,
                                        float scale, float alpha, Vec3 cameraPosition, Quaternionf cameraRotation) {
        alpha *= getCameraProximityAlpha(position, cameraPosition);
        if (alpha <= 0.001f) {
            return;
        }

        var half = scale * 0.5f;
        var right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(cameraRotation).mul(half);
        var up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(cameraRotation).mul(half);
        var normal = new Vector3f(0.0f, 0.0f, 1.0f).rotate(cameraRotation);
        var p0 = position.subtract(right.x + up.x, right.y + up.y, right.z + up.z);
        var p1 = position.add(-right.x + up.x, -right.y + up.y, -right.z + up.z);
        var p2 = position.add(right.x + up.x, right.y + up.y, right.z + up.z);
        var p3 = position.add(right.x - up.x, right.y - up.y, right.z - up.z);
        var red = RED * alpha;
        var green = GREEN * alpha;
        var blue = BLUE * alpha;
        var normalVec = new Vec3(normal.x(), normal.y(), normal.z());

        addQuad(poseStack, buffer, p0, p1, p2, p3, red, green, blue, alpha, normalVec);
        addQuad(poseStack, buffer, p3, p2, p1, p0, red, green, blue, alpha, normalVec.reverse());
    }

    private static float getCameraProximityAlpha(Vec3 position, Vec3 cameraPosition) {
        var distance = position.distanceTo(cameraPosition);
        if (distance <= 0.35d) {
            return 0.0f;
        }
        if (distance >= 1.1d) {
            return 1.0f;
        }

        var progress = (distance - 0.35d) / 0.75d;
        return (float) (progress * progress * (3.0d - 2.0d * progress));
    }

    private static void addQuad(PoseStack poseStack, VertexConsumer buffer,
                                Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                float red, float green, float blue, float alpha, Vec3 normal) {
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        vertex(buffer, poseMatrix, normalMatrix, p0, 0.0f, 1.0f, red, green, blue, alpha, normal);
        vertex(buffer, poseMatrix, normalMatrix, p1, 0.0f, 0.0f, red, green, blue, alpha, normal);
        vertex(buffer, poseMatrix, normalMatrix, p2, 1.0f, 0.0f, red, green, blue, alpha, normal);
        vertex(buffer, poseMatrix, normalMatrix, p3, 1.0f, 1.0f, red, green, blue, alpha, normal);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                               Vec3 position, float u, float v,
                               float red, float green, float blue, float alpha, Vec3 normal) {
        var transformedNormal = normalMatrix.transform(new Vector3f((float) normal.x, (float) normal.y, (float) normal.z));
        buffer.addVertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }

    private record Trajectory(List<Vec3> points, boolean hitTerrain) {
        private static final Trajectory EMPTY = new Trajectory(List.of(), false);
    }
}
