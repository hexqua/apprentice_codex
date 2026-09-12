package jp.aquafactory.apprenticecodex.spell.lightningarrow;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/** 標準の速度パケットは各軸3.9で切り詰められるため、軌道を独立して同期する。 */
public record LightningArrowFlight(Vec3 origin, Vec3 direction, double speed, double range, int seed) {
    public static final double SPEED = 5.0;

    public LightningArrowFlight {
        if (!finite(origin) || !finite(direction) || direction.lengthSqr() < 1.0e-8
                || !Double.isFinite(speed) || speed <= 0 || !Double.isFinite(range) || range <= 0) {
            throw new IllegalArgumentException("Invalid lightning arrow flight");
        }
        direction = direction.normalize();
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    public Vec3 point(double distance) {
        return origin.add(direction.scale(Math.clamp(distance, 0.0, range)));
    }

    public CompoundTag encode() {
        var tag = new CompoundTag();
        tag.putDouble("X", origin.x);
        tag.putDouble("Y", origin.y);
        tag.putDouble("Z", origin.z);
        tag.putDouble("DX", direction.x);
        tag.putDouble("DY", direction.y);
        tag.putDouble("DZ", direction.z);
        tag.putDouble("Speed", speed);
        tag.putDouble("Range", range);
        tag.putInt("Seed", seed);
        return tag;
    }

    public static LightningArrowFlight decode(CompoundTag tag) {
        return new LightningArrowFlight(new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                new Vec3(tag.getDouble("DX"), tag.getDouble("DY"), tag.getDouble("DZ")),
                tag.getDouble("Speed"), tag.getDouble("Range"), tag.getInt("Seed"));
    }
}
