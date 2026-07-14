package jp.aquafactory.apprenticecodex.spell.fieldoverseer;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.particle.ZapParticleOption;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.PlacementHelper;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FieldOverseerStaffEntity extends PathfinderMob implements GeoEntity, IMagicSummon {
    public static final float WIDTH = 0.5F;
    public static final float HEIGHT = 1.0F;
    private static final int ATTACK_INTERVAL = 40;
    private static final int CHARGE_TICKS = 10;
    private static final int SHOT_INTERVAL = 3;
    private static final int SHOT_ANIMATION_TICKS = 20;
    private static final int MAX_TARGETS = 3;
    private static final int MANA_TRANSFER_INTERVAL = 5;
    private static final float MANA_TRANSFER_LIMIT = 20.0F;
    private static final double MANA_TRANSFER_RANGE_SQR = 8.0D * 8.0D;
    private static final float IMPACT_RADIUS = 1.0F;

    private static final RawAnimation IDLE_MAIN = RawAnimation.begin().thenLoop("idle_main");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenLoop("charge");
    private static final RawAnimation SHOT = RawAnimation.begin().thenPlay("shot");

    private static final EntityDataAccessor<Float> CURRENT_MANA =
            SynchedEntityData.defineId(FieldOverseerStaffEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_MANA =
            SynchedEntityData.defineId(FieldOverseerStaffEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ATTACK_MANA_COST =
            SynchedEntityData.defineId(FieldOverseerStaffEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIMATION_STATE =
            SynchedEntityData.defineId(FieldOverseerStaffEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(FieldOverseerStaffEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private BlockPos anchorPos = BlockPos.ZERO;
    private float damage;
    private double targetingRadius = 24.0D;
    private int attackSequenceTick = -1;
    private int shotAnimationTicks;
    private final List<UUID> selectedTargets = new ArrayList<>(MAX_TARGETS);
    private UUID ownerUuid;
    private LivingEntity cachedOwner;

    public FieldOverseerStaffEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public static AABB makePlacementAabb(Vec3 center) {
        var halfWidth = WIDTH / 2.0D;
        return new AABB(center.x - halfWidth, center.y, center.z - halfWidth,
                center.x + halfWidth, center.y + HEIGHT, center.z + halfWidth);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURRENT_MANA, 0.0F);
        builder.define(MAX_MANA, 0.0F);
        builder.define(ATTACK_MANA_COST, 0);
        builder.define(ANIMATION_STATE, 0);
        builder.define(OWNER_UUID, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    public void configure(BlockPos anchorPos, float damage, double targetingRadius, int attackManaCost,
                          float maxMana, int maxHealth) {
        this.anchorPos = anchorPos.immutable();
        this.damage = damage;
        this.targetingRadius = Math.max(0.0D, targetingRadius);
        entityData.set(ATTACK_MANA_COST, Math.max(0, attackManaCost));
        entityData.set(MAX_MANA, Math.max(0.0F, maxMana));
        entityData.set(CURRENT_MANA, Math.max(0.0F, maxMana));
        var maxHealthAttribute = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(Math.max(1, maxHealth));
        }
        setHealth(getMaxHealth());
    }

    public void setOwner(LivingEntity owner) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
        entityData.set(OWNER_UUID, Optional.of(ownerUuid));
    }

    @Override
    public @Nullable Entity getSummoner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }
        var managedOwner = SummonManager.getOwner(this);
        if (managedOwner instanceof LivingEntity livingOwner) {
            ownerUuid = livingOwner.getUUID();
            cachedOwner = livingOwner;
            return livingOwner;
        }
        if (ownerUuid != null && level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(ownerUuid) instanceof LivingEntity livingOwner) {
            cachedOwner = livingOwner;
            return livingOwner;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (level() instanceof ServerLevel serverLevel) {
            tickServer(serverLevel);
        }
    }

    private void tickServer(ServerLevel level) {
        updateSupportState();
        transferOwnerMana(level);

        if (shotAnimationTicks > 0 && --shotAnimationTicks == 0) {
            entityData.set(ANIMATION_STATE, attackSequenceTick >= 0 ? 1 : 0);
        }

        if (attackSequenceTick >= 0) {
            attackSequenceTick++;
            var relativeAttackTick = attackSequenceTick - CHARGE_TICKS;
            if (relativeAttackTick >= 0 && relativeAttackTick % SHOT_INTERVAL == 0) {
                var targetIndex = relativeAttackTick / SHOT_INTERVAL;
                if (targetIndex < selectedTargets.size()) {
                    strike(level, targetIndex, targetIndex == 0);
                }
                if (targetIndex + 1 >= selectedTargets.size()) {
                    attackSequenceTick = -1;
                    selectedTargets.clear();
                    if (shotAnimationTicks <= 0) {
                        entityData.set(ANIMATION_STATE, 0);
                    }
                }
            }
        }

        if (attackSequenceTick < 0 && tickCount % ATTACK_INTERVAL == 0) {
            selectTargets(level);
        }
    }

    private void updateSupportState() {
        if (PlacementHelper.hasSupportBelow(level(), anchorPos)) {
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
            var y = PlacementHelper.getSupportTopY(level(), anchorPos);
            var center = new Vec3(anchorPos.getX() + 0.5D, y, anchorPos.getZ() + 0.5D);
            if (position().distanceToSqr(center) > 0.0001D) {
                setPos(center.x, center.y, center.z);
            }
        } else {
            setNoGravity(false);
        }
    }

    private void selectTargets(ServerLevel level) {
        var owner = getSummoner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            return;
        }
        var rangeSqr = targetingRadius * targetingRadius;
        var candidates = level.getEntitiesOfClass(
                        LivingEntity.class,
                        getBoundingBox().inflate(targetingRadius),
                        target -> target != this
                                && target.isAlive()
                                && target.distanceToSqr(this) <= rangeSqr
                                && CombatTools.isValidCombatTarget(target, livingOwner)
                                && CombatTools.canBeHostileToMe(target, livingOwner)
                                && RaycastTools.hasLineOfSight(level, this, target)
                ).stream()
                .sorted(Comparator.comparingDouble(LivingEntity::getHealth).reversed())
                .limit(MAX_TARGETS)
                .toList();
        if (candidates.isEmpty() || !consumeAttackMana()) {
            return;
        }
        selectedTargets.clear();
        candidates.forEach(target -> selectedTargets.add(target.getUUID()));
        attackSequenceTick = 0;
        entityData.set(ANIMATION_STATE, 1);
    }

    private boolean consumeAttackMana() {
        var cost = getAttackManaCost();
        var mana = getCurrentMana();
        if (mana + 0.0001F < cost) {
            return false;
        }
        entityData.set(CURRENT_MANA, Math.max(0.0F, mana - cost));
        return true;
    }

    private void strike(ServerLevel level, int targetIndex, boolean playShotAnimation) {
        var rawTarget = level.getEntity(selectedTargets.get(targetIndex));
        if (rawTarget == null || !rawTarget.isAlive()) {
            return;
        }
        var impact = RaycastTools.getEntityTargetPosition(rawTarget);
        var owner = getSummoner();
        var source = owner != null
                ? CombatTools.getDamageSource(level, this, owner, DamageTypes.FIELD_OVERSEER)
                : CombatTools.getDamageSource(level, this, DamageTypes.FIELD_OVERSEER);
        var targets = CombatTools.resolveUniqueCombatTargets(level.getEntities(
                this,
                new AABB(impact, impact).inflate(IMPACT_RADIUS),
                target -> target.getBoundingBox().distanceToSqr(impact) <= IMPACT_RADIUS * IMPACT_RADIUS
                        && CombatTools.isValidCombatTarget(target, owner)
        ));
        for (var target : targets) {
            CombatTools.applyDamage(target, damage, source, SpellRegistry.FIELD_OVERSEER.get().getSchoolType(),
                    CombatTools.KnockbackTypes.NO_KNOCKBACK);
            if (target instanceof Mob mob && !mob.isAlliedTo(this)) {
                mob.setTarget(this);
                mob.setLastHurtByMob(this);
            }
        }
        playStrikeEffects(level, impact);
        if (playShotAnimation) {
            entityData.set(ANIMATION_STATE, 2);
            shotAnimationTicks = SHOT_ANIMATION_TICKS;
        }
    }

    private void playStrikeEffects(ServerLevel level, Vec3 impact) {
        var top = impact.add(0.0D, 15.0D, 0.0D);
        var middle = impact.add(
                level.random.nextDouble() * 4.0D - 2.0D,
                4.0D + level.random.nextDouble() * 7.0D,
                level.random.nextDouble() * 4.0D - 2.0D);
        MagicManager.spawnParticles(level, new ZapParticleOption(top), middle.x, middle.y, middle.z,
                1, 0, 0, 0, 0, true);
        MagicManager.spawnParticles(level, new ZapParticleOption(middle), impact.x, impact.y, impact.z,
                1, 0, 0, 0, 0, true);
        MagicManager.spawnParticles(level, ParticleHelper.ELECTRIC_SPARKS, impact.x, impact.y, impact.z,
                25, 0.2F, 0.2F, 0.2F, 0.25D, true);
        MagicManager.spawnParticles(level, ParticleHelper.FIERY_SPARKS, impact.x, impact.y, impact.z,
                5, 0.2F, 0.2F, 0.2F, 0.125D, true);
        level.playSound(null, impact.x, impact.y, impact.z,
                io.redspace.ironsspellbooks.registries.SoundRegistry.SMALL_LIGHTNING_STRIKE.get(),
                SoundSource.PLAYERS, 2.0F, 0.8F + level.random.nextFloat() * 0.5F);
    }

    private void transferOwnerMana(ServerLevel level) {
        if (tickCount % MANA_TRANSFER_INTERVAL != 0) {
            return;
        }
        var owner = getSummoner();
        if (!(owner instanceof ServerPlayer player) || player.level() != level
                || player.distanceToSqr(this) > MANA_TRANSFER_RANGE_SQR) {
            return;
        }
        var missing = getMaxStaffMana() - getCurrentMana();
        if (missing <= 0.0001F) {
            return;
        }
        var magicData = MagicData.getPlayerMagicData(player);
        var amount = Math.min(Math.min(missing, MANA_TRANSFER_LIMIT), magicData.getMana());
        if (amount <= 0.0001F) {
            return;
        }
        magicData.setMana(Math.max(0.0F, magicData.getMana() - amount));
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        entityData.set(CURRENT_MANA, Math.min(getMaxStaffMana(), getCurrentMana() + amount));
        spawnManaTransferParticles(level, player);
    }

    private void spawnManaTransferParticles(ServerLevel level, ServerPlayer player) {
        var start = player.getEyePosition().subtract(0.0D, 0.25D, 0.0D);
        var end = position().add(0.0D, HEIGHT * 0.65D, 0.0D);
        var travel = end.subtract(start);
        for (var i = 1; i <= 4; i++) {
            var point = start.add(travel.scale(i / 5.0D));
            var particle = i % 3 == 0 ? createManaRhombus() : createManaSpark();
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.025D, 0.025D, 0.025D, 0.0D);
        }
    }

    private static AdditiveGlowParticleOptions createManaSpark() {
        return new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), 0.11F,
                0.35F, 0.75F, 1.0F, 2, 11, 3, 0.65F, 1.3F,
                0.62F, 0.95F, 0.08F, 0.42F, 0.55F, true);
    }

    private static AdditiveGlowParticleOptions createManaRhombus() {
        return new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_RHOMBUS.get(), 0.16F,
                0.35F, 0.75F, 1.0F, 3, 14, 4, 0.75F, 1.25F,
                0.5F, 0.82F, 0.08F, 0.58F, 0.35F, true);
    }

    public float getCurrentMana() {
        return entityData.get(CURRENT_MANA);
    }

    public float getMaxStaffMana() {
        return entityData.get(MAX_MANA);
    }

    public int getAttackManaCost() {
        return entityData.get(ATTACK_MANA_COST);
    }

    public double getTargetingRadius() {
        return targetingRadius;
    }

    public boolean isOwnedBy(Entity entity) {
        return entityData.get(OWNER_UUID).filter(entity.getUUID()::equals).isPresent();
    }

    public @Nullable String getOwnerName() {
        var owner = getSummoner();
        return owner != null ? owner.getName().getString() : null;
    }

    public boolean hasEnoughManaToAttack() {
        return getCurrentMana() + 0.0001F >= getAttackManaCost();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (shouldIgnoreDamage(source)) {
            return false;
        }
        var owner = getSummoner();
        if (owner != null && (source.getEntity() == owner || source.getDirectEntity() == owner)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(@NotNull Entity entity) {
        var owner = getSummoner();
        return entity == this || entity == owner || isAlliedHelper(entity) || owner != null && owner.isAlliedTo(entity);
    }

    @Override
    public void onUnSummon() {
        discard();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        onRemovedHelper(this);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        onDeathHelper();
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("AnchorPos", anchorPos.asLong());
        tag.putFloat("Damage", damage);
        tag.putDouble("TargetingRadius", targetingRadius);
        tag.putFloat("CurrentMana", getCurrentMana());
        tag.putFloat("MaxMana", getMaxStaffMana());
        tag.putInt("AttackManaCost", getAttackManaCost());
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        anchorPos = tag.contains("AnchorPos") ? BlockPos.of(tag.getLong("AnchorPos")) : blockPosition();
        damage = tag.getFloat("Damage");
        targetingRadius = tag.contains("TargetingRadius") ? tag.getDouble("TargetingRadius") : 24.0D;
        entityData.set(CURRENT_MANA, tag.getFloat("CurrentMana"));
        entityData.set(MAX_MANA, tag.getFloat("MaxMana"));
        entityData.set(ATTACK_MANA_COST, tag.getInt("AttackManaCost"));
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        entityData.set(OWNER_UUID, Optional.ofNullable(ownerUuid));
        cachedOwner = null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_main", 0, state -> {
            state.setAnimation(IDLE_MAIN);
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "action", 0, state -> {
            var animation = switch (entityData.get(ANIMATION_STATE)) {
                case 1 -> CHARGE;
                case 2 -> SHOT;
                default -> IDLE;
            };
            state.setAnimation(animation);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
