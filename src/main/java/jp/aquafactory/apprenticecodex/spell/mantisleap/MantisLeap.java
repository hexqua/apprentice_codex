package jp.aquafactory.apprenticecodex.spell.mantisleap;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MantisLeap extends AbstractSpell {
    private static final double DEFAULT_LEAP_TICKS_PER_BLOCK = 1.0;
    private static final double DEFAULT_LEAP_ARC_HEIGHT = 1.0;
    private static final double TARGET_STOP_DISTANCE = 1.0;
    private static final double MIN_LEAP_DISTANCE = 0.25;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mantis_leap");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(4)
            .build();

    public MantisLeap() {
        baseSpellPower = 600;
        spellPowerPerLevel = 250;
        baseManaCost = 50;
        manaCostPerLevel = 15;
        castTime = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity) / 100.0f;
    }

    private double getRange(int spellLevel, LivingEntity entity) {
        return Math.min(20, 4 + getSpellPower(spellLevel, entity) / 150.0);
    }

    private double getLeapTicksPerBlock(int spellLevel, LivingEntity entity) {
        return DEFAULT_LEAP_TICKS_PER_BLOCK;
    }

    private double getLeapArcHeight(int spellLevel, LivingEntity entity) {
        return DEFAULT_LEAP_ARC_HEIGHT;
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
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        // 詠唱時間短縮は乗らない.
        return getCastTime(spellLevel);
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
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (!cancelled && !level.isClientSide) {
            var destination = resolveLeapDestination(spellLevel, entity);
            startLeap(
                    entity,
                    destination,
                    getLeapTicksPerBlock(spellLevel, entity),
                    getLeapArcHeight(spellLevel, entity)
            );
        }

        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private Vec3 resolveLeapDestination(int spellLevel, LivingEntity caster) {
        var range = getRange(spellLevel, caster);
        var start = caster.position();
        var look = caster.getLookAngle().normalize();
        var result = RaycastTools.raycastFromEye(caster, range, 0.5, e -> CombatTools.isValidCombatTarget(e, caster));

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

    private void startLeap(LivingEntity entity, Vec3 destination, double ticksPerBlock, double arcHeight) {
        var start = entity.position();
        var offset = destination.subtract(start);
        if (offset.lengthSqr() < MIN_LEAP_DISTANCE * MIN_LEAP_DISTANCE) {
            return;
        }

        var distance = offset.length();
        var durationTicks = Math.max(1, (int) Math.ceil(distance * Math.max(0.0, ticksPerBlock)));
        var firstStep = calculateEasedPosition(start, destination, Math.max(0.0, arcHeight), 1.0 / durationTicks).subtract(start);
        entity.setDeltaMovement(firstStep);
        entity.hasImpulse = true;
        entity.hurtMarked = true;
        entity.fallDistance = 0;

        Capabilities.withSpellData(entity, data -> data.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, state -> {
            state.totalTicks = durationTicks;
            state.elapsedTicks = 0;
            state.startX = start.x;
            state.startY = start.y;
            state.startZ = start.z;
            state.targetX = destination.x;
            state.targetY = destination.y;
            state.targetZ = destination.z;
            state.arcHeight = Math.max(0.0, arcHeight);
            state.noGravityApplied = false;
        }));
    }

    private Vec3 calculateEasedPosition(Vec3 start, Vec3 target, double arcHeight, double progress) {
        var clamped = Math.max(0.0, Math.min(1.0, progress));
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
