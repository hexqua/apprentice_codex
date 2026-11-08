package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.casting;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.SpellScrollTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateDispenserTestSupport.*;

final class SpellDispenserContinuousCastScenarios {
    private SpellDispenserContinuousCastScenarios() {}

    static void spellDispenserCastHelperStopsContinuousCastAtDurationCap(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
            );
            var scrollStack = createSpellScroll(spell);
            var validation = createSpellDispenserValidation(scrollStack, spell);

            var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    castPos,
                    Direction.NORTH,
                    validation,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_continuous_test")
            );
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser cast helper failed to start a CONTINUOUS Blaze Storm scroll");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser cast helper did not return a CONTINUOUS session");

            var session = startResult.session();
            var maxTicks = session.magicData().getCastDuration();
            for (var tick = 0; tick <= maxTicks + 1 && !session.isFinished(); tick++) {
                SpellDispenserCastHelper.tickContinuousCast(level, session);
            }

            helper.assertTrue(session.isFinished(), "Spell Dispenser CONTINUOUS cast did not stop by its cast duration cap");
            helper.assertTrue(session.consumeFinishedCooldownTicks() == spell.getSpellCooldown(),
                    "Spell Dispenser CONTINUOUS cast did not record the expected cooldown after completion");
            var fireballBox = new AABB(castPos).inflate(16.0D);
            var fireballs = level.getEntitiesOfClass(SmallMagicFireball.class, fireballBox);
            helper.assertTrue(!fireballs.isEmpty(), "Spell Dispenser CONTINUOUS cast completed without spawning Blaze Storm fireballs");
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser proxy caster was left behind after CONTINUOUS cast");
        });
    }

    static void spellDispenserContinuousBreathBindsProjectileToTrackedAnchor(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
            var scrollStack = createSpellScroll(spell);
            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected Fire Breath profile");

            var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    castPos,
                    Direction.NORTH,
                    validation,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_fire_breath_test")
            );
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser cast helper failed to start Fire Breath");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser cast helper did not return a Fire Breath session");
            helper.assertTrue(startResult.session().spellCaster() instanceof SpellDispenserAnchorEntity,
                    "Spell Dispenser Fire Breath did not select a tracked living caster from its explicit profile: " + startResult.session().spellCaster());

            var session = startResult.session();
            for (var tick = 0; tick < 20; tick++) {
                SpellDispenserCastHelper.tickContinuousCast(level, session);
            }

            var coneProjectiles = level.getEntitiesOfClass(FireBreathProjectile.class, new AABB(castPos).inflate(16.0D));
            helper.assertTrue(!coneProjectiles.isEmpty(), "Spell Dispenser Fire Breath never spawned its cone projectile");
            helper.assertTrue(coneProjectiles.get(0).getOwner() instanceof SpellDispenserAnchorEntity,
                    "Spell Dispenser Fire Breath did not rebind its cone owner to a tracked anchor: " + coneProjectiles.get(0).getOwner());

            SpellDispenserCastHelper.finishContinuousCast(level, session, true);
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser tracked anchor was left behind after Fire Breath cleanup");
        });
    }
}
