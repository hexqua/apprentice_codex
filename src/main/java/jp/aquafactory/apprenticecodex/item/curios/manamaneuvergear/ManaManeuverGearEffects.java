package jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear;

import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ManaManeuverGearFallEffectPacket;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.joml.Vector3f;

public final class ManaManeuverGearEffects {
    private static final float EFFECT_EPSILON = 1.0e-4F;
    private static final int WALL_JUMP_SPARK_COUNT = 8;
    private static final int WALL_JUMP_RHOMBUS_COUNT = 3;
    // 壁滑りは常時発動するため、継続演出は間引いて視界と通信量への負担を抑える。
    private static final int WALL_SLIDE_PARTICLE_INTERVAL_TICKS = 4;
    private static final int WALL_SLIDE_MANA_PARTICLE_INTERVAL_TICKS = 8;
    private static final double FOOT_Y_OFFSET = 0.12D;
    private static final DustParticleOptions WALL_SLIDE_MANA_DUST =
            new DustParticleOptions(new Vector3f(0.16F, 0.34F, 0.46F), 0.65F);

    private ManaManeuverGearEffects() {
    }

    public static void playWallJump(ServerPlayer player) {
        if (player instanceof FakePlayer) {
            return;
        }

        var level = player.serverLevel();
        var random = level.random;
        var origin = footPosition(player);
        for (var i = 0; i < WALL_JUMP_SPARK_COUNT; ++i) {
            sendGlowParticle(level, origin, createWallJumpParticle(ParticleRegistry.ADDITIVE_SPARK.get(), random, false));
        }
        for (var i = 0; i < WALL_JUMP_RHOMBUS_COUNT; ++i) {
            sendGlowParticle(level, origin, createWallJumpParticle(ParticleRegistry.ADDITIVE_RHOMBUS.get(), random, true));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.VANILLA_HIGH_JUMP.get(), SoundSource.PLAYERS, 0.75F, 1.15F);
    }

    public static void playWallSlide(ServerPlayer player, boolean started) {
        if (player instanceof FakePlayer) {
            return;
        }

        var gameTime = player.level().getGameTime();
        if (!started && gameTime % WALL_SLIDE_PARTICLE_INTERVAL_TICKS != 0L) {
            return;
        }

        //noinspection resource
        var level = player.serverLevel();
        var origin = footPosition(player);
        var smokeCount = started ? 4 : 1;
        level.sendParticles(
                ParticleTypes.SMOKE,
                origin.x,
                origin.y,
                origin.z,
                smokeCount,
                0.22D,
                0.12D,
                0.22D,
                0.015D
        );

        var manaParticleCount = started ? 2
                : gameTime % WALL_SLIDE_MANA_PARTICLE_INTERVAL_TICKS == 0L ? 1 : 0;
        if (manaParticleCount > 0) {
            level.sendParticles(
                    WALL_SLIDE_MANA_DUST,
                    origin.x,
                    origin.y,
                    origin.z,
                    manaParticleCount,
                    0.18D,
                    0.08D,
                    0.18D,
                    0.008D
            );
        }
    }

    public static void playFallDamageReduction(ServerPlayer player, float reducedDamage) {
        if (player instanceof FakePlayer || reducedDamage <= EFFECT_EPSILON) {
            return;
        }

        // 小さな落下は控えめにしつつ、8ダメージ以上では演出規模を頭打ちにする。
        var intensity = Mth.clamp(reducedDamage / 8.0F, 0.0F, 1.0F);
        var radius = Mth.lerp(intensity, 0.6F, 1.6F);
        var volume = Mth.lerp(intensity, 0.45F, 0.65F);
        var pitch = Mth.lerp(intensity, 1.12F, 0.96F);
        var origin = new Vec3(player.getX(), player.getY(), player.getZ());
        Networks.sendToTrackingEntityAndSelf(player, new ManaManeuverGearFallEffectPacket(origin, radius));
        //noinspection resource
        player.serverLevel().playSound(null, origin.x, origin.y, origin.z,
                SoundRegistry.FORCE_FIELD_DEFLECT.get(), SoundSource.PLAYERS, volume, pitch);
    }

    private static Vec3 footPosition(ServerPlayer player) {
        return new Vec3(player.getX(), player.getY() + FOOT_Y_OFFSET, player.getZ());
    }

    private static void sendGlowParticle(ServerLevel level, Vec3 origin, AdditiveGlowParticleOptions particle) {
        level.sendParticles(
                particle,
                origin.x,
                origin.y,
                origin.z,
                1,
                0.25D,
                0.16D,
                0.25D,
                0.035D
        );
    }

    private static AdditiveGlowParticleOptions createWallJumpParticle(
            ParticleType<AdditiveGlowParticleOptions> type,
            RandomSource random,
            boolean rhombus
    ) {
        var color = randomWallJumpColor(random);
        return new AdditiveGlowParticleOptions(
                type,
                (rhombus ? 0.19F : 0.14F) + random.nextFloat() * 0.07F,
                color.x(),
                color.y(),
                color.z(),
                rhombus ? 3 : 2,
                rhombus ? 13 : 9,
                3,
                0.65F,
                1.3F,
                0.55F,
                1.0F,
                0.04F,
                0.62F,
                0.4F,
                true
        );
    }

    private static Vector3f randomWallJumpColor(RandomSource random) {
        var t = random.nextFloat();
        return new Vector3f(
                Mth.lerp(t, 0.24F, 0.72F),
                Mth.lerp(t, 0.82F, 0.38F),
                1.0F
        );
    }
}
