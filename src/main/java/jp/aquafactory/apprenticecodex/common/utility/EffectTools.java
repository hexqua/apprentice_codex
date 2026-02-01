package jp.aquafactory.apprenticecodex.common.utility;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EffectTools {
    private static final RandomSource RNG = RandomSource.create();

    public static void createRingParticleClient(Vec3 position, Vec3 normal, double radius, int count,
                                                double randomOffsetRange, double randomSpeed,
                                                ParticleOptions particle, Level level){
        // 平面基底を作る.
        var norm = normal.normalize();
        var arbitrary = Math.abs(norm.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        var u = norm.cross(arbitrary).normalize();
        var w = norm.cross(u).normalize();
        var startAngle = RNG.nextDouble() * Math.PI * 2.0;

        for( var i = 0; i < count; i++){
            var r = radius + 0.1 * Math.sqrt(RNG.nextDouble());
            var angle = startAngle + (Math.PI * 2.0) * i / count + RNG.nextDouble() * 0.05;
            var a = Math.cos(angle) * r;
            var b = Math.sin(angle) * r;
            var offset = u.scale(a).add(w.scale(b));
            var randomPosition = new Vec3(
                    RNG.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                    RNG.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                    RNG.nextDouble() * randomOffsetRange - randomOffsetRange / 2
            );
            var pos = position.add(offset).add(randomPosition);
            level.addParticle(
                    particle,
                    pos.x,
                    pos.y,
                    pos.z,
                    RNG.nextDouble() * randomSpeed - randomSpeed / 2,
                    RNG.nextDouble() * randomSpeed - randomSpeed / 2,
                    RNG.nextDouble() * randomSpeed - randomSpeed / 2
            );
        }
    }

    public static void createRingParticleServer(Vec3 position, Vec3 normal, double radius, int count,
                                                double randomOffsetRange, double randomSpeed,
                                                ParticleOptions particle, Level level){

        // todo:いずれクライアント版に諸々移植し、サーバーパーティクル系は消す.
        // 平面基底を作る.
        var norm = normal.normalize();
        var arbitrary = Math.abs(norm.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        var u = norm.cross(arbitrary).normalize();
        var w = norm.cross(u).normalize();
        var startAngle = level.random.nextDouble() * Math.PI * 2.0;

        if (level instanceof ServerLevel server) {
            for( var i = 0; i < count; i++) {
                var angle = startAngle + (Math.PI * 2.0) * i / count;
                var a = Math.cos(angle) * radius;
                var b = Math.sin(angle) * radius;
                var offset = u.scale(a).add(w.scale(b));
                var pos = position.add(offset);

                server.sendParticles(
                        particle,
                        pos.x,
                        pos.y,
                        pos.z,
                        1,
                        server.random.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                        server.random.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                        server.random.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                        randomSpeed
                );
            }
        }
    }

    public static void createStickParticleClient(Vec3 position, Vec3 normal, double distance, int count,
                                                double randomOffsetRange, double randomSpeed,
                                                ParticleOptions particle, Level level){
        for( var i = 0; i < count; i++){
            var offset = normal.scale(RNG.nextDouble() * distance - distance / 2);
            var randomPosition = new Vec3(
                    RNG.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                    RNG.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                    RNG.nextDouble() * randomOffsetRange - randomOffsetRange / 2
            );
            var pos = position.add(offset).add(randomPosition);
            level.addParticle(
                    particle,
                    pos.x,
                    pos.y,
                    pos.z,
                    RNG.nextDouble() * randomSpeed - randomSpeed / 2,
                    RNG.nextDouble() * randomSpeed - randomSpeed / 2,
                    RNG.nextDouble() * randomSpeed - randomSpeed / 2
            );
        }
    }

    public static void createLineParticleServer(Vec3 position, Vec3 direction, double length, double step,
                                                ParticleOptions particle, Level level) {
        createLineParticleServer(position, direction, length, step, 0, 0, particle, level);
    }

    public static void createLineParticleServer(Vec3 position, Vec3 direction, double length, double step,
                                                double randomOffsetRange, double randomSpeed,
                                                ParticleOptions particle, Level level) {
        // todo:クライアント版も作る.
        var normalizedDirection = direction.normalize();
        if (level instanceof ServerLevel server) {
            for (var offset = 0.0; offset < length; offset += step) {
                var pos = position.add(normalizedDirection.scale(offset));
                server.sendParticles(
                        particle,
                        pos.x,
                        pos.y,
                        pos.z,
                        1,
                        server.random.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                        server.random.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                        server.random.nextDouble() * randomOffsetRange - randomOffsetRange / 2,
                        randomSpeed
                );
            }
        }
    }
}
