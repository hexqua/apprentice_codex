package jp.aquafactory.apprenticecodex.spell.deepsensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.packet.DeepSensorObservationsPacket;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.renderer.WallThroughHighlightRenderSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class DeepSensorClientDisplay {
    private static final ResourceLocation RHOMBUS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/particle/glow_rhombus.png");
    private static final RenderType CORE_RENDER_TYPE = ApprenticeRenderTypes.entityAdditiveGlowNoCullNoDepth(
            "deep_sensor_core_additive_no_depth", RHOMBUS_TEXTURE);
    private static final double DIRECTION_LENGTH = 4.0D;
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-6D;
    private static final int PULSE_STAGE_COUNT = 4;
    private static final float CORE_SIZE = 0.10F;
    private static final float CORE_ALPHA = 0.75F;

    private static final Map<BlockPos, Long> NEXT_PULSE_GAME_TIME = new HashMap<>();
    private static final List<ActivePulse> ACTIVE_PULSES = new ArrayList<>();
    private static List<DeepSensorObservationsPacket.Observation> observations = List.of();
    private static ClientLevel trackedLevel;
    private static UUID trackedPlayerUuid;

    private DeepSensorClientDisplay() {
    }

    public static void replace(ResourceKey<Level> dimension,
                               List<DeepSensorObservationsPacket.Observation> nextObservations) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !minecraft.level.dimension().equals(dimension)) {
            clear();
            return;
        }
        trackedLevel = minecraft.level;
        trackedPlayerUuid = minecraft.player.getUUID();
        observations = List.copyOf(nextObservations);
        NEXT_PULSE_GAME_TIME.keySet().retainAll(
                observations.stream().map(DeepSensorObservationsPacket.Observation::position).toList());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null || level != trackedLevel || !player.getUUID().equals(trackedPlayerUuid)) {
            clear();
            return;
        }

        var gameTime = level.getGameTime();
        observations = observations.stream()
                .filter(observation -> observation.expiresAtGameTime() > gameTime)
                .toList();
        NEXT_PULSE_GAME_TIME.keySet().retainAll(
                observations.stream().map(DeepSensorObservationsPacket.Observation::position).toList());

        var playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        for (var observation : observations) {
            var nextPulse = NEXT_PULSE_GAME_TIME.getOrDefault(observation.position(), gameTime);
            if (gameTime < nextPulse) {
                continue;
            }
            createPulse(observation, playerCenter, gameTime);
            NEXT_PULSE_GAME_TIME.put(observation.position(), gameTime + pulseInterval(observation.distance()));
        }

        var iterator = ACTIVE_PULSES.iterator();
        while (iterator.hasNext()) {
            var pulse = iterator.next();
            var stage = (int) (gameTime - pulse.startGameTime());
            if (stage < 0) {
                continue;
            }
            if (stage >= PULSE_STAGE_COUNT) {
                iterator.remove();
                continue;
            }
            spawnPulseParticle(level, pulse, stage);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!WallThroughHighlightRenderSupport.shouldRenderAt(event.getStage()) || observations.isEmpty()) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null || level != trackedLevel) {
            clear();
            return;
        }

        var partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        var playerCenter = new Vec3(
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()) + player.getBbHeight() * 0.5D,
                Mth.lerp(partialTick, player.zo, player.getZ())
        );
        var cameraPosition = event.getCamera().getPosition();
        var cameraRotation = new Quaternionf(event.getCamera().rotation());
        var poseStack = event.getPoseStack();
        var buffer = WallThroughHighlightRenderSupport.getBuffer(CORE_RENDER_TYPE);

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        for (var observation : observations) {
            var direction = observation.position().getCenter().subtract(playerCenter);
            if (direction.lengthSqr() <= MIN_DIRECTION_LENGTH_SQR) {
                continue;
            }
            var tip = playerCenter.add(direction.normalize().scale(DIRECTION_LENGTH));
            var color = colorForDistance(observation.distance());
            drawBillboard(poseStack, buffer, tip, CORE_SIZE,
                    color.red(), color.green(), color.blue(), CORE_ALPHA, cameraRotation);
        }
        poseStack.popPose();
        WallThroughHighlightRenderSupport.endBatch(CORE_RENDER_TYPE);
    }

    private static void createPulse(DeepSensorObservationsPacket.Observation observation,
                                    Vec3 playerCenter, long gameTime) {
        var direction = observation.position().getCenter().subtract(playerCenter);
        if (direction.lengthSqr() <= MIN_DIRECTION_LENGTH_SQR) {
            return;
        }
        var color = colorForDistance(observation.distance());
        ACTIVE_PULSES.add(new ActivePulse(
                playerCenter.add(direction.normalize().scale(DIRECTION_LENGTH)),
                playerCenter,
                color,
                gameTime
        ));
    }

    private static void spawnPulseParticle(ClientLevel level, ActivePulse pulse, int stage) {
        var progress = stage / (float) (PULSE_STAGE_COUNT - 1);
        var position = pulse.start().lerp(pulse.end(), progress);
        var rhombus = stage == 0 || stage == PULSE_STAGE_COUNT - 1;
        var type = rhombus ? ParticleRegistry.ADDITIVE_RHOMBUS.get() : ParticleRegistry.ADDITIVE_SPARK.get();
        var options = new AdditiveGlowParticleOptions(
                type,
                rhombus ? 0.10F : 0.07F,
                pulse.color().red(),
                pulse.color().green(),
                pulse.color().blue(),
                1,
                4,
                0,
                1.0F,
                1.0F,
                0.85F,
                1.0F,
                0.0F,
                0.75F,
                0.5F,
                false
        );
        level.addParticle(options, position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
    }

    public static int pulseInterval(float distance) {
        var progress = Mth.clamp((distance - 8.0F) / 16.0F, 0.0F, 1.0F);
        return Math.round(Mth.lerp(progress, 4.0F, 10.0F));
    }

    public static Color colorForDistance(float distance) {
        var progress = Mth.clamp((distance - 8.0F) / 16.0F, 0.0F, 1.0F);
        return hsvToRgb(Mth.lerp(progress, 0.0F, 0.75F));
    }

    private static Color hsvToRgb(float hue) {
        var scaledHue = hue * 6.0F;
        var sector = Mth.floor(scaledHue);
        var fraction = scaledHue - sector;
        var descending = 1.0F - fraction;
        return switch (sector % 6) {
            case 0 -> new Color(1.0F, fraction, 0.0F);
            case 1 -> new Color(descending, 1.0F, 0.0F);
            case 2 -> new Color(0.0F, 1.0F, fraction);
            case 3 -> new Color(0.0F, descending, 1.0F);
            case 4 -> new Color(fraction, 0.0F, 1.0F);
            default -> new Color(1.0F, 0.0F, descending);
        };
    }

    private static void clear() {
        observations = List.of();
        NEXT_PULSE_GAME_TIME.clear();
        ACTIVE_PULSES.clear();
        trackedLevel = null;
        trackedPlayerUuid = null;
    }

    private static void drawBillboard(PoseStack poseStack, VertexConsumer buffer, Vec3 center, float size,
                                      float red, float green, float blue, float alpha,
                                      Quaternionf cameraRotation) {
        var facing = new Vector3f(0.0F, 0.0F, 1.0F).rotate(cameraRotation);
        var right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(cameraRotation).mul(size);
        var up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(cameraRotation).mul(size);
        var p0 = center.subtract(right.x + up.x, right.y + up.y, right.z + up.z);
        var p1 = center.add(-right.x + up.x, -right.y + up.y, -right.z + up.z);
        var p2 = center.add(right.x + up.x, right.y + up.y, right.z + up.z);
        var p3 = center.add(right.x - up.x, right.y - up.y, right.z - up.z);
        var normal = new Vec3(facing.x(), facing.y(), facing.z());
        addQuad(poseStack, buffer, p0, p1, p2, p3, normal, red, green, blue, alpha);
        addQuad(poseStack, buffer, p3, p2, p1, p0, normal.reverse(), red, green, blue, alpha);
    }

    private static void addQuad(PoseStack poseStack, VertexConsumer buffer,
                                Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 normal,
                                float red, float green, float blue, float alpha) {
        var pose = poseStack.last();
        vertex(buffer, pose.pose(), pose.normal(), p0, 0.0F, 1.0F, normal, red, green, blue, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p1, 0.0F, 0.0F, normal, red, green, blue, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p2, 1.0F, 0.0F, normal, red, green, blue, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p3, 1.0F, 1.0F, normal, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                               Vec3 position, float u, float v, Vec3 normal,
                               float red, float green, float blue, float alpha) {
        var transformedNormal = normalMatrix.transform(
                new Vector3f((float) normal.x, (float) normal.y, (float) normal.z));
        buffer.addVertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red * alpha, green * alpha, blue * alpha, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }

    public record Color(float red, float green, float blue) {
    }

    private record ActivePulse(Vec3 start, Vec3 end, Color color, long startGameTime) {
    }
}
