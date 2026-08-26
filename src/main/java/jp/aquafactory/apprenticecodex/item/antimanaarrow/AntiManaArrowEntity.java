package jp.aquafactory.apprenticecodex.item.antimanaarrow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AntiManaArrowEntity extends AbstractArrow {
    private static final AdditiveGlowParticleOptions PURPLE_SPARK = new AdditiveGlowParticleOptions(
            ParticleRegistry.ADDITIVE_SPARK.get(), 0.08F, 0.541F, 0.169F, 0.886F, 0);
    private static final AdditiveGlowParticleOptions YELLOW_SPARK = new AdditiveGlowParticleOptions(
            ParticleRegistry.ADDITIVE_SPARK.get(), 0.08F, 1.0F, 0.847F, 0.302F, 0);

    public AntiManaArrowEntity(EntityType<AntiManaArrowEntity> type, Level level) {
        super(type, level);
    }

    public AntiManaArrowEntity(Level level, LivingEntity owner) {
        super(EntityRegistry.ANTI_MANA_ARROW.get(), owner, level);
    }

    public AntiManaArrowEntity(Level level, double x, double y, double z) {
        super(EntityRegistry.ANTI_MANA_ARROW.get(), x, y, z, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide || inGround) {
            return;
        }
        var movement = getDeltaMovement();
        var trailOffset = random.nextDouble();
        var x = getX() - movement.x * trailOffset;
        var y = getY() - movement.y * trailOffset;
        var z = getZ() - movement.z * trailOffset;
        level().addParticle(PURPLE_SPARK, x, y, z, 0.0, 0.0, 0.0);
        level().addParticle(YELLOW_SPARK, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        var shield = resolveShield(hitResult.getEntity());
        if (!level().isClientSide && shield != null) {
            playBreakSound(shield);
            shield.onAntiMagic(resolveOwnerMagicData());
            discard();
            return;
        }
        super.onHitEntity(hitResult);
    }

    @Override
    protected void doPostHurtEffects(@NotNull LivingEntity target) {
        super.doPostHurtEffects(target);
        if (!level().isClientSide && target instanceof IMagicSummon summon) {
            playBreakSound(target);
            summon.onAntiMagic(resolveOwnerMagicData());
        }
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        return ItemRegistry.ANTI_MANA_ARROW.get().getDefaultInstance();
    }

    private @Nullable AbstractShieldEntity resolveShield(Entity hitEntity) {
        if (hitEntity instanceof AbstractShieldEntity shield) {
            return shield;
        }
        if (hitEntity instanceof PartEntity<?> part && part.getParent() instanceof AbstractShieldEntity shield) {
            return shield;
        }
        return null;
    }

    private MagicData resolveOwnerMagicData() {
        return getOwner() instanceof LivingEntity living
                ? MagicData.getPlayerMagicData(living)
                : new MagicData(true);
    }

    private void playBreakSound(Entity target) {
        level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundRegistry.VANILLA_DEMICREATOR_BREAK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
