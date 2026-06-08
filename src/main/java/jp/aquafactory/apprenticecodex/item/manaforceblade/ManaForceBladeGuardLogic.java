package jp.aquafactory.apprenticecodex.item.manaforceblade;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ManaForceBladeGuardLogic {
    private static final int COST_INTERVAL_TICKS = 20;
    private static final int ACTION_INTERVAL_TICKS = 5;
    private static final int RANGED_GUARD_DURABILITY_COST = 1;
    private static final int MELEE_GUARD_DURABILITY_COST = 2;
    private static final double RANGED_DISTANCE_SQR = 3.0D * 3.0D;
    private static final double PROJECTILE_SPEED = 2.45D;
    private static final int DEFAULT_PROJECTILE_COLOR = 0xFFFFFF;
    private static final int SPARK_LIFETIME = 4;
    private static final int SPARK_LIFETIME_VARIANCE = 3;
    private static final SparkColor[] GUARD_SPARK_COLORS = {
            new SparkColor(1.0F, 0.96F, 0.82F),
            new SparkColor(1.0F, 0.78F, 0.18F),
            new SparkColor(1.0F, 0.38F, 0.08F)
    };
    private static final SparkColor[] BLUE_GUARD_SPARK_COLORS = {
            new SparkColor(0.72F, 0.96F, 1.0F),
            new SparkColor(0.28F, 0.66F, 1.0F),
            new SparkColor(0.12F, 0.36F, 1.0F)
    };
    private static final String RANGED_COST_TICK_TAG = ApprenticeCodex.MODID + ":mana_force_blade_ranged_cost_tick";
    private static final String RANGED_ACTION_TICK_TAG = ApprenticeCodex.MODID + ":mana_force_blade_ranged_action_tick";
    private static final String MELEE_COST_TICK_TAG = ApprenticeCodex.MODID + ":mana_force_blade_melee_cost_tick";
    private static final String MELEE_ACTION_TICK_TAG = ApprenticeCodex.MODID + ":mana_force_blade_melee_action_tick";

    private static boolean applyingGuardCounterDamage;

    private record SparkColor(float red, float green, float blue) {
    }

    private ManaForceBladeGuardLogic() {
    }

    public static boolean isApplyingGuardCounterDamage() {
        return applyingGuardCounterDamage;
    }

    public static boolean isPerfectGuard(int heldTicks) {
        return heldTicks <= ApprenticeCodexServerConfig.manaForceBladePerfectGuardTicks();
    }

    public static boolean tryHandleGuard(
            ServerPlayer player,
            ItemStack stack,
            DamageSource source,
            boolean perfectGuard
    ) {
        return tryHandleGuard(player, stack, source, perfectGuard, true);
    }

    public static boolean tryHandleGuard(
            ServerPlayer player,
            ItemStack stack,
            DamageSource source,
            boolean perfectGuard,
            boolean playGuardEffects
    ) {
        var guarded = isRangedAttack(source, player)
                ? handleRangedGuard(player, stack, source, perfectGuard, playGuardEffects)
                : handleMeleeGuard(player, stack, source, perfectGuard, playGuardEffects);
        if (guarded && perfectGuard) {
            ManaForceBlade.rememberPerfectGuardReleaseCooldownGrace(stack, player.level().getGameTime());
        }
        return guarded;
    }

    private static boolean isRangedAttack(DamageSource source, ServerPlayer player) {
        var directEntity = source.getDirectEntity();
        if (directEntity instanceof Projectile) {
            return true;
        }

        var sourceEntity = source.getEntity();
        return sourceEntity != null && sourceEntity.distanceToSqr(player) >= RANGED_DISTANCE_SQR;
    }

    private static boolean handleRangedGuard(
            ServerPlayer player,
            ItemStack stack,
            DamageSource source,
            boolean perfectGuard,
            boolean playGuardEffects
    ) {
        var now = player.level().getGameTime();
        if (!perfectGuard && !tryPayPeriodicGuardCost(
                player,
                stack,
                RANGED_COST_TICK_TAG,
                now,
                ApprenticeCodexServerConfig.manaForceBladeRangedGuardManaCost(),
                RANGED_GUARD_DURABILITY_COST
        )) {
            return false;
        }

        var origin = resolveGuardOrigin(source, player);
        if (source.getDirectEntity() instanceof Projectile projectile) {
            projectile.discard();
        }

        if (tryMarkAction(stack, RANGED_ACTION_TICK_TAG, now)) {
            shootGuardProjectile(player, stack, origin, perfectGuard);
            if (playGuardEffects) {
                playGuardEffect(player, origin, 12);
            }
        }
        return true;
    }

    private static boolean handleMeleeGuard(
            ServerPlayer player,
            ItemStack stack,
            DamageSource source,
            boolean perfectGuard,
            boolean playGuardEffects
    ) {
        var now = player.level().getGameTime();
        if (!perfectGuard && !tryPayPeriodicGuardCost(
                player,
                stack,
                MELEE_COST_TICK_TAG,
                now,
                ApprenticeCodexServerConfig.manaForceBladeMeleeGuardManaCost(),
                MELEE_GUARD_DURABILITY_COST
        )) {
            return false;
        }

        if (tryMarkAction(stack, MELEE_ACTION_TICK_TAG, now)) {
            var sourceEntity = source.getEntity();
            applyMeleeCounter(player, stack, sourceEntity, perfectGuard);
            if (sourceEntity != null && playGuardEffects) {
                playGuardEffect(player, resolveMeleeSparkPosition(player, sourceEntity), 16);
            }
        }
        return true;
    }

    private static boolean tryPayPeriodicGuardCost(
            ServerPlayer player,
            ItemStack stack,
            String lastCostTickTag,
            long now,
            float manaCost,
            int durabilityCost
    ) {
        var tag = stack.getOrCreateTag();
        if (tag.contains(lastCostTickTag) && now - tag.getLong(lastCostTickTag) < COST_INTERVAL_TICKS) {
            return true;
        }

        if (manaCost > 0.0F) {
            var magicData = MagicData.getPlayerMagicData(player);
            if (!player.getAbilities().instabuild && (magicData == null || magicData.getMana() < manaCost)) {
                return false;
            }
        }

        ManaForceBlade.spendMana(player, manaCost);
        stack.hurtAndBreak(durabilityCost, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        tag.putLong(lastCostTickTag, now);
        return true;
    }

    private static boolean tryMarkAction(ItemStack stack, String actionTickTag, long now) {
        var tag = stack.getOrCreateTag();
        if (tag.contains(actionTickTag) && now - tag.getLong(actionTickTag) < ACTION_INTERVAL_TICKS) {
            return false;
        }

        tag.putLong(actionTickTag, now);
        return true;
    }

    private static Vec3 resolveGuardOrigin(DamageSource source, ServerPlayer player) {
        var directEntity = source.getDirectEntity();
        if (directEntity instanceof Projectile) {
            return directEntity.position();
        }

        var sourceEntity = source.getEntity();
        if (sourceEntity != null) {
            return sourceEntity.position().add(0.0D, sourceEntity.getBbHeight() * 0.5D, 0.0D);
        }
        return player.getEyePosition();
    }

    private static void shootGuardProjectile(ServerPlayer player, ItemStack stack, Vec3 origin, boolean perfectGuard) {
        var projectile = new ManaForceBladeProjectileEntity(EntityRegistry.MANA_FORCE_BLADE_PROJECTILE.get(), player.level(), player);
        projectile.setPos(origin);
        projectile.setDamage(ManaForceBlade.resolveFinalAttackDamage(player, stack) * (perfectGuard ? 1.5F : 1.0F));
        projectile.setColor(resolveProjectileColor(stack));
        projectile.setProjectileVelocity(player.getLookAngle().normalize(), PROJECTILE_SPEED);
        player.level().addFreshEntity(projectile);
    }

    private static int resolveProjectileColor(ItemStack stack) {
        var school = MagicTools.getImbuedSpellSchool(stack);
        if (school == null) {
            return DEFAULT_PROJECTILE_COLOR;
        }

        var color = school.getDisplayName().getStyle().getColor();
        return color != null ? color.getValue() : DEFAULT_PROJECTILE_COLOR;
    }

    public static void playBlueGuardEffect(ServerPlayer player, Vec3 position, int sparkCount) {
        playGuardEffect(player, position, sparkCount, BLUE_GUARD_SPARK_COLORS);
    }

    private static void playGuardEffect(ServerPlayer player, Vec3 position, int sparkCount) {
        playGuardEffect(player, position, sparkCount, GUARD_SPARK_COLORS);
    }

    private static void playGuardEffect(ServerPlayer player, Vec3 position, int sparkCount, SparkColor[] sparkColors) {
        var level = player.level();
        AudioTools.playSoundFromPosition(level, position, SoundRegistry.PARRY.get(), SoundSource.PLAYERS);
        if (level instanceof ServerLevel serverLevel) {
            spawnGuardSparks(serverLevel, position, sparkCount, sparkColors);
        }
    }

    private static void spawnGuardSparks(ServerLevel level, Vec3 position, int count, SparkColor[] sparkColors) {
        for (var i = 0; i < sparkColors.length; i++) {
            var color = sparkColors[i];
            var colorCount = count / sparkColors.length + (i < count % sparkColors.length ? 1 : 0);
            level.sendParticles(
                    createGuardSpark(color),
                    position.x, position.y, position.z,
                    colorCount,
                    0.18D, 0.18D, 0.18D,
                    0.08D
            );
        }
    }

    private static AdditiveGlowParticleOptions createGuardSpark(SparkColor color) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                0.12F,
                color.red(),
                color.green(),
                color.blue(),
                1,
                SPARK_LIFETIME,
                SPARK_LIFETIME_VARIANCE,
                -1.0F,
                -1.0F,
                -1.0F,
                -1.0F,
                -1.0F,
                -1.0F,
                -1.0F,
                true
        );
    }

    private static Vec3 resolveMeleeSparkPosition(ServerPlayer player, Entity sourceEntity) {
        return player.getBoundingBox().getCenter()
                .add(sourceEntity.getBoundingBox().getCenter())
                .scale(0.5D);
    }

    private static void applyMeleeCounter(ServerPlayer player, ItemStack stack, @Nullable Entity sourceEntity, boolean perfectGuard) {
        if (sourceEntity == null) {
            return;
        }

        var direction = sourceEntity.position().subtract(player.position());
        if (direction.lengthSqr() < 1.0E-7D) {
            direction = player.getLookAngle();
        }
        direction = direction.normalize();
        sourceEntity.push(direction.x * 1.6D, 0.35D, direction.z * 1.6D);
        sourceEntity.hurtMarked = true;

        if (perfectGuard && sourceEntity instanceof LivingEntity livingTarget) {
            applyingGuardCounterDamage = true;
            try {
                CombatTools.applyDamage(
                        livingTarget,
                        ManaForceBlade.resolveFinalAttackDamage(player, stack),
                        CombatTools.getDamageSource(player.level(), player, DamageTypes.MANA_FORCE_BLADE),
                        null,
                        CombatTools.KnockbackTypes.NO_KNOCKBACK
                );
            } finally {
                applyingGuardCounterDamage = false;
            }
        }
    }
}
