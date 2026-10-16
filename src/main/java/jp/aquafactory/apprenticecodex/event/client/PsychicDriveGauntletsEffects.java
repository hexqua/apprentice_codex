package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsEffect;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsEffect.Kind;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsVisualState;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class PsychicDriveGauntletsEffects {
    private static final float RED = 0.72F;
    private static final float GREEN = 0.26F;
    private static final float BLUE = 1;
    private static final int TRAIL_TICKS = 3;
    private static final int MAX_PULSES = 128;
    private static final int MAX_PENDING = 128;
    private static final RenderType WAVE = ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly(
            "psychic_drive_gauntlets_wave", ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID, "textures/spell/force_field_wave.png"));
    private static final RenderType TRAIL = ApprenticeRenderTypes.additiveColorNoCull("psychic_drive_gauntlets_trail");
    private record Side(UUID player, boolean right) {}
    private record Pending(UUID player, PsychicDriveGauntletsEffect effect, long due) {}
    private record Pulse(UUID player, Vec3 position, Vec3 direction, long start, int lifetime,
                         float minRadius, float maxRadius, float strength, boolean arc) {}
    private record Sample(Vec3 position, long tick) {}
    private static final class Trail {
        private final Deque<Sample> samples = new ArrayDeque<>();
        private final long launch;
        private final float width;
        private Vec3 ownerPosition;

        private Trail(long launch, float charge, Vec3 ownerPosition) {
            this.launch = launch;
            width = charge >= 1 ? 0.065F : 0.025F;
            this.ownerPosition = ownerPosition;
        }
    }
    private static final Deque<Pending> PENDING = new ArrayDeque<>();
    private static final Deque<Pulse> PULSES = new ArrayDeque<>();
    private static final Map<Side, Trail> TRAILS = new HashMap<>();
    private static final Map<Side, Long> FLASHES = new HashMap<>();
    private static final Map<Side, PsychicDriveGauntletsVisualState> DRILL_EMITTERS = new HashMap<>();
    private static ClientLevel level;
    private static long lastTick = Long.MIN_VALUE;

    private PsychicDriveGauntletsEffects() {}

    private static void resetLevel() {
        var current = Minecraft.getInstance().level;
        if (level != current) {
            PENDING.clear();
            PULSES.clear();
            TRAILS.clear();
            FLASHES.clear();
            DRILL_EMITTERS.clear();
            level = current;
            lastTick = Long.MIN_VALUE;
        }
    }

    public static void clear(UUID player) {
        PENDING.removeIf(value -> value.player.equals(player));
        PULSES.removeIf(value -> value.player.equals(player));
        TRAILS.keySet().removeIf(side -> side.player.equals(player));
        FLASHES.keySet().removeIf(side -> side.player.equals(player));
        DRILL_EMITTERS.keySet().removeIf(side -> side.player.equals(player));
    }

    public static void accept(UUID player, PsychicDriveGauntletsEffect effect) {
        resetLevel();
        if (level == null) return;
        // 命中位置は既存の1tick補間が終わってから提示する。過去のsnapshotからは再生しない。
        PENDING.addLast(new Pending(player, effect, level.getGameTime() + (effect.kind().delayed() ? 1 : 0)));
        while (PENDING.size() > MAX_PENDING) PENDING.removeFirst();
    }

    public static float flash(UUID player, boolean right, double time) {
        var start = FLASHES.get(new Side(player, right));
        return start == null ? 0 : (float) Mth.clamp(1 - (time - start) / 3, 0, 1);
    }

    private static boolean eligible(Player player) {
        return player != null && player.isAlive() && !player.isSpectator()
                && PsychicDriveGauntletsClientState.mode(player) != Mode.DISABLED;
    }

    private static boolean hidden(Player player) {
        return !eligible(player) || player.isInvisible();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        resetLevel();
        if (level == null) return;
        long now = level.getGameTime();
        if (lastTick == now) return;
        lastTick = now;
        PULSES.removeIf(value -> now - value.start >= value.lifetime
                || hidden(level.getPlayerByUUID(value.player)));
        FLASHES.entrySet().removeIf(entry -> now - entry.getValue() > 3
                || hidden(level.getPlayerByUUID(entry.getKey().player)));
        TRAILS.keySet().removeIf(side -> hidden(level.getPlayerByUUID(side.player)));
        DRILL_EMITTERS.keySet().removeIf(side -> hidden(level.getPlayerByUUID(side.player)));
        var iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            var pending = iterator.next();
            if (pending.due > now) continue;
            iterator.remove();
            if (eligible(level.getPlayerByUUID(pending.player))) play(pending.player, pending.effect, now);
        }
        for (var player : level.players()) {
            if (hidden(player)) continue;
            for (boolean right : new boolean[] {false, true}) {
                var side = new Side(player.getUUID(), right);
                var arm = PsychicDriveGauntletsClientState.get(player.getUUID(), right, 0);
                if (PsychicDriveGauntletsVisualState.drilling(arm)) {
                    var emitter = DRILL_EMITTERS.computeIfAbsent(side, ignored -> new PsychicDriveGauntletsVisualState());
                    if (emitter.emitContinuousSpark(arm, now)) {
                        sparks(arm.position(), direction(arm.destination().subtract(arm.flightStart())), 1);
                    }
                } else {
                    DRILL_EMITTERS.remove(side);
                }
                if (arm == null || arm.phase() != Phase.OUTBOUND) {
                    TRAILS.remove(side);
                } else {
                    var trail = TRAILS.get(side);
                    if (trail == null || trail.launch != arm.launchTick()
                            || trail.ownerPosition.distanceToSqr(player.position()) > 16) {
                        trail = new Trail(arm.launchTick(), arm.charge(), player.position());
                        TRAILS.put(side, trail);
                    }
                    trail.ownerPosition = player.position();
                    trail.samples.addLast(new Sample(arm.position(), now));
                    while (!trail.samples.isEmpty() && now - trail.samples.getFirst().tick >= TRAIL_TICKS) {
                        trail.samples.removeFirst();
                    }
                }
                if (arm != null && arm.phase() == Phase.CHARGING && now % 2 == 0) {
                    var center = arm.position();
                    var offset = randomOffset().normalize().scale(0.22 + arm.charge() * 0.12);
                    particle(center.add(offset), offset.scale(-0.15), 0.035F, 5, false);
                }
            }
        }
    }

    private static void play(UUID player, PsychicDriveGauntletsEffect effect, long now) {
        var sound = effect.sound();
        if (sound != null && effect.kind() != Kind.CHARGE_READY) {
            var pos = effect.position();
            level.playLocalSound(pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, 1, 1, false);
        }
        // 透明化で拳の表示を隠していても、周囲へ届く攻撃音は消さない。
        if (hidden(level.getPlayerByUUID(player))) return;
        if (effect.kind() != Kind.CHARGE_START && effect.kind() != Kind.REJECTED) {
            FLASHES.put(new Side(player, effect.right()), now);
        }
        var center = effect.position();
        var direction = direction(effect.direction());
        switch (effect.kind()) {
            case LAUNCH -> {
                boolean full = effect.charge() >= 1;
                burst(center, direction.scale(-1), full ? 6 : 3, full ? 0.13 : 0.06);
                if (full) pulse(player, center.add(direction.scale(-0.15)), direction, now, 5, 0.12F, 0.5F, 0.65F, false);
            }
            case IMPACT -> impact(player, center, direction, now, false);
            case BLOCK_IMPACT -> {
                impact(player, center, direction, now, false);
                if (effect.charge() >= 1) burst(center, Vec3.ZERO, 2, 0.08);
            }
            case DRILL_START -> {
                particle(center, Vec3.ZERO, 0.13F, 3, true);
                sparks(center, direction, PsychicDriveGauntletsVisualState.INITIAL_SPARKS);
                pulse(player, center.subtract(direction.scale(0.025)), direction, now,
                        PsychicDriveGauntletsVisualState.CONTACT_RING_TICKS,
                        PsychicDriveGauntletsVisualState.CONTACT_RING_START,
                        PsychicDriveGauntletsVisualState.CONTACT_RING_END,
                        PsychicDriveGauntletsVisualState.CONTACT_RING_STRENGTH, false);
                pulse(player, center, direction, now, 4, 0.12F, 0.25F, 0.5F, true);
            }
            case DRILL_TICK -> {
                sparks(center, direction, PsychicDriveGauntletsVisualState.INTERMEDIATE_SPARKS);
                pulse(player, center, direction, now, 4, 0.14F, 0.26F, 0.45F, true);
            }
            case FINISH -> {
                impact(player, center, direction, now, true);
                burst(center, direction, 10, 0.15);
            }
            case REJECTED -> particle(center, Vec3.ZERO, 0.045F, 2, false);
            case CHARGE_START, CHARGE_READY -> { }
        }
    }

    private static void impact(UUID player, Vec3 center, Vec3 direction, long now, boolean finish) {
        pulse(player, center.subtract(direction.scale(0.025)), direction, now,
                finish ? 8 : PsychicDriveGauntletsVisualState.NORMAL_RING_TICKS,
                finish ? 0.2F : PsychicDriveGauntletsVisualState.NORMAL_RING_START,
                finish ? 0.9F : PsychicDriveGauntletsVisualState.NORMAL_RING_END,
                finish ? 0.85F : PsychicDriveGauntletsVisualState.NORMAL_RING_STRENGTH, false);
        particle(center, Vec3.ZERO, finish ? 0.18F : 0.09F, 3, true);
        if (!finish) burst(center, Vec3.ZERO, 4, 0.06);
    }

    private static void pulse(UUID player, Vec3 center, Vec3 direction, long now, int lifetime,
                              float min, float max, float strength, boolean arc) {
        PULSES.addLast(new Pulse(player, center, direction, now, lifetime, min, max, strength, arc));
        while (PULSES.size() > MAX_PULSES) PULSES.removeFirst();
    }

    private static Vec3 randomOffset() {
        return new Vec3(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5,
                level.random.nextDouble() - 0.5);
    }

    private static void sparks(Vec3 center, Vec3 direction, int count) {
        var lateral = direction.cross(new Vec3(0, 1, 0));
        lateral = lateral.lengthSqr() < 1.0e-8 ? new Vec3(1, 0, 0) : lateral.normalize();
        for (int i = 0; i < count; i++) {
            double sideSpeed = Mth.lerp(level.random.nextDouble(),
                    PsychicDriveGauntletsVisualState.SPARK_MIN_SIDE_SPEED,
                    PsychicDriveGauntletsVisualState.SPARK_MAX_SIDE_SPEED);
            double upSpeed = Mth.lerp(level.random.nextDouble(),
                    PsychicDriveGauntletsVisualState.SPARK_MIN_UP_SPEED,
                    PsychicDriveGauntletsVisualState.SPARK_MAX_UP_SPEED);
            var velocity = lateral.scale(level.random.nextBoolean() ? sideSpeed : -sideSpeed).add(0, upSpeed, 0);
            var position = center.add(randomOffset().scale(0.12));
            level.addParticle(ParticleRegistry.PSYCHIC_DRIVE_GAUNTLETS_SPARK.get(),
                    position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static Vec3 direction(Vec3 direction) {
        return direction.lengthSqr() < 1.0e-8 ? new Vec3(0, 0, 1) : direction.normalize();
    }

    private static void burst(Vec3 center, Vec3 bias, int count, double speed) {
        for (int i = 0; i < count; i++) {
            var offset = randomOffset();
            particle(center.add(offset.scale(0.12)), offset.add(bias).scale(speed), 0.04F, 4, false);
        }
    }

    private static void particle(Vec3 position, Vec3 velocity, float size, int lifetime, boolean circle) {
        var options = new AdditiveGlowParticleOptions(
                circle ? ParticleRegistry.ADDITIVE_CIRCLE.get() : ParticleRegistry.ADDITIVE_SPARK.get(),
                size, RED, GREEN, BLUE, circle ? 1 : 0, lifetime, 0,
                1, 1, 0.45F, 0.65F, 0, 0.15F, 0.15F, true);
        level.addParticle(options, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        resetLevel();
        if (level == null) return;
        var minecraft = Minecraft.getInstance();
        var camera = event.getCamera().getPosition();
        double time = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true);
        var stack = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        stack.pushPose();
        stack.translate(-camera.x, -camera.y, -camera.z);
        if (!PULSES.isEmpty()) {
            var buffer = buffers.getBuffer(WAVE);
            for (var pulse : PULSES) if (!pulse.arc) renderPulse(stack, buffer, pulse, time);
            buffers.endBatch(WAVE);
        }
        if (!TRAILS.isEmpty() || !PULSES.isEmpty()) {
            var buffer = buffers.getBuffer(TRAIL);
            for (var pulse : PULSES) if (pulse.arc) renderPulse(stack, buffer, pulse, time);
            for (var trail : TRAILS.values()) renderTrail(stack, buffer, trail, time, camera);
            buffers.endBatch(TRAIL);
        }
        stack.popPose();
    }

    private static void renderPulse(PoseStack stack, VertexConsumer buffer, Pulse pulse, double time) {
        float progress = Mth.clamp((float) (time - pulse.start) / pulse.lifetime, 0, 1);
        float radius = Mth.lerp(1 - (1 - progress) * (1 - progress), pulse.minRadius, pulse.maxRadius);
        float alpha = pulse.strength * (1 - progress);
        var normal = pulse.direction;
        var u = normal.cross(Math.abs(normal.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        var v = normal.cross(u).normalize();
        if (pulse.arc) {
            // 途中段は外へ広がる衝撃波にせず、接触面内を回る短い弧にする。
            for (int i = 0; i < 8; i++) {
                double angle = progress * Math.PI * 2 + i * Math.PI / 12;
                var radialA = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
                var radialB = u.scale(Math.cos(angle + 0.18)).add(v.scale(Math.sin(angle + 0.18)));
                trailVertex(stack, buffer, pulse.position.add(radialA.scale(radius)), alpha);
                trailVertex(stack, buffer, pulse.position.add(radialB.scale(radius)), alpha);
                trailVertex(stack, buffer, pulse.position.add(radialB.scale(radius + 0.025)), alpha);
                trailVertex(stack, buffer, pulse.position.add(radialA.scale(radius + 0.025)), alpha);
            }
        } else {
            u = u.scale(radius);
            v = v.scale(radius);
            var center = pulse.position;
            waveQuad(stack, buffer, center.subtract(u).subtract(v), center.add(u).subtract(v),
                    center.add(u).add(v), center.subtract(u).add(v), normal, alpha);
        }
    }

    private static void waveQuad(PoseStack stack, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                 Vec3 normal, float alpha) {
        waveVertex(stack, buffer, a, 0, 1, normal, alpha);
        waveVertex(stack, buffer, b, 1, 1, normal, alpha);
        waveVertex(stack, buffer, c, 1, 0, normal, alpha);
        waveVertex(stack, buffer, d, 0, 0, normal, alpha);
    }

    private static void waveVertex(PoseStack stack, VertexConsumer buffer, Vec3 point, float u, float v,
                                   Vec3 normal, float alpha) {
        var n = stack.last().normal().transform(new Vector3f((float) normal.x, (float) normal.y, (float) normal.z));
        buffer.addVertex(stack.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(RED * alpha, GREEN * alpha, BLUE * alpha, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(n.x, n.y, n.z);
    }

    private static void renderTrail(PoseStack stack, VertexConsumer buffer, Trail trail, double time, Vec3 camera) {
        Sample previous = null;
        for (var sample : trail.samples) {
            if (previous != null) {
                var tangent = sample.position.subtract(previous.position);
                var width = tangent.cross(camera.subtract(sample.position)).normalize().scale(trail.width);
                float alpha = (float) Mth.clamp(1 - (time - previous.tick) / TRAIL_TICKS, 0, 1) * 0.4F;
                trailVertex(stack, buffer, previous.position.subtract(width), alpha);
                trailVertex(stack, buffer, previous.position.add(width), alpha);
                trailVertex(stack, buffer, sample.position.add(width), alpha);
                trailVertex(stack, buffer, sample.position.subtract(width), alpha);
            }
            previous = sample;
        }
    }

    private static void trailVertex(PoseStack stack, VertexConsumer buffer, Vec3 point, float alpha) {
        buffer.addVertex(stack.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(RED * alpha, GREEN * alpha, BLUE * alpha, alpha);
    }
}
