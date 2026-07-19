package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightChargeCutEntity;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightCounterspellEffect;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightKatanaEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.function.Consumer;

final class MoonLightCounterspellGameTestScenarios {
    private MoonLightCounterspellGameTestScenarios() {
    }

    static void successfulDamageAppliesCounterspellEffects(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var caster = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            caster.setNoAi(true);
            var target = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                    helper, new BlockPos(0, 2, 1), "moon_light_counterspell_target"
            );
            var magicData = preparePlayerCounterspellState(target);

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 200));

            var milkProofNormalEffect = new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200);
            milkProofNormalEffect.setCurativeItems(List.of());
            target.addEffect(milkProofNormalEffect);

            var milkProofMagicEffect = new MobEffectInstance(EffectRegistry.ECHO_SPELL.get(), 200);
            milkProofMagicEffect.setCurativeItems(List.of());
            target.addEffect(milkProofMagicEffect);

            var source = CombatTools.getDamageSource(helper.getLevel(), caster, DamageTypes.MOON_LIGHT);
            MoonLightCounterspellEffect.applyAfterSuccessfulDamage(source, target, caster);

            helper.assertFalse(magicData.isCasting(),
                    "MoonLight should interrupt a spell whose canBeInterrupted is false");
            helper.assertFalse(hasCounterspellTestRecast(magicData),
                    "MoonLight should remove player recasts with the Counterspell result");
            helper.assertFalse(target.hasEffect(MobEffects.MOVEMENT_SPEED),
                    "MoonLight should cure milk-curable beneficial effects");
            helper.assertFalse(target.hasEffect(MobEffects.POISON),
                    "MoonLight should also cure milk-curable harmful effects");
            helper.assertTrue(target.hasEffect(MobEffects.DAMAGE_BOOST),
                    "MoonLight should preserve milk-proof non-magic effects");
            helper.assertFalse(target.hasEffect(EffectRegistry.ECHO_SPELL.get()),
                    "MoonLight should remove milk-proof MagicMobEffect instances");
        });
    }

    static void failedDamageDoesNotApplyCounterspellEffects(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var caster = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            caster.setNoAi(true);
            var target = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                    helper, new BlockPos(0, 2, 1), "moon_light_failed_damage_target"
            );
            var magicData = preparePlayerCounterspellState(target);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
            target.setInvulnerable(true);
            var healthBefore = target.getHealth();

            slashTarget(helper, caster, 1.0f);

            helper.assertTrue(Math.abs(target.getHealth() - healthBefore) < 0.001f,
                    "Invulnerable MoonLight target should reject damage");
            helper.assertTrue(magicData.isCasting(),
                    "Rejected MoonLight damage should not interrupt casting");
            helper.assertTrue(hasCounterspellTestRecast(magicData),
                    "Rejected MoonLight damage should not remove recasts");
            helper.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SPEED),
                    "Rejected MoonLight damage should not cure mob effects");
        });
    }

    static void canceledCounterspellEventKeepsAdditionalEffects(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var caster = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            caster.setNoAi(true);
            var target = helper.spawn(EntityType.SHEEP, new BlockPos(0, 2, 1));
            target.setNoAi(true);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
            var healthBefore = target.getHealth();

            Consumer<CounterSpellEvent> cancelListener = event -> {
                if (event.target == target) {
                    event.setCanceled(true);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(cancelListener);
            try {
                slashTarget(helper, caster, 1.0f);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(cancelListener);
            }

            helper.assertTrue(target.getHealth() < healthBefore,
                    "Canceled CounterSpellEvent should not roll back successful MoonLight damage");
            helper.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SPEED),
                    "Canceled CounterSpellEvent should skip milk curing");
        });
    }

    static void successfulDamageCancelsMagicEntityCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var caster = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                    helper, new BlockPos(0, 2, 0), "moon_light_magic_mob_caster"
            );
            var target = io.redspace.ironsspellbooks.registries.EntityRegistry.CRYOMANCER.get().create(level);
            helper.assertTrue(target != null, "MoonLight test should create a Cryomancer");
            var targetPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 2, 1)));
            target.setPos(targetPos);
            target.setNoAi(true);
            level.addFreshEntity(target);
            target.initiateCastSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get(), 1);
            helper.assertTrue(target.isCasting(), "Cryomancer should be casting before MoonLight damage");
            var healthBefore = target.getHealth();

            slashTarget(helper, caster, 1.0f);

            helper.assertTrue(target.getHealth() < healthBefore,
                    "MoonLight should damage the casting magic entity");
            helper.assertFalse(target.isCasting(),
                    "MoonLight should call IMagicEntity.cancelCast after successful damage");
        });
    }

    static void chargeCutAppliesCounterspellEffects(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        caster.setNoAi(true);
        caster.setYRot(0.0f);
        caster.setXRot(0.0f);
        var target = helper.spawn(EntityType.SHEEP, new BlockPos(0, 2, 4));
        target.setNoAi(true);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
        var healthBefore = target.getHealth();

        var cut = new MoonLightChargeCutEntity(EntityRegistry.MOON_LIGHT_CHARGE_CUT.get(), level, caster);
        var direction = caster.getLookAngle().normalize();
        var cutPos = caster.position().add(direction.scale(
                MoonLightChargeCutEntity.START_OFFSET_BLOCKS + MoonLightChargeCutEntity.SURFACE_OFFSET_BLOCKS
        ));
        cut.setPos(cutPos);
        cut.setYRot(caster.getYRot());
        cut.setXRot(caster.getXRot());
        cut.setup(8.0f, 1.0f);
        level.addFreshEntity(cut);

        helper.runAfterDelay(MoonLightChargeCutEntity.PROCESS_START_DELAY_TICKS
                + MoonLightChargeCutEntity.PROCESS_DURATION_TICKS + 2, () -> {
            helper.assertTrue(target.getHealth() < healthBefore,
                    "MoonLight charge cut should damage its target");
            helper.assertFalse(target.hasEffect(MobEffects.MOVEMENT_SPEED),
                    "MoonLight charge cut should apply the shared Counterspell effects");
            helper.succeed();
        });
    }

    private static MagicData preparePlayerCounterspellState(net.minecraftforge.common.util.FakePlayer target) {
        var magicData = MagicData.getPlayerMagicData(target);
        var interruptedSpell = SpellRegistry.MOON_LIGHT.get();
        magicData.initiateCast(interruptedSpell, 1, 60, CastSource.SPELLBOOK, "gametest");

        var recastSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.COUNTERSPELL_SPELL.get();
        magicData.getPlayerRecasts().addRecast(new RecastInstance(
                recastSpell.getSpellId(),
                1,
                2,
                200,
                CastSource.SPELLBOOK,
                null
        ), magicData);
        return magicData;
    }

    private static boolean hasCounterspellTestRecast(MagicData magicData) {
        return magicData.getPlayerRecasts().hasRecastForSpell(
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.COUNTERSPELL_SPELL.get()
        );
    }

    private static void slashTarget(
            GameTestHelper helper,
            LivingEntity caster,
            float damage
    ) {
        var weapon = new MoonLightKatanaEntity(EntityRegistry.MOON_LIGHT_KATANA.get(), helper.getLevel(), caster);
        weapon.setDamage(damage);
        helper.getLevel().addFreshEntity(weapon);
        weapon.slash(helper.getLevel());
    }
}
