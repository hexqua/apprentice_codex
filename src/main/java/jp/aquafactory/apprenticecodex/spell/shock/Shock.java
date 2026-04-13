package jp.aquafactory.apprenticecodex.spell.shock;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Shock extends AbstractSpell {
    private static final double THIN_RAYCAST_WIDTH = 1.0;
    private static final double WIDE_RAYCAST_WIDTH = 4.0;
    private static final double EMPTY_SHOT_RANGE = 16.0;
    private static final int MIN_BOLT_LIFE_TICKS = 5;
    private static final int BOLT_LIFE_VARIATION = 4;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "shock");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(1.5)
            .build();

    public Shock() {
        baseSpellPower = 500;
        spellPowerPerLevel = 50;
        baseManaCost = 15;
        manaCostPerLevel = 2;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel), 0))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.SHOCK);
    }

    private double getRange(int spellLevel) {
        return 24 + (spellLevel - 1) * 2;
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
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.LIGHTNING_CAST.get());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            var castTarget = resolveCastTarget(level, spellLevel, entity);

            if (castTarget.target() != null) {
                var source = CombatTools.getDamageSource(level, entity, DamageTypes.SHOCK);
                CombatTools.applyDamage(
                        castTarget.target(),
                        getDamage(spellLevel, entity),
                        source,
                        getSchoolType(),
                        CombatTools.KnockbackTypes.DEFAULT
                );
            }

            spawnShockBolt(level, entity, castTarget);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private TargetingResult resolveCastTarget(Level level, int spellLevel, LivingEntity caster) {
        var range = getRange(spellLevel);

        var thinTarget = resolveRaycastTarget(caster, range, THIN_RAYCAST_WIDTH);
        if (thinTarget != null) {
            return thinTarget;
        }

        var wideTarget = resolveRaycastTarget(caster, range, WIDE_RAYCAST_WIDTH);
        if (wideTarget != null) {
            return wideTarget;
        }

        return new TargetingResult(null, resolveEmptyShotPosition(level, caster), true);
    }

    @Nullable
    private TargetingResult resolveRaycastTarget(LivingEntity caster, double range, double width) {
        var result = RaycastTools.raycastFromEye(
                caster,
                range,
                width,
                target -> CombatTools.isValidCombatTarget(target, caster)
        );
        if (result.hitEntity() == null) {
            return null;
        }

        var target = CombatTools.resolutePartEntity(result.hitEntity());
        return new TargetingResult(target, result.hitPosition(), false);
    }

    private Vec3 resolveEmptyShotPosition(Level level, LivingEntity caster) {
        var start = caster.getEyePosition(1.0F);
        var end = start.add(caster.getLookAngle().normalize().scale(EMPTY_SHOT_RANGE));
        var blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        return blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
    }

    private void spawnShockBolt(Level level, LivingEntity caster, TargetingResult castTarget) {
        var start = calculateBoltStart(caster);
        var end = castTarget.impactPosition();
        var yawPitch = RotationTools.calculateYawPitchByDirection(end.subtract(start));
        var bolt = new ShockBoltEntity(EntityRegistry.SHOCK_BOLT.get(), level);

        bolt.moveTo(start.x, start.y, start.z, yawPitch.yaw(), yawPitch.pitch());
        bolt.setup(
                end,
                MIN_BOLT_LIFE_TICKS + level.getRandom().nextInt(BOLT_LIFE_VARIATION),
                level.getRandom().nextInt(),
                !castTarget.emptyShot()
        );
        level.addFreshEntity(bolt);
    }

    private static Vec3 calculateBoltStart(LivingEntity caster) {
        // 視界を隠しすぎない範囲で手元寄りに見せる。
        return caster.getEyePosition(1.0F)
                .add(0.0, -0.22, 0.0)
                .add(caster.getLookAngle().scale(0.55));
    }

    private record TargetingResult(@Nullable Entity target, Vec3 impactPosition, boolean emptyShot) {
    }
}
