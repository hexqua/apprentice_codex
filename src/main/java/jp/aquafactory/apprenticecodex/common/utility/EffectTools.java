package jp.aquafactory.apprenticecodex.common.utility;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EffectTools {
    private static final RandomSource RNG = RandomSource.create();

    public static void createRingParticleClient(Vec3 position, Vec3 normal, int count, Level level){
        // todo:パラメータを増やして汎用性を上げるか検討.
        // 平面基底を作る.
        var norm = normal.normalize();
        var arbitrary = Math.abs(norm.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        var u = norm.cross(arbitrary).normalize();
        var w = norm.cross(u).normalize();
        for( var i = 0; i < count; i++){
            var radius = 0.4 + 0.1 * Math.sqrt(level.random.nextDouble());
            var angle = (Math.PI * 2.0) * i / count + level.random.nextDouble() * 0.05;
            var a = Math.cos(angle) * radius;
            var b = Math.sin(angle) * radius;
            var offset = u.scale(a).add(w.scale(b));
            var pos = position.add(offset);
            level.addParticle(
                    ParticleTypes.END_ROD,
                    pos.x,
                    pos.y,
                    pos.z,
                    RNG.nextDouble() * 0.015,
                    RNG.nextDouble() * 0.015,
                    RNG.nextDouble() * 0.015
            );
        }
    }

    public static void createLineParticleServer(Vec3 position, Vec3 direction, double length, double step,
                                                ParticleOptions particle, Level level) {
        // todo:パラメータを増やして汎用性を上げるか検討.
        // todo:クライアント版も作る.
        var normalizedDirection = direction.normalize();
        if (level instanceof ServerLevel server) {
            for (var offset = 0.0; offset < length; offset += step) {
                var pos = position.add(normalizedDirection.scale(offset));
                server.sendParticles(
                        particle,
                        pos.x + server.random.nextDouble() * 0.01 - 0.005,
                        pos.y + server.random.nextDouble() * 0.01 - 0.005,
                        pos.z + server.random.nextDouble() * 0.01 - 0.005,
                        1,
                        server.random.nextDouble() * 0.1 - 0.05,
                        server.random.nextDouble() * 0.1 - 0.05,
                        server.random.nextDouble() * 0.1 - 0.05,
                        0.01
                );
            }
        }
    }
}
