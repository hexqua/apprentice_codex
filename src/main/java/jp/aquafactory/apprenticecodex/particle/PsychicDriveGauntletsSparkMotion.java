package jp.aquafactory.apprenticecodex.particle;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsVisualState;
import net.minecraft.world.phys.Vec3;

public final class PsychicDriveGauntletsSparkMotion {
    public record Contact(Vec3 velocity, int lifetime, boolean bounced, boolean removed) {}

    private PsychicDriveGauntletsSparkMotion() {}

    public static Contact collide(Vec3 requested, Vec3 actual, int age, int lifetime, boolean bounced) {
        double x = requested.x == actual.x ? requested.x : 0;
        double z = requested.z == actual.z ? requested.z : 0;
        boolean ground = requested.y < 0 && requested.y != actual.y;
        if (ground) {
            if (bounced) return new Contact(Vec3.ZERO, lifetime, true, true);
            return new Contact(new Vec3(x * 0.5, -requested.y * PsychicDriveGauntletsVisualState.SPARK_BOUNCE_FACTOR, z * 0.5),
                    Math.min(lifetime, age + PsychicDriveGauntletsVisualState.SPARK_BOUNCE_TICKS), true, false);
        }
        double y = requested.y == actual.y ? requested.y : 0;
        return new Contact(new Vec3(x, y, z), lifetime, bounced, false);
    }
}
