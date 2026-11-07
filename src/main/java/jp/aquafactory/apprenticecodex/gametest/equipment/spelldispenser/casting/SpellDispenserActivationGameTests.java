package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.casting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserActivationGameTests {
    private SpellDispenserActivationGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserBlockEntityStopsContinuousCastWhenSignalTurnsOff(GameTestHelper helper) {
        var scope = new DispenserTestScope(helper);
        var level = (ServerLevel) helper.getLevel();
        var pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
        helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
        var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
        );
        var scrollStack = createSpellScroll(spell);
        spellDispenser.getInventory().setStackInSlot(0, scrollStack);
        spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_signal_stop_test"));

        helper.runAtTickTime(1, () -> {
            var startResult = scope.start(() -> SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    helper.absolutePos(pos),
                    Direction.NORTH,
                    createSpellDispenserValidation(scrollStack, spell),
                    scrollStack,
                    spellDispenser.getOwnerProfile()
            ));
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser block entity failed to prepare a CONTINUOUS session");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser block entity did not receive a powered CONTINUOUS session");
            spellDispenser.startContinuousCast(startResult.session());
        });
        helper.runAtTickTime(2, () -> {
            scope.run(() -> SpellDispenserBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), spellDispenser));
            helper.assertTrue(spellDispenser.hasActiveContinuousCast(), "Spell Dispenser CONTINUOUS cast stopped immediately while powered");
            helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, false));
        });
        helper.runAtTickTime(3, () -> {
            scope.run(() -> SpellDispenserBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), spellDispenser));
            helper.assertFalse(spellDispenser.hasActiveContinuousCast(), "Spell Dispenser CONTINUOUS cast did not stop after redstone signal loss");
            scope.assertReleased(helper, "Spell Dispenser proxy caster was left behind after signal loss");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserBlockEntityDoesNotRestartContinuousCastWhileSignalStaysOn(GameTestHelper helper) {
        var scope = new DispenserTestScope(helper);
        var level = (ServerLevel) helper.getLevel();
        var pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
        helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
        var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
        );
        var scrollStack = createSpellScroll(spell);
        spellDispenser.getInventory().setStackInSlot(0, scrollStack);
        spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_signal_hold_test"));

        helper.runAtTickTime(1, () -> {
            var startResult = scope.start(() -> SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    helper.absolutePos(pos),
                    Direction.NORTH,
                    createSpellDispenserValidation(scrollStack, spell),
                    scrollStack,
                    spellDispenser.getOwnerProfile()
            ));
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser block entity failed to prepare a held-signal CONTINUOUS session");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser block entity did not receive a held-signal CONTINUOUS session");
            spellDispenser.startContinuousCast(startResult.session());

            var maxTicks = startResult.session().magicData().getCastDuration();
            for (var tick = 0; tick <= maxTicks + 20; tick++) {
                scope.run(() -> SpellDispenserBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), spellDispenser));
            }

            helper.assertFalse(spellDispenser.hasActiveContinuousCast(),
                    "Spell Dispenser CONTINUOUS cast restarted even though the redstone signal never toggled off");
            scope.assertReleased(helper, "Spell Dispenser proxy caster was left behind after held-signal completion");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserBlockEntityMarksContinuousResetRequirementAfterHeldCompletion(GameTestHelper helper) {
        var scope = new DispenserTestScope(helper);
        var level = (ServerLevel) helper.getLevel();
        var pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
        helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
        var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
        );
        var scrollStack = createSpellScroll(spell);
        spellDispenser.getInventory().setStackInSlot(0, scrollStack);
        spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_reset_required_test"));

        helper.runAtTickTime(1, () -> {
            var startResult = scope.start(() -> SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    helper.absolutePos(pos),
                    Direction.NORTH,
                    createSpellDispenserValidation(scrollStack, spell),
                    scrollStack,
                    spellDispenser.getOwnerProfile()
            ));
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser block entity failed to prepare a reset-required CONTINUOUS session");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser block entity did not receive a reset-required CONTINUOUS session");
            spellDispenser.startContinuousCast(startResult.session());

            var maxTicks = startResult.session().magicData().getCastDuration();
            for (var tick = 0; tick <= maxTicks + 20; tick++) {
                scope.run(() -> SpellDispenserBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), spellDispenser));
            }

            helper.assertFalse(spellDispenser.hasActiveContinuousCast(),
                    "Spell Dispenser CONTINUOUS cast stayed active after its held-signal duration cap");
            helper.assertTrue(spellDispenser.requiresContinuousReset(),
                    "Spell Dispenser did not record that CONTINUOUS needs a signal reset after held completion");
            helper.assertTrue(spellDispenser.isCoolingDown(),
                    "Spell Dispenser did not enter cooldown after a held CONTINUOUS cast completed");
            scope.assertReleased(helper, "Spell Dispenser proxy caster was left behind after held completion reset state");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserBlockEntityStartsAndClearsInstantCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var pos = new BlockPos(0, 1, 0);
                helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

                var blockEntity = helper.getBlockEntity(pos);
                helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
                var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);

                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                var scrollStack = createSpellScroll(spell);
                spellDispenser.getInventory().setStackInSlot(0, scrollStack);
                spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_cooldown_test"));

                var firstResult = scope.capture(() -> spellDispenser.tryActivate());
                helper.assertTrue(firstResult.succeeded(), "Spell Dispenser failed to activate an INSTANT spell for cooldown test");
                helper.assertTrue(spellDispenser.getRemainingCooldownTicks() == spell.getSpellCooldown(),
                        "Spell Dispenser stored the wrong cooldown after an INSTANT cast: " + spellDispenser.getRemainingCooldownTicks());

                var blockedResult = scope.capture(() -> spellDispenser.tryActivate());
                helper.assertTrue(!blockedResult.succeeded() && !blockedResult.reachedOnCast(),
                        "Spell Dispenser did not reject activation while cooling down");

                for (var tick = 0; tick < spell.getSpellCooldown(); tick++) {
                    scope.run(() -> SpellDispenserBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), spellDispenser));
                }

                helper.assertFalse(spellDispenser.isCoolingDown(), "Spell Dispenser cooldown did not expire after the expected number of ticks");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "spell_dispenser_block_server_disabled_config")
    public static void spellDispenserBlockEntityReportsServerDisabledActivation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                        false,
                        false,
                        List.of(),
                        1.0D
                )) {
                    var pos = new BlockPos(0, 1, 0);
                    helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

                    var blockEntity = helper.getBlockEntity(pos);
                    helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
                    var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);
                    spellDispenser.getInventory().setStackInSlot(
                            0,
                            createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
                    );

                    var result = scope.capture(() -> spellDispenser.tryActivate());
                    helper.assertTrue(!result.succeeded(), "Spell Dispenser activated while disabled by server config");
                    helper.assertTrue(result.failureType() == SpellDispenserCastHelper.FailureType.SERVER_DISABLED,
                            "Spell Dispenser returned the wrong server-disabled activation failure: " + result.failureType());
                    helper.assertFalse(spellDispenser.isCoolingDown(),
                            "Spell Dispenser entered cooldown despite server-disabled activation");
                }
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "spell_dispenser_block_cooldown_multiplier_config")
    public static void spellDispenserBlockEntityAppliesCooldownMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                        true,
                        false,
                        List.of(),
                        0.1D
                )) {
                    var pos = new BlockPos(0, 1, 0);
                    helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

                    var blockEntity = helper.getBlockEntity(pos);
                    helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
                    var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);
                    var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                    spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(spell));
                    spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_cooldown_multiplier_test"));

                    var result = scope.capture(() -> spellDispenser.tryActivate());
                    var expectedCooldown = Math.max(1, (int) Math.ceil(spell.getSpellCooldown() * 0.1D));
                    helper.assertTrue(result.succeeded(), "Spell Dispenser failed to activate for cooldown multiplier test");
                    helper.assertTrue(result.cooldownTicks() == expectedCooldown,
                            "Spell Dispenser returned the wrong scaled cooldown: " + result.cooldownTicks() + " / expected " + expectedCooldown);
                    helper.assertTrue(spellDispenser.getRemainingCooldownTicks() == expectedCooldown,
                            "Spell Dispenser stored the wrong scaled cooldown: " + spellDispenser.getRemainingCooldownTicks());
                }
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserActivationFailsWhenManaIsInsufficient(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var pos = new BlockPos(0, 1, 0);
                helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

                var blockEntity = helper.getBlockEntity(pos);
                helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
                var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);

                var spell = SpellRegistry.COMPOUND_PHIAL.get();
                var requiredMana = spell.getManaCost(1);
                spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(spell));
                spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_low_mana_test"));
                spellDispenser.setCurrentMana(Math.max(0, requiredMana - 1));

                var castResult = scope.capture(() -> spellDispenser.tryActivate());
                helper.assertTrue(!castResult.succeeded(), "Spell Dispenser activated even though it had insufficient mana");
                helper.assertTrue(castResult.insufficientMana(), "Spell Dispenser returned the wrong failure for insufficient mana");
                helper.assertTrue(spellDispenser.getCurrentMana() == Math.max(0, requiredMana - 1),
                        "Spell Dispenser changed mana despite rejecting the cast");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var pos = new BlockPos(0, 1, 0);
                helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
                helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));

                var blockEntity = helper.getBlockEntity(pos);
                helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
                var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);

                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
                );
                spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(spell));
                spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_continuous_low_mana_test"));
                spellDispenser.setCurrentMana(spell.getManaCost(1));

                var startResult = scope.capture(() -> spellDispenser.tryActivate());
                helper.assertTrue(startResult.succeeded(), "Spell Dispenser failed to start a CONTINUOUS cast with exactly one cost worth of mana");
                helper.assertTrue(spellDispenser.hasActiveContinuousCast(), "Spell Dispenser did not keep the CONTINUOUS session active after start");
                helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                        "Spell Dispenser did not consume the initial CONTINUOUS mana cost at start: " + spellDispenser.getCurrentMana());

                for (var tick = 0; tick < 40 && spellDispenser.hasActiveContinuousCast(); ++tick) {
                    scope.run(() -> SpellDispenserBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), spellDispenser));
                }

                helper.assertFalse(spellDispenser.hasActiveContinuousCast(),
                        "Spell Dispenser kept a CONTINUOUS cast active after its mana ran out");
                helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                        "Spell Dispenser mana changed unexpectedly after a mana-depleted CONTINUOUS stop");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserBlockEntityRejectsActivationWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var pos = new BlockPos(0, 1, 0);
                helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
                var blockEntity = helper.getBlockEntity(pos);
                helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");

                var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);
                spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get()));

                var castResult = scope.capture(() -> spellDispenser.tryActivate());
                helper.assertTrue(!castResult.succeeded(), "Spell Dispenser activated without an owner profile");
                helper.assertTrue(castResult.missingOwnerProfile(), "Spell Dispenser returned the wrong failure for missing owner profile");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserBlockEntityAllowsOwnerOptionalActivationWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var pos = new BlockPos(0, 1, 0);
                helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
                var blockEntity = helper.getBlockEntity(pos);
                helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");

                var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);
                var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
                spellDispenser.getInventory().setStackInSlot(0, scrollStack);

                var castResult = scope.capture(() -> spellDispenser.tryActivate());
                helper.assertTrue(castResult.succeeded(), "Spell Dispenser rejected an owner-optional spell without an owner profile");
                scope.assertReleased(helper, "Spell Dispenser block entity owner-optional cast left proxy state behind");
            }
        });
    }

}
