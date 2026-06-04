package jp.aquafactory.apprenticecodex.entity.spellthrowablecard;

import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class SpellAutonomyCardEntity extends AbstractSpellThrowableCardEntity {
    private static final String INITIAL_MOVEMENT_TAG = "InitialMovement";
    private static final int STOP_TICKS = 20;
    private static final int CAST_DELAY_TICKS = 20;
    private static final double OWNER_AIM_RANGE = 64.0D;
    private static final double OWNER_AIM_ENTITY_BOX_WIDTH = 0.25D;

    private Vec3 initialMovement = Vec3.ZERO;
    private boolean stopped;

    public SpellAutonomyCardEntity(EntityType<? extends SpellAutonomyCardEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SpellAutonomyCardEntity(
            EntityType<? extends SpellAutonomyCardEntity> entityType,
            Level level,
            LivingEntity owner,
            ItemStack cardStack
    ) {
        super(entityType, level, owner, cardStack);
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, inaccuracy);
        initialMovement = getDeltaMovement();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) {
            return;
        }

        if (tickCount < STOP_TICKS) {
            move(MoverType.SELF, getDeltaMovement());
            ProjectileUtil.rotateTowardsMovement(this, 1.0F);
            var remainingScale = Math.max(0.0D, (STOP_TICKS - tickCount) / (double) STOP_TICKS);
            setDeltaMovement(initialMovement.scale(remainingScale));
            return;
        }

        if (!stopped) {
            stopped = true;
            setDeltaMovement(Vec3.ZERO);
        }

        if (tickCount >= STOP_TICKS + CAST_DELAY_TICKS) {
            castAndDiscard(position(), resolveAutonomyCastForward());
        }
    }

    private Vec3 resolveAutonomyCastForward() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return resolveForward();
        }

        var owner = resolveOwnerPlayer(serverLevel);
        if (owner == null || !owner.isAlive()) {
            return resolveForward();
        }

        var target = RaycastTools.raycastFromEye(
                owner,
                OWNER_AIM_RANGE,
                OWNER_AIM_ENTITY_BOX_WIDTH,
                entity -> entity.isAlive() && entity != owner && entity != this
        );
        if (target.hitType() == RaycastTools.TargetType.BLOCK
                || target.hitType() == RaycastTools.TargetType.LIVING_ENTITY) {
            var targetedForward = target.hitPosition().subtract(position());
            if (targetedForward.lengthSqr() > 1.0E-6D) {
                return targetedForward.normalize();
            }
        }

        var ownerForward = owner.getViewVector(1.0F);
        return ownerForward.lengthSqr() > 1.0E-6D ? ownerForward.normalize() : resolveForward();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide && reason == RemovalReason.DISCARDED && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 12, 0.12D, 0.12D, 0.12D, 0.01D);
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        initialMovement = readVec3ForAutonomy(tag.getCompound(INITIAL_MOVEMENT_TAG));
        stopped = tag.getBoolean("Stopped");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(INITIAL_MOVEMENT_TAG, saveVec3ForAutonomy(initialMovement));
        tag.putBoolean("Stopped", stopped);
    }

    private static CompoundTag saveVec3ForAutonomy(Vec3 vector) {
        var tag = new CompoundTag();
        tag.putDouble("X", vector.x);
        tag.putDouble("Y", vector.y);
        tag.putDouble("Z", vector.z);
        return tag;
    }

    private static Vec3 readVec3ForAutonomy(CompoundTag tag) {
        return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
    }
}
