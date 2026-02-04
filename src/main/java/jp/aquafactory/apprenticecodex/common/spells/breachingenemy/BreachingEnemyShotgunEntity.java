package jp.aquafactory.apprenticecodex.common.spells.breachingenemy;

import jp.aquafactory.apprenticecodex.client.particles.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.common.entity.spell.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.common.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.utility.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class BreachingEnemyShotgunEntity extends SummonWeaponEntity {

    private float damage;
    private float range;
    private int count;

    public BreachingEnemyShotgunEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BreachingEnemyShotgunEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        // do nothing.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
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
                0.2f,
                8,
                0.01f,
                0.01,
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

        if (level.isClientSide) {
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

            var hitPosition = hitResult.hitPosition();
            switch (hitResult.hitType()){
                case LIVING_ENTITY:
                    entityHitPositionList.add(hitPosition);
                    break;
                case BLOCK:
                    blockHitPositionList.add(hitPosition);
                    break;
            }
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
        }

        for(var entry : counts.entrySet()){
            var entity = entities.get(entry.getKey());
            if (entity == null){
                continue;
            }

            var finalDamage = damage * entry.getValue();
            var source = CombatTools.getDamageSource(level(), this, getOwner(), "breaching_enemy");
            CombatTools.applyDamage(entity, finalDamage, source, SpellsRegistry.BREACHING_ENEMY.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);

            if (entity instanceof LivingEntity livingEntity){
                var knockbackDir = livingEntity.position().subtract(position()).normalize().scale(-1);
                livingEntity.knockback(entry.getValue() * 0.25, knockbackDir.x, knockbackDir.z);
            }
        }

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
}
