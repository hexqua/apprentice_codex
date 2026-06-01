package jp.aquafactory.apprenticecodex.entity.spellthrowablecard;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class SpellInvokeCardEntity extends AbstractSpellThrowableCardEntity {
    private static final double DRAG = 0.99D;
    private static final double WATER_DRAG = 0.8D;
    private static final double GRAVITY = 0.04D;
    private static final double BLOCK_IMPACT_OFFSET = 0.03D;

    public SpellInvokeCardEntity(EntityType<? extends SpellInvokeCardEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SpellInvokeCardEntity(
            EntityType<? extends SpellInvokeCardEntity> entityType,
            Level level,
            LivingEntity owner,
            ItemStack cardStack
    ) {
        super(entityType, level, owner, cardStack);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) {
            return;
        }

        var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitResult)) {
            onHit(hitResult);
            return;
        }

        move(MoverType.SELF, getDeltaMovement());
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
        var drag = isInWater() ? WATER_DRAG : DRAG;
        setDeltaMovement(getDeltaMovement().scale(drag).add(0.0D, -GRAVITY, 0.0D));
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        if (!level().isClientSide) {
            castAndDiscard(hitResult.getLocation(), resolveForward());
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        if (!level().isClientSide) {
            var castPosition = hitResult.getLocation().add(
                    Vec3.atLowerCornerOf(hitResult.getDirection().getNormal()).scale(BLOCK_IMPACT_OFFSET)
            );
            castAndDiscard(castPosition, resolveForward());
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide && reason == RemovalReason.DISCARDED && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 8, 0.08D, 0.08D, 0.08D, 0.01D);
        }
        super.remove(reason);
    }
}
