package jp.aquafactory.apprenticecodex.spell.magicspear;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import jp.aquafactory.apprenticecodex.utility.SummonedFirearmTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class MagicSpear extends AbstractSpell {
    private static final double LOCK_ON_RANGE = 64.0;
    private static final String NEXT_LEFT_TAG = "apprenticecodex.magic_spear.next_left";

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "magic_spear");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(0.5)
            .build();

    public MagicSpear() {
        baseSpellPower = 900;
        spellPowerPerLevel = 200;
        baseManaCost = 100;
        manaCostPerLevel = 30;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.MAGIC_SPEAR);
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
        // todo:音を作る.
        return Optional.of(SoundEvents.SHULKER_SHOOT);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            spawnMissile(serverLevel, spellLevel, entity);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnMissile(ServerLevel level, int spellLevel, LivingEntity caster) {
        var forward = resolveLaunchForward(caster);
        var right = resolveLaunchRight(caster, forward);
        var side = nextSide(caster);
        var sideDirection = right.scale(side);
        var spawnPosition = caster.getEyePosition()
                .add(forward.scale(0.4))
                .add(sideDirection.scale(0.75))
                .add(0.0, -0.25, 0.0);
        var target = findLockOnTarget(caster).orElse(null);

        var missile = new MagicSpearMissileEntity(EntityRegistry.MAGIC_SPEAR_MISSILE.get(), level, caster);
        missile.setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        missile.setup(getDamage(spellLevel, caster), forward, sideDirection, target);
        level.addFreshEntity(missile);
    }

    private static Vec3 resolveLaunchForward(LivingEntity caster) {
        if (isProneLaunch(caster)) {
            var look = caster.getViewVector(1.0F);
            if (look.lengthSqr() > 1.0e-6) {
                return look.normalize();
            }
        }
        return RotationTools.getFlatForward(caster);
    }

    private static Vec3 resolveLaunchRight(LivingEntity caster, Vec3 forward) {
        var worldUp = new Vec3(0.0, 1.0, 0.0);
        var right = worldUp.cross(forward);
        if (right.lengthSqr() > 1.0e-6) {
            return right.normalize();
        }

        var flatForward = RotationTools.getFlatForward(caster);
        return new Vec3(flatForward.z, 0.0, -flatForward.x).normalize();
    }

    private static boolean isProneLaunch(LivingEntity caster) {
        return caster.isFallFlying()
                || caster.isSwimming()
                || caster.getPose() == Pose.SWIMMING;
    }

    private static Optional<Entity> findLockOnTarget(LivingEntity caster) {
        var result = SummonedFirearmTools.resolveAssistedAim(caster, LOCK_ON_RANGE,
                e -> CombatTools.isValidCombatTarget(e, caster));
        if (result.hitEntity() == null) {
            return Optional.empty();
        }

        var target = CombatTools.resolutePartEntity(result.hitEntity());
        return CombatTools.isValidCombatTarget(target, caster) ? Optional.of(target) : Optional.empty();
    }

    private static int nextSide(LivingEntity caster) {
        var tag = caster.getPersistentData();
        var left = tag.getBoolean(NEXT_LEFT_TAG);
        tag.putBoolean(NEXT_LEFT_TAG, !left);
        return left ? -1 : 1;
    }
}
