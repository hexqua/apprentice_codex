package jp.aquafactory.apprenticecodex.spell.echoarrow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public final class EchoArrowCoreEntity extends Entity implements AntiMagicSusceptible {
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(EchoArrowCoreEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> DIRECTION = SynchedEntityData.defineId(EchoArrowCoreEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> COUNT = SynchedEntityData.defineId(EchoArrowCoreEntity.class, EntityDataSerializers.INT);
    private LivingEntity owner;
    private EchoArrowEntity initialArrow;
    private Vec3 target;
    private float damage;
    private int remaining;
    private long deadline;
    private long nextShot;
    private double shotPhase;
    private int clientEndTicks;

    public EchoArrowCoreEntity(EntityType<? extends EchoArrowCoreEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public void configure(LivingEntity owner, Vec3 origin, float damage, int count, EchoArrowEntity arrow) {
        this.owner = owner;
        setPos(origin);
        this.damage = damage;
        remaining = Math.max(0, count);
        shotPhase = random.nextDouble() * Math.PI * 2;
        entityData.set(COUNT, remaining);
        initialArrow = arrow;
        deadline = level().getGameTime() + EchoArrowEntity.LIFETIME;
    }

    public static boolean validOwner(Entity owner, Level level) {
        return owner instanceof LivingEntity && owner.level() == level && owner.isAlive() && !owner.isRemoved();
    }

    public void acceptImpact(EchoArrowEntity arrow, Vec3 point) {
        if (level().isClientSide || isRemoved() || target != null || arrow != initialArrow) return;
        target = point;
        initialArrow = null;
        nextShot = level().getGameTime() + 10;
        entityData.set(START, nextShot);
        entityData.set(DIRECTION, point.subtract(position()).normalize().toVector3f());
        deadline = nextShot + Math.max(0, remaining - 1L);
        if (remaining == 0) finish(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        if (!validOwner(owner, level()) || now > deadline || !server.hasChunkAt(blockPosition())) {
            discard();
            return;
        }
        if (target == null) {
            if (initialArrow == null || initialArrow.isRemoved() || now >= deadline) discard();
            return;
        }
        if (now < nextShot) return;
        // tick停止後に遅れた弾をまとめて放たず、その残響を終了する。
        if (now != nextShot) {
            discard();
            return;
        }
        if (now == startTime()) server.sendParticles(ParticleTypes.SONIC_BOOM,
                getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        if ((totalCount() - remaining) % 3 == 0) {
            server.playSound(null, getX(), getY(), getZ(), SoundRegistry.ECHO_ARROW_VOLLEY.get(),
                    SoundSource.PLAYERS, 0.35f, 0.95f + random.nextFloat() * 0.1f);
        }
        fireArrow(server);
        nextShot++;
        if (remaining == 0) finish(false);
    }

    private void fireArrow(ServerLevel server) {
        remaining--;
        // 核自体が埋まっている場合、横ずらしで壁外へ射出口を逃がさない。
        if (blocked(position())) {
            return;
        }
        Vec3 forward = target.subtract(position()).normalize();
        Vec3 axis = Math.abs(forward.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = forward.cross(axis).normalize();
        Vec3 up = right.cross(forward).normalize();
        // 隣接する矢の射出口が偶然重ならないよう、位相を1発ずつ進める。
        double angle = shotPhase;
        shotPhase += Math.PI * 2 / 3;
        Vec3 origin = position().add(right.scale(Math.cos(angle) * 0.12)).add(up.scale(Math.sin(angle) * 0.12));
        if (blocked(origin) || server.clip(new ClipContext(position(), origin, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) origin = position();
        Vec3 direction = target.subtract(origin);
        if (blocked(origin) || direction.lengthSqr() < 1.0e-8) return;
        var arrow = new EchoArrowEntity(EntityRegistry.ECHO_ARROW.get(), server);
        arrow.launch(owner, origin, direction, damage, false, 2.4 + random.nextDouble() * 0.2, null);
        server.addFreshEntity(arrow);
    }

    private boolean blocked(Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        if (!level().hasChunkAt(pos)) return true;
        return level().getBlockState(pos).getCollisionShape(level(), pos, CollisionContext.of(this))
                .toAabbs().stream().anyMatch(box -> box.move(pos).contains(point));
    }

    private void finish(boolean counterspell) {
        level().broadcastEntityEvent(this, (byte) (counterspell ? 62 : 61));
        discard();
    }

    public long startTime() {
        return entityData.get(START);
    }

    public Vec3 direction() {
        return new Vec3(entityData.get(DIRECTION));
    }

    public int totalCount() {
        return entityData.get(COUNT);
    }

    public int clientEndTicks() {
        return clientEndTicks;
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 61 || event == 62) clientEndTicks = event == 61 ? 4 : 2;
        else super.handleEntityEvent(event);
    }

    @Override
    public void onAntiMagic(MagicData data) {
        if (!level().isClientSide && !isRemoved()) finish(true);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        initialArrow = null;
        owner = null;
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(START, -1L);
        builder.define(DIRECTION, new Vector3f(0, 0, 1));
        builder.define(COUNT, 0);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }
}
