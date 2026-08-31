package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import jp.aquafactory.apprenticecodex.effect.ThermalSundered;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.thermalslice.ThermalSlice;
import jp.aquafactory.apprenticecodex.spell.thermalslice.ThermalSliceKatanaEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

final class ThermalSunderedGameTestScenarios {
    private static final double ATTRIBUTE_EPSILON = 1.0E-6D;
    private static final float HEALTH_EPSILON = 1.0E-4F;

    private ThermalSunderedGameTestScenarios() {
    }

    static void thermalSliceAppliesThermalSunderedAfterDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "thermal_sundered_owner", new Vec3(3.5D, 2.0D, 2.5D));
        owner.setYRot(0.0F);
        var weapon = createWeapon(level, owner, 4.0F, 99);
        var target = createMob(level, EntityType.ZOMBIE, weapon.position().add(0.0D, 0.0D, 2.0D));
        target.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
        var initialHealth = target.getHealth();

        weapon.slash(level);

        var effect = getThermalSundered(target);
        helper.assertTrue(target.getHealth() < initialHealth - HEALTH_EPSILON,
                "Thermal Slice should damage a valid target before applying Thermal Sundered");
        helper.assertTrue(effect != null, "Thermal Slice should apply Thermal Sundered after damage succeeds");
        helper.assertTrue(effect.getDuration() == ThermalSundered.INITIAL_DURATION_TICKS,
                "Thermal Sundered should start with a three-second duration");
        helper.assertTrue(effect.getAmplifier() == ThermalSundered.MAX_AMPLIFIER,
                "Thermal Slice should clamp Thermal Sundered to amplifier four");
        helper.assertTrue(effect.isVisible() && effect.showIcon(),
                "Thermal Sundered should display particles and its status icon");
        helper.assertTrue(!(EffectRegistry.THERMAL_SUNDERED.get() instanceof MagicMobEffect),
                "Thermal Sundered should remain a milk-removable non-magic mob effect");
        helper.assertTrue(Math.abs(target.getAttributeValue(AttributeRegistry.FIRE_MAGIC_RESIST) - 0.5D)
                        <= ATTRIBUTE_EPSILON,
                "Amplifier four should reduce Fire Magic Resist by a fixed 0.5");
        helper.assertTrue(target.getRemainingFireTicks() <= 0,
                "Thermal Slice should not ignite its target");

        var spell = (ThermalSlice) SpellRegistry.THERMAL_SLICE.get();
        helper.assertTrue(spell.getThermalSunderedAmplifierForGameTest(1) == 0,
                "Thermal Slice level one should use amplifier zero");
        helper.assertTrue(spell.getThermalSunderedAmplifierForGameTest(5) == 4
                        && spell.getThermalSunderedAmplifierForGameTest(100) == 4,
                "Thermal Slice should cap configured and over-level casts at amplifier four");

        var saved = new CompoundTag();
        weapon.saveWithoutId(saved);
        var loadedWeapon = new ThermalSliceKatanaEntity(EntityRegistry.THERMAL_SLICE_KATANA.get(), level);
        loadedWeapon.load(saved);
        helper.assertTrue(loadedWeapon.getThermalSunderedAmplifierForGameTest() == ThermalSundered.MAX_AMPLIFIER,
                "Thermal Slice should preserve its clamped amplifier in NBT");

        discard(owner, weapon, loadedWeapon, target);
        helper.succeed();
    }

    static void thermalSliceSkipsProtectedAndRejectedTargets(GameTestHelper helper) {
        var level = helper.getLevel();

        var fireResistanceTarget = runSlashScenario(
                helper, level, "thermal_sundered_fire_resistance", new Vec3(2.5D, 2.0D, 2.5D), EntityType.ZOMBIE,
                target -> target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200))
        );
        helper.assertTrue(fireResistanceTarget.damaged(),
                "Fire Resistance should not block Thermal Slice damage");
        helper.assertTrue(getThermalSundered(fireResistanceTarget.target()) == null,
                "Fire Resistance should prevent Thermal Sundered");

        var fireImmuneTarget = runSlashScenario(
                helper, level, "thermal_sundered_fire_immune", new Vec3(7.5D, 2.0D, 2.5D), EntityType.BLAZE,
                target -> {
                }
        );
        helper.assertTrue(fireImmuneTarget.target().fireImmune(),
                "The fire-immune scenario should use an innately fire-immune target");
        helper.assertTrue(fireImmuneTarget.damaged(),
                "Innate fire immunity should not block Thermal Slice damage");
        helper.assertTrue(getThermalSundered(fireImmuneTarget.target()) == null,
                "Innate fire immunity should prevent Thermal Sundered");

        var rejectedTarget = runSlashScenario(
                helper, level, "thermal_sundered_rejected", new Vec3(12.5D, 2.0D, 2.5D), EntityType.ZOMBIE,
                target -> target.setInvulnerable(true)
        );
        helper.assertFalse(rejectedTarget.damaged(),
                "An invulnerable target should reject Thermal Slice damage");
        helper.assertTrue(getThermalSundered(rejectedTarget.target()) == null,
                "Rejected damage should not apply Thermal Sundered");

        discard(fireResistanceTarget.entities());
        discard(fireImmuneTarget.entities());
        discard(rejectedTarget.entities());
        helper.succeed();
    }

    static void thermalSunderedRefreshesWithoutDowngrading(GameTestHelper helper) {
        var level = helper.getLevel();
        var target = createMob(level, EntityType.ZOMBIE, helper.absoluteVec(new Vec3(4.5D, 2.0D, 4.5D)));
        var effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.THERMAL_SUNDERED.get());
        target.addEffect(new MobEffectInstance(effectHolder, 20, 4, false, true, true));

        jp.aquafactory.apprenticecodex.spell.thermalslice.ThermalSunderedLogic
                .applyFromThermalSlice(target, 0);
        var refreshed = getThermalSundered(target);
        helper.assertTrue(refreshed != null
                        && refreshed.getDuration() == ThermalSundered.INITIAL_DURATION_TICKS
                        && refreshed.getAmplifier() == 4,
                "A lower-amplifier re-hit should refresh three seconds without downgrading");

        target.removeEffect(effectHolder);
        target.addEffect(new MobEffectInstance(effectHolder, 20, 3, false, true, true));
        target.invulnerableTime = 0;
        helper.assertTrue(target.hurt(level.damageSources().generic(), 1.0F),
                "Generic setup damage should be accepted");
        helper.assertTrue(getThermalSundered(target).getDuration() == 20,
                "Non-on-fire damage should not extend Thermal Sundered");

        target.invulnerableTime = 0;
        helper.assertTrue(target.hurt(level.damageSources().onFire(), 1.0F),
                "On-fire setup damage should be accepted");
        var extended = getThermalSundered(target);
        helper.assertTrue(extended.getDuration() == ThermalSundered.ON_FIRE_EXTENDED_DURATION_TICKS
                        && extended.getAmplifier() == 3,
                "On-fire damage should extend Thermal Sundered to five seconds without changing its amplifier");

        target.removeEffect(effectHolder);
        target.addEffect(new MobEffectInstance(effectHolder, 120, 3, false, true, true));
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().onFire(), 1.0F);
        helper.assertTrue(getThermalSundered(target).getDuration() == 120,
                "On-fire damage should not shorten an already longer Thermal Sundered effect");

        discard(target);
        helper.succeed();
    }

    private static <T extends Mob> SlashScenario<T> runSlashScenario(
            GameTestHelper helper,
            ServerLevel level,
            String ownerName,
            Vec3 ownerPosition,
            EntityType<T> targetType,
            java.util.function.Consumer<T> setup
    ) {
        var owner = createPlayer(helper, ownerName, ownerPosition);
        owner.setYRot(0.0F);
        var weapon = createWeapon(level, owner, 4.0F, 2);
        var target = createMob(level, targetType, weapon.position().add(0.0D, 0.0D, 2.0D));
        target.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
        setup.accept(target);
        var initialHealth = target.getHealth();
        weapon.slash(level);
        return new SlashScenario<>(owner, weapon, target, target.getHealth() < initialHealth - HEALTH_EPSILON);
    }

    private static ThermalSliceKatanaEntity createWeapon(
            ServerLevel level, LivingEntity owner, float damage, int amplifier
    ) {
        var weapon = new ThermalSliceKatanaEntity(EntityRegistry.THERMAL_SLICE_KATANA.get(), level, owner);
        weapon.setDamage(damage);
        weapon.setThermalSunderedAmplifier(amplifier);
        level.addFreshEntity(weapon);
        return weapon;
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name, Vec3 localPosition) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        var position = helper.absoluteVec(localPosition);
        player.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static <T extends Mob> T createMob(ServerLevel level, EntityType<T> type, Vec3 position) {
        var mob = type.create(level);
        if (mob == null) {
            throw new IllegalStateException("Failed to create Thermal Sundered test mob");
        }
        mob.setPos(position.x, position.y, position.z);
        mob.setNoAi(true);
        level.addFreshEntity(mob);
        return mob;
    }

    private static MobEffectInstance getThermalSundered(LivingEntity target) {
        var holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.THERMAL_SUNDERED.get());
        return target.getEffect(holder);
    }

    private static void discard(Entity... entities) {
        for (var entity : entities) {
            entity.discard();
        }
    }

    private record SlashScenario<T extends Mob>(FakePlayer owner, ThermalSliceKatanaEntity weapon, T target,
                                                 boolean damaged) {
        private Entity[] entities() {
            return new Entity[]{owner, weapon, target};
        }
    }
}
