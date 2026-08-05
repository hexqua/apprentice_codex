package jp.aquafactory.apprenticecodex.spell.mantisleap;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.ICastHighlightSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MantisLeap extends AbstractSummonWeaponSpell<MantisLeapBladeEntity> implements ICastHighlightSpell {
    private static final double RAYCAST_WIDTH = 1.0;
    private static final double TARGET_STOP_DISTANCE = 1.0;
    private static final double MIN_LEAP_DISTANCE = 0.25;
    private static final int MIN_LEAP_DURATION_TICKS = 10;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mantis_leap");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(8)
            .build();

    public MantisLeap() {
        super(MantisLeapBladeEntity.class);
        baseSpellPower = 1200;
        spellPowerPerLevel = 400;
        baseManaCost = 70;
        manaCostPerLevel = 15;
        castTime = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.MANTIS_LEAP);
    }

    private double getRange(int spellLevel, LivingEntity entity) {
        return Math.min(32, getSpellPower(spellLevel, entity) / 100.0);
    }

    @Override
    public int getHighlightColor() {
        return 0x4488FF;
    }

    @Override
    @Nullable
    public Entity getHighlightEntity(@NotNull Player player, int skillLevel) {
        return RaycastTools.raycastFromEye(
                player,
                getRange(skillLevel, player),
                RAYCAST_WIDTH,
                e -> CombatTools.isValidCombatTarget(e, player)
        ).hitEntity();
    }

    private double getLeapTicksPerBlock(int spellLevel, LivingEntity entity) {
        return Math.max(0.25, 2.5 - getSpellPower(spellLevel, entity) / 800.0);
    }

    private double getLeapArcHeight() {
        return 1.5;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public boolean canBeInterrupted(@Nullable Player player) {
        // 中断されない.
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.MANTIS.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public MantisLeapBladeEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new MantisLeapBladeEntity(EntityRegistry.MANTIS_LEAP_BLADE.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    protected void onInitialCastWithWeapon(Level level, int spellLevel, LivingEntity entity,
                                           MagicData playerMagicData, @NotNull MantisLeapBladeEntity weapon) {
        // FocusStaffbow などの完了時補正を、跳躍後に行う斬撃へ保持する。
        weapon.setDamage(getDamage(spellLevel, entity));
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull MantisLeapBladeEntity weapon) {
        weapon.setDamage(getDamage(spellLevel, entity));
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull MantisLeapBladeEntity weapon) {
        if (cancelled) {
            return CompleteCastTypes.RELEASE_WEAPON;
        }

        // サーバー側で移動パラメータを確定する。ここで決まる値が権威状態になる.
        var destination = resolveLeapDestination(spellLevel, entity);
        var started = startLeap(
                entity,
                destination,
                getLeapTicksPerBlock(spellLevel, entity),
                getLeapArcHeight(),
                weapon.getId()
        );
        if (started) {
            // サーバー権威の方で跳躍開始成功で音を鳴らす.
            AudioTools.playSoundFromEntity(level, entity, SoundRegistry.VANILLA_HIGH_JUMP.get(), SoundSource.PLAYERS);
        } else {
            weapon.slash(level);
        }

        return CompleteCastTypes.KEEP_WEAPON;
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        if (level.isClientSide) {
            // クライアント予測:
            // サーバー同期を待ってから動かすと詠唱完了後に硬直感が出るため,
            // クライアントでも同一式で先行して跳躍を開始する.
            //
            // 予測ズレ抑制:
            // resolveLeapDestination/startLeap の計算内容はサーバーと揃える前提.
            // 片側だけ変更すると補正ワープが目立つ.
            var destination = resolveLeapDestination(spellLevel, entity);
            startLeap(
                    entity,
                    destination,
                    getLeapTicksPerBlock(spellLevel, entity),
                    getLeapArcHeight(),
                    // 斬撃はサーバー権威でのみ発火するため、予測時は blade 紐付け不要.
                    -1
            );
        }

        super.onClientCast(level, spellLevel, entity, castData);
    }

    private Vec3 resolveLeapDestination(int spellLevel, LivingEntity caster) {
        // 着地点解決を共通化し、クライアント予測とサーバー実移動で同じ入力を使う.
        // これがずれると移動終端での補正頻度が上がり、プレイフィールが悪化する.
        var range = getRange(spellLevel, caster);
        var start = caster.position();
        var look = caster.getLookAngle().normalize();

        // boxWidthは吸い付きやすさに影響するため、ちょっと広めに取る.
        var result = RaycastTools.raycastFromEye(caster, range, RAYCAST_WIDTH, e -> CombatTools.isValidCombatTarget(e, caster));

        Vec3 destination;
        if (result.hitType() == RaycastTools.TargetType.LIVING_ENTITY && result.hitEntity() != null) {
            destination = resolveEntityDestination(caster, result.hitEntity(), result.hitPosition());
        } else if (result.hitType() == RaycastTools.TargetType.BLOCK) {
            destination = result.hitPosition();
        } else {
            destination = start.add(look.scale(range));
        }

        return clampDistance(start, destination, range);
    }

    private Vec3 resolveEntityDestination(LivingEntity caster, Entity target, Vec3 hitPosition) {
        var look = caster.getLookAngle().normalize();
        var stopDistance = Math.max(TARGET_STOP_DISTANCE, target.getBbWidth() * 0.5);
        var destination = hitPosition.subtract(look.scale(stopDistance));
        return new Vec3(destination.x, target.getY(), destination.z);
    }

    private Vec3 clampDistance(Vec3 start, Vec3 target, double maxDistance) {
        var offset = target.subtract(start);
        var distance = offset.length();
        if (distance <= maxDistance || distance <= 1.0e-6) {
            return target;
        }
        return start.add(offset.scale(maxDistance / distance));
    }

    private boolean startLeap(LivingEntity entity, Vec3 destination, double ticksPerBlock, double arcHeight, int bladeEntityId) {
        var spellData = Capabilities.getSpellDataOrNull(entity);
        if (spellData == null) {
            return false;
        }

        var start = entity.position();
        var offset = destination.subtract(start);
        if (offset.lengthSqr() < MIN_LEAP_DISTANCE * MIN_LEAP_DISTANCE) {
            return false;
        }

        var distance = offset.length();
        var durationTicks = Math.max(MIN_LEAP_DURATION_TICKS, (int) Math.ceil(distance * Math.max(0.0, ticksPerBlock)));
        var safeArcHeight = Math.max(0.0, arcHeight);
        // 初速も両側で一致させる。1tick目だけでも差があると予測ズレが蓄積しやすい.
        var firstStep = calculateEasedPosition(start, destination, safeArcHeight, 1.0 / durationTicks).subtract(start);
        entity.setDeltaMovement(firstStep);
        entity.hasImpulse = true;
        entity.hurtMarked = true;
        entity.fallDistance = 0;

        spellData.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, state -> {
            // 実際の移動は MantisLeapMovementEvent で毎tick計算する.
            // ここには再現に必要な最小パラメータだけを保存する.
            state.totalTicks = durationTicks;
            state.elapsedTicks = 0;
            state.startX = start.x;
            state.startY = start.y;
            state.startZ = start.z;
            state.targetX = destination.x;
            state.targetY = destination.y;
            state.targetZ = destination.z;
            state.arcHeight = safeArcHeight;
            state.bladeEntityId = bladeEntityId;
            state.lastDistanceToTargetSq = -1.0;
            state.stagnantTicks = 0;
            state.noGravityApplied = false;
        });
        return true;
    }

    private Vec3 calculateEasedPosition(Vec3 start, Vec3 target, double arcHeight, double progress) {
        // 進行度は必ず 0..1 に正規化して両側の数値暴れを防ぐ.
        var clamped = Math.max(0.0, Math.min(1.0, progress));
        // easeOutCubic で終端を減速させ、見た目の「着地時にスッと止まる」感覚を作る.
        // この式も予測一致のため、サーバー/クライアントで同一実装を維持する.
        var eased = easeOutCubic(clamped);
        var linear = start.lerp(target, eased);
        var arc = 4.0 * arcHeight * clamped * (1.0 - clamped);
        return linear.add(0.0, arc, 0.0);
    }

    private double easeOutCubic(double value) {
        var inverse = 1.0 - value;
        return 1.0 - inverse * inverse * inverse;
    }
}
