package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.effect.ThermalProcessing;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.thermalprocess.ThermalProcessThrowerEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

final class FireSpellResistanceGameTestScenarios {
    private static final float HEALTH_EPSILON = 1.0E-4F;

    private FireSpellResistanceGameTestScenarios() {
    }

    static void thermalProcessAndCombustionJetDamageFireResistantTargets(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "fire_spell_resistance_owner", new Vec3(1.5D, 2.0D, 1.5D));
        var thermalProcessSource = CombatTools.getDamageSource(level, owner, DamageTypes.THERMAL_PROCESS);
        var combustionJetSource = CombatTools.getDamageSource(level, owner, DamageTypes.COMBUSTION_JET);
        var catchFlameSource = CombatTools.getDamageSource(level, owner, DamageTypes.CATCH_FLAME);
        var catchFlamePenetrateSource = CombatTools.getDamageSource(level, owner, DamageTypes.CATCH_FLAME_PENETRATE);

        helper.assertFalse(thermalProcessSource.is(DamageTypeTags.IS_FIRE),
                "Thermal Process damage should not use the vanilla is_fire tag");
        helper.assertFalse(combustionJetSource.is(DamageTypeTags.IS_FIRE),
                "Combustion Jet damage should not use the vanilla is_fire tag");
        helper.assertTrue(catchFlameSource.is(DamageTypeTags.IS_FIRE)
                        && catchFlamePenetrateSource.is(DamageTypeTags.IS_FIRE),
                "Both Catch Flame damage types should keep the vanilla is_fire tag");

        var thermalPotionTarget = createTarget(helper, EntityType.ZOMBIE, new Vec3(3.5D, 2.0D, 2.5D));
        thermalPotionTarget.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
        var thermalImmuneTarget = createTarget(helper, EntityType.BLAZE, new Vec3(5.5D, 2.0D, 2.5D));
        var combustionPotionTarget = createTarget(helper, EntityType.ZOMBIE, new Vec3(7.5D, 2.0D, 2.5D));
        combustionPotionTarget.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
        var combustionImmuneTarget = createTarget(helper, EntityType.BLAZE, new Vec3(9.5D, 2.0D, 2.5D));

        assertDamageApplied(helper, thermalPotionTarget, thermalProcessSource,
                SpellRegistry.THERMAL_PROCESS.get(), "Thermal Process should damage a Fire Resistance target");
        assertDamageApplied(helper, thermalImmuneTarget, thermalProcessSource,
                SpellRegistry.THERMAL_PROCESS.get(), "Thermal Process should damage a fire-immune target");
        assertDamageApplied(helper, combustionPotionTarget, combustionJetSource,
                SpellRegistry.COMBUSTION_JET.get(), "Combustion Jet should damage a Fire Resistance target");
        assertDamageApplied(helper, combustionImmuneTarget, combustionJetSource,
                SpellRegistry.COMBUSTION_JET.get(), "Combustion Jet should damage a fire-immune target");

        discard(owner, thermalPotionTarget, thermalImmuneTarget, combustionPotionTarget, combustionImmuneTarget);
        helper.succeed();
    }

    static void thermalProcessSkipsEffectsForFireResistantTargets(GameTestHelper helper) {
        var level = helper.getLevel();
        var normalTarget = createTarget(helper, EntityType.ZOMBIE, new Vec3(3.5D, 2.0D, 2.5D));
        var potionTarget = createTarget(helper, EntityType.ZOMBIE, new Vec3(5.5D, 2.0D, 2.5D));
        potionTarget.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
        var immuneTarget = createTarget(helper, EntityType.BLAZE, new Vec3(7.5D, 2.0D, 2.5D));
        var thrower = new ThermalProcessThrowerEntity(EntityRegistry.THERMAL_PROCESS_THROWER.get(), level);

        for (var i = 0; i <= ThermalProcessing.MAX_AMPLIFIER; ++i) {
            thrower.applyOrUpdateThermalProcessingForGameTest(normalTarget);
            thrower.applyOrUpdateThermalProcessingForGameTest(potionTarget);
            thrower.applyOrUpdateThermalProcessingForGameTest(immuneTarget);
        }

        var effect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.THERMAL_PROCESSING.get());
        var normalEffect = normalTarget.getEffect(effect);
        helper.assertTrue(normalEffect != null && normalEffect.getAmplifier() == ThermalProcessing.MAX_AMPLIFIER,
                "Thermal Process should build Thermal Processing on a normal target");
        helper.assertTrue(normalTarget.getRemainingFireTicks() > 0,
                "Maximum Thermal Processing should ignite a normal target");
        helper.assertTrue(potionTarget.getEffect(effect) == null && potionTarget.getRemainingFireTicks() <= 0,
                "Fire Resistance should block Thermal Processing and its ignition");
        helper.assertTrue(immuneTarget.getEffect(effect) == null && immuneTarget.getRemainingFireTicks() <= 0,
                "Innate fire immunity should block Thermal Processing and its ignition");

        discard(thrower, normalTarget, potionTarget, immuneTarget);
        helper.succeed();
    }

    private static void assertDamageApplied(
            GameTestHelper helper,
            Mob target,
            net.minecraft.world.damagesource.DamageSource source,
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell,
            String message
    ) {
        var initialHealth = target.getHealth();
        var applied = CombatTools.applyDamage(
                target, 2.0F, source, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
        helper.assertTrue(applied && target.getHealth() < initialHealth - HEALTH_EPSILON, message);
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name, Vec3 localPosition) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        var position = helper.absoluteVec(localPosition);
        player.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static <T extends Mob> T createTarget(GameTestHelper helper, EntityType<T> type, Vec3 localPosition) {
        var target = type.create(helper.getLevel());
        if (target == null) {
            throw new IllegalStateException("Failed to create fire spell resistance test target");
        }
        var position = helper.absoluteVec(localPosition);
        target.setPos(position.x, position.y, position.z);
        target.setNoAi(true);
        var armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(0.0D);
        }
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static void discard(Entity... entities) {
        for (var entity : entities) {
            entity.discard();
        }
    }
}
