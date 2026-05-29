package jp.aquafactory.apprenticecodex.spell.inscribeice;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InscribeIceDaggerThrowJob {
    private static final int MAX_RELEASE_TICKS = 3;
    private static final double YAW_JITTER = Math.tan(4.0D * Mth.DEG_TO_RAD);
    private static final double PITCH_JITTER = Math.tan(2.5D * Mth.DEG_TO_RAD);
    private static final double RIGHT_POSITION_JITTER = 0.12D;
    private static final double UP_POSITION_JITTER = 0.12D;
    private static final double FORWARD_POSITION_JITTER = 0.05D;
    private static final double SPEED_JITTER = 0.08D;

    private final LivingEntity caster;
    private final float damage;
    private final float burstDamage;
    private final List<List<Integer>> releaseBuckets;
    private final long creationGameTime;
    private final Vec3 fixedBasePosition;
    private final Vec3 fixedForward;
    private int releaseTick;
    private long nextReleaseGameTime;
    private boolean complete;

    public InscribeIceDaggerThrowJob(ServerLevel level, LivingEntity caster, int projectileCount,
                                     float damage, float burstDamage) {
        this.caster = caster;
        this.damage = damage;
        this.burstDamage = burstDamage;
        releaseBuckets = createReleaseBuckets(projectileCount, level.random);
        creationGameTime = level.getGameTime();
        nextReleaseGameTime = creationGameTime;
        fixedBasePosition = null;
        fixedForward = null;
    }

    public InscribeIceDaggerThrowJob(ServerLevel level, LivingEntity caster, int projectileCount,
                                     float damage, float burstDamage, Vec3 fixedBasePosition, Vec3 fixedForward) {
        this.caster = caster;
        this.damage = damage;
        this.burstDamage = burstDamage;
        releaseBuckets = createReleaseBuckets(projectileCount, level.random);
        creationGameTime = level.getGameTime();
        nextReleaseGameTime = creationGameTime;
        this.fixedBasePosition = fixedBasePosition;
        this.fixedForward = fixedForward.lengthSqr() > 1.0E-8D ? fixedForward.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    public boolean isComplete() {
        return complete;
    }

    public void tick(ServerLevel level) {
        if (complete) {
            return;
        }

        if (!isCasterValid(level) || level.getGameTime() > creationGameTime + MAX_RELEASE_TICKS) {
            complete = true;
            return;
        }

        if (level.getGameTime() < nextReleaseGameTime) {
            return;
        }

        releaseBucket(level);
    }

    private void releaseBucket(ServerLevel level) {
        if (releaseTick >= releaseBuckets.size()) {
            complete = true;
            return;
        }

        var forward = fixedForward != null ? fixedForward : InscribeIce.getLookForward(caster);
        var right = InscribeIce.getRightVector(caster, forward);
        var up = right.cross(forward).normalize();
        var projectileCount = releaseBuckets.stream().mapToInt(List::size).sum();
        var arcDegrees = InscribeIce.getArcDegrees(projectileCount);
        var basePosition = fixedBasePosition != null
                ? fixedBasePosition
                : InscribeIce.calculateDaggerLaunchPosition(caster, forward);

        for (var index : releaseBuckets.get(releaseTick)) {
            spawnDagger(level, forward, right, up, basePosition, projectileCount, arcDegrees, index);
        }

        ++releaseTick;
        if (releaseTick >= releaseBuckets.size()) {
            complete = true;
        } else {
            nextReleaseGameTime = level.getGameTime() + 1L;
        }
    }

    private void spawnDagger(ServerLevel level, Vec3 forward, Vec3 right, Vec3 up, Vec3 basePosition,
                             int projectileCount, float arcDegrees, int index) {
        var random = level.random;
        var projectile = new InscribeIceDaggerEntity(EntityRegistry.INSCRIBE_ICE_DAGGER.get(), level, caster);
        var direction = InscribeIce.calculateProjectileDirection(forward, right, projectileCount, arcDegrees, index);
        direction = addDirectionJitter(direction, right, up, random);
        var spawnPosition = basePosition
                .add(right.scale(centered(random) * RIGHT_POSITION_JITTER))
                .add(up.scale(centered(random) * UP_POSITION_JITTER))
                .add(forward.scale(centered(random) * FORWARD_POSITION_JITTER));
        var speed = InscribeIceDaggerEntity.SPEED * (1.0D + centered(random) * SPEED_JITTER);

        projectile.setPos(spawnPosition);
        projectile.setDamage(damage);
        projectile.setBurstDamage(burstDamage);
        projectile.setProjectileVelocity(direction, speed);
        level.addFreshEntity(projectile);
    }

    private boolean isCasterValid(ServerLevel level) {
        return caster != null
                && caster.level() == level
                && !caster.isRemoved()
                && caster.isAlive();
    }

    private static List<List<Integer>> createReleaseBuckets(int projectileCount, RandomSource random) {
        var bucketCount = projectileCount <= 3 ? 1 : projectileCount <= 5 ? 2 : MAX_RELEASE_TICKS;
        var indices = new ArrayList<Integer>(projectileCount);
        for (var i = 0; i < projectileCount; ++i) {
            indices.add(i);
        }
        Collections.shuffle(indices, new java.util.Random(random.nextLong()));

        var buckets = new ArrayList<List<Integer>>(bucketCount);
        for (var i = 0; i < bucketCount; ++i) {
            buckets.add(new ArrayList<>());
        }
        for (var i = 0; i < indices.size(); ++i) {
            buckets.get(i % bucketCount).add(indices.get(i));
        }
        return buckets;
    }

    private static Vec3 addDirectionJitter(Vec3 direction, Vec3 right, Vec3 up, RandomSource random) {
        return direction
                .add(right.scale(centered(random) * YAW_JITTER))
                .add(up.scale(centered(random) * PITCH_JITTER))
                .normalize();
    }

    private static double centered(RandomSource random) {
        return random.nextDouble() * 2.0D - 1.0D;
    }
}
