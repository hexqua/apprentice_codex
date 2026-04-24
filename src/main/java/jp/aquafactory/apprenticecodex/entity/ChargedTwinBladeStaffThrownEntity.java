package jp.aquafactory.apprenticecodex.entity;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ChargedTwinBladeStaffThrownEntity extends Projectile {
    private static final int THROWN_RENDER_CUSTOM_MODEL_DATA = 1;
    private static final String STACK_TAG = "WeaponStack";
    private static final String SPELL_PAYLOAD_TAG = "SpellPayload";
    private static final String OWNER_UUID_TAG = "OwnerUuid";
    private static final String IMPACTED_TAG = "Impacted";
    private static final String IMPACT_TICK_TAG = "ImpactTick";
    private static final String MAX_FLIGHT_TICKS_TAG = "MaxFlight";
    private static final int IMPACT_LIFETIME_TICKS = 20 * 5;
    private static final int MAX_FLIGHT_TICKS = 20 * 20;
    private static final double DRAG = 0.99D;
    private static final double GRAVITY = 0.05D;

    private ItemStack weaponStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
    private ChargedTwinBladeStaffSpellPayload spellPayload = ChargedTwinBladeStaffSpellPayload.EMPTY;
    private UUID ownerUuid;
    private boolean impacted;
    private int impactTick = -1;
    private int maxFlightTicks = MAX_FLIGHT_TICKS;

    public ChargedTwinBladeStaffThrownEntity(EntityType<? extends ChargedTwinBladeStaffThrownEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ChargedTwinBladeStaffThrownEntity(
            EntityType<? extends ChargedTwinBladeStaffThrownEntity> entityType,
            Level level,
            LivingEntity owner,
            ItemStack weaponStack,
            ChargedTwinBladeStaffSpellPayload spellPayload
    ) {
        super(entityType, level);
        this.weaponStack = weaponStack.copy();
        this.spellPayload = spellPayload;
        this.ownerUuid = owner.getUUID();
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (impacted) {
            if (impactTick >= 0 && tickCount - impactTick >= IMPACT_LIFETIME_TICKS) {
                spawnImpactExpiryParticles();
                discard();
            }
            return;
        }

        if (tickCount > maxFlightTicks) {
            discard();
            return;
        }

        var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitResult)) {
            onHit(hitResult);
            if (impacted) {
                return;
            }
        }

        move(MoverType.SELF, getDeltaMovement());
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
        setDeltaMovement(getDeltaMovement().scale(DRAG).add(0.0D, -GRAVITY, 0.0D));
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (level().isClientSide) {
            return;
        }

        var hitEntity = hitResult.getEntity();
        var owner = getOwner();
        var damageSource = damageSources().trident(this, owner == null ? this : owner);
        var damage = (float) (ChargedTwinBladeStaff.resolveThrownDamage(weaponStack)
                + (hitEntity instanceof LivingEntity livingEntity
                ? EnchantmentHelper.getDamageBonus(weaponStack, livingEntity.getMobType())
                : 0.0F));

        if (hitEntity.hurt(damageSource, damage) && hitEntity instanceof LivingEntity livingTarget) {
            if (owner instanceof LivingEntity livingOwner) {
                EnchantmentHelper.doPostHurtEffects(livingTarget, livingOwner);
                EnchantmentHelper.doPostDamageEffects(livingOwner, livingTarget);
            }
        }

        finishImpact(hitResult.getLocation());
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!level().isClientSide) {
            finishImpact(hitResult.getLocation());
        }
    }

    private void finishImpact(Vec3 impactPosition) {
        setPos(impactPosition.x, impactPosition.y, impactPosition.z);
        setDeltaMovement(Vec3.ZERO);
        impacted = true;
        impactTick = tickCount;

        if (level() instanceof ServerLevel serverLevel) {
            if (canSummonLightning(serverLevel)) {
                var lightningBolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
                if (lightningBolt != null) {
                    lightningBolt.moveTo(impactPosition);
                    if (resolveOwnerPlayer(serverLevel) instanceof ServerPlayer serverPlayer) {
                        lightningBolt.setCause(serverPlayer);
                    }
                    serverLevel.addFreshEntity(lightningBolt);
                    serverLevel.playSound(null, impactPosition.x, impactPosition.y, impactPosition.z,
                            SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 5.0F, 1.0F);
                }
            }

            if (resolveOwnerPlayer(serverLevel) instanceof ServerPlayer serverPlayer) {
                ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                        serverLevel,
                        serverPlayer,
                        weaponStack,
                        spellPayload,
                        impactPosition,
                        resolveImpactForward()
                );
                PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(MagicData.getPlayerMagicData(serverPlayer)));
            }
        }
    }

    private void spawnImpactExpiryParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 18, 0.18D, 0.18D, 0.18D, 0.02D);
    }

    private boolean canSummonLightning(ServerLevel level) {
        return EnchantmentHelper.hasChanneling(weaponStack)
                && level.isThundering()
                && level.canSeeSky(blockPosition());
    }

    private Vec3 resolveImpactForward() {
        var movement = getDeltaMovement();
        if (movement.lengthSqr() > 1.0E-6D) {
            return movement.normalize();
        }
        return calculateViewVector(getXRot(), getYRot());
    }

    private Player resolveOwnerPlayer(ServerLevel level) {
        if (getOwner() instanceof Player player) {
            return player;
        }
        return ownerUuid == null ? null : level.getPlayerByUUID(ownerUuid);
    }

    public ItemStack getRenderStack() {
        var renderStack = weaponStack.copy();
        renderStack.getOrCreateTag().putInt("CustomModelData", THROWN_RENDER_CUSTOM_MODEL_DATA);
        return renderStack;
    }

    public boolean isImpacted() {
        return impacted;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.contains(STACK_TAG)) {
            weaponStack = ItemStack.of(tag.getCompound(STACK_TAG));
        }
        spellPayload = ChargedTwinBladeStaffSpellPayload.load(tag.getCompound(SPELL_PAYLOAD_TAG));
        if (tag.hasUUID(OWNER_UUID_TAG)) {
            ownerUuid = tag.getUUID(OWNER_UUID_TAG);
        }
        impacted = tag.getBoolean(IMPACTED_TAG);
        impactTick = tag.getInt(IMPACT_TICK_TAG);
        maxFlightTicks = tag.contains(MAX_FLIGHT_TICKS_TAG) ? tag.getInt(MAX_FLIGHT_TICKS_TAG) : MAX_FLIGHT_TICKS;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.put(STACK_TAG, weaponStack.save(new CompoundTag()));
        tag.put(SPELL_PAYLOAD_TAG, spellPayload.save());
        if (ownerUuid != null) {
            tag.putUUID(OWNER_UUID_TAG, ownerUuid);
        }
        tag.putBoolean(IMPACTED_TAG, impacted);
        tag.putInt(IMPACT_TICK_TAG, impactTick);
        tag.putInt(MAX_FLIGHT_TICKS_TAG, maxFlightTicks);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 128.0D * 128.0D;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
