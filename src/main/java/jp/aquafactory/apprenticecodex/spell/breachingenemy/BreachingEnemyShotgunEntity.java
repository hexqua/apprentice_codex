package jp.aquafactory.apprenticecodex.spell.breachingenemy;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.GunSpellTracerPacket;
import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class BreachingEnemyShotgunEntity extends SummonWeaponEntity {

    public static final int MAX_RECOIL_TICK = 10;
    private static final float TRACER_SPEED_BLOCKS_PER_TICK = 12.0F;
    private static final float TRACER_LENGTH = 8.0F;

    private static final EntityDataAccessor<Integer> RECOIL_TICK =
            SynchedEntityData.defineId(BreachingEnemyShotgunEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_RELEASED =
            SynchedEntityData.defineId(BreachingEnemyShotgunEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage;
    private float range;
    private int count;
    private int recoilTick;
    private boolean isReleased;
    public BreachingEnemyShotgunEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BreachingEnemyShotgunEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RECOIL_TICK, 0);
        builder.define(IS_RELEASED, false);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        range = pCompound.getFloat("Range");
        count = pCompound.getInt("Count");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putFloat("Range", range);
        pCompound.putInt("Count", count);
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                1.5,
                12,
                0.1f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );

        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();

        // 射出時パーティクル(再ログインで消えるので制御不要)
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.2f,
                    8,
                    0.01f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (isReleased) {
            if (recoilTick > 0) {
                --recoilTick;
                entityData.set(RECOIL_TICK, recoilTick);
            } else {
                releaseWeapon();
            }
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var locatePosition = getStandbyPosition();
        followTargetPosition(locatePosition);

        // ブリーチングエネミーは常に視線先を狙う.
        var aimResult = RaycastTools.raycastFromEye(owner, range, 1, e -> CombatTools.isValidCombatTarget(e, this));
        var targetVec = aimResult.hitPosition().subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        hasImpulse = true;
    }

    public void fire(Level level){
        var counts = new HashMap<Integer, Integer>();
        var entities = new HashMap<Integer, Entity>();
        var blockHitPositionList = new ArrayList<Vec3>();
        var entityHitPositionList = new ArrayList<Vec3>();
        var allHitPositionList = new ArrayList<Vec3>();
        var blockCounts = new HashMap<Long, Integer>();

        var baseAngle = getLookAngle();
        for(var i = 0; i < count; i++){
            // 散弾処理をするため、所有者ではなくこの武器から判定を飛ばす.
            var pellet = RaycastTools.randomRotateInCone(baseAngle, 15, level.getRandom());
            var hitResult = RaycastTools.raycast(this, pellet, range, 0.25, e -> CombatTools.isValidCombatTarget(e, this) && e != getOwner());
            if (hitResult.hitEntity() != null){
                // エンダードラゴンは先に解決しておく.
                counts.merge(hitResult.hitEntity().getId(), 1, Integer::sum);
                entities.put(hitResult.hitEntity().getId(), CombatTools.resolutePartEntity(hitResult.hitEntity()));
            }

            if (hitResult.hitBlock() != null){
                blockCounts.merge(hitResult.hitBlock().asLong(), 1, Integer::sum);
            }

            var hitPosition = hitResult.hitPosition();
            switch (hitResult.hitType()){
                case LIVING_ENTITY:
                    entityHitPositionList.add(hitPosition);
                    break;
                case BLOCK:
                    blockHitPositionList.add(hitPosition);
                    break;
            }

            // トレーサー用に無条件に詰める.
            allHitPositionList.add(hitPosition);
        }

        if (level instanceof ServerLevel server) {
            var firePosition = position().add(getLookAngle().normalize().scale(1));
            server.sendParticles(new MuzzleFlashParticleOptions(1.25f), firePosition.x, firePosition.y, firePosition.z, 0, 0, 0, 0, 0);

            for (var hitPosition : blockHitPositionList) {
                server.sendParticles(ParticleTypes.SMOKE, hitPosition.x, hitPosition.y, hitPosition.z, 1, 0, 0, 0, 0);
            }
            for (var hitPosition : entityHitPositionList) {
                server.sendParticles(ParticleTypes.ENCHANTED_HIT, hitPosition.x, hitPosition.y, hitPosition.z, 2, .1, .1, .1, .1);
            }
            
            // 命中に貢献しなかった弾もトレーサーを出す.
            // todo:近隣プレイヤー配信のため、ペレット数分のパケットをまとめる対応を入れるかは実プレイ検証を踏まえて考慮.
            if (getOwner() instanceof ServerPlayer serverPlayer) {
                for (var hitPosition : allHitPositionList) {
                    Networks.sendToTrackingEntityAndSelf(serverPlayer, new GunSpellTracerPacket(
                            firePosition,
                            hitPosition,
                            TRACER_SPEED_BLOCKS_PER_TICK,
                            TRACER_LENGTH
                    ));
                }
            }
        }

        for(var entry : counts.entrySet()){
            var entity = entities.get(entry.getKey());
            if (entity == null){
                continue;
            }

            var finalDamage = damage * entry.getValue();
            var source = createCombatDamageSource(DamageTypes.BREACHING_ENEMY);
            var damaged = CombatTools.applyDamage(entity, finalDamage, source,
                    SpellRegistry.BREACHING_ENEMY.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);

            if (damaged && entity instanceof LivingEntity livingEntity){
                var knockbackDir = livingEntity.position().subtract(position()).normalize().scale(-1);
                livingEntity.knockback(entry.getValue() * 0.25, knockbackDir.x, knockbackDir.z);
            }
        }

        for(var entry : blockCounts.entrySet()){
            // 半分以上当ててなければブロックへの効果は影響させない.
            if (entry.getValue() < count / 2){
                continue;
            }

            var pos = BlockPos.of(entry.getKey());
            var state = level.getBlockState(pos);
            if (state.isAir()){
                continue;
            }

            // 試しにドアをぶち破る.
            if (state.getBlock() instanceof DoorBlock){
                AudioTools.playSoundFromPosition(level, pos.getCenter(), SoundRegistry.VANILLA_BREAK_DOOR.get(), SoundSource.PLAYERS);
                level.destroyBlock(pos, true, getOwner());
            }
        }

        recoilTick = MAX_RECOIL_TICK;
        entityData.set(RECOIL_TICK, MAX_RECOIL_TICK);
        entityData.set(IS_RELEASED, true);
        isReleased = true;
        AudioTools.playSoundFromEntity(level, this, SoundRegistry.SHOTGUN.get(), SoundSource.PLAYERS, 1.0f);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return RotationTools.calculateBehindPosition(owner, -0.8, 1.1, 0.1);
        }

        return Vec3.ZERO;
    }

    public void setDamage(float newDamage) {
        damage = newDamage;
    }
    public void setRange(float newRange) {
        range = newRange;
    }
    public void setCount(int newCount) {
        count = newCount;
    }

    public int getRecoilTick() {
        return entityData.get(RECOIL_TICK);
    }
    public boolean getIsReleased() {
        return entityData.get(IS_RELEASED);
    }
}

