package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.SpellScrollTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateDispenserTestSupport.*;

final class CreateDispenserCastingScenarios {
    private CreateDispenserCastingScenarios() {}

    static void spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
            );
            var scrollStack = createSpellScroll(spell);
            var mountedInventory = new ItemStackHandler(1);
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var harness = createSpellDispenserMovementHarness(level, castPos, mountedInventory, createSpellDispenserOwnerProfile("spell_dispenser_create_reset_test"));

            startCreateSpellDispenserMovement(harness);
            tickCreateSpellDispenserMovement(harness);
            helper.assertTrue(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser did not start CONTINUOUS casting when enabled");

            var maxTicks = spell.getEffectiveCastTime(1, new FakePlayer(level, createSpellDispenserOwnerProfile("spell_dispenser_create_reset_probe")));
            for (var tick = 0; tick <= maxTicks + 40 && hasCreateSpellDispenserContinuousCast(harness); tick++) {
                tickCreateSpellDispenserMovement(harness);
            }

            helper.assertFalse(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser CONTINUOUS cast did not stop at its duration cap");
            helper.assertTrue(createSpellDispenserRequiresReset(harness),
                    "Create-mounted Spell Dispenser did not enter reset-required state after held completion");
            helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser did not enter cooldown after held completion");

            tickCreateSpellDispenserMovement(harness);
            helper.assertFalse(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser restarted CONTINUOUS casting without being disabled first");

            setCreateSpellDispenserDisabled(harness, true);
            tickCreateSpellDispenserMovement(harness);
            helper.assertFalse(createSpellDispenserRequiresReset(harness),
                    "Create-mounted Spell Dispenser did not clear reset-required state when disabled");

            for (var tick = 0; tick < spell.getSpellCooldown() && createSpellDispenserIsCoolingDown(harness); tick++) {
                tickCreateSpellDispenserMovement(harness);
            }
            helper.assertFalse(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser cooldown did not expire after the expected ticks");

            setCreateSpellDispenserDisabled(harness, false);
            tickCreateSpellDispenserMovement(harness);
            helper.assertTrue(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser did not restart CONTINUOUS casting after disable/enable");

            stopCreateSpellDispenserMovement(harness);
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Create-mounted Spell Dispenser left proxy state behind after stopMoving");
        });
    }

    static void spellDispenserCreateContinuousCastStopsWhenDisabled(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var scrollStack = createSpellScroll(spell);
            var mountedInventory = new ItemStackHandler(1);
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var harness = createSpellDispenserMovementHarness(level, castPos, mountedInventory, createSpellDispenserOwnerProfile("spell_dispenser_create_disable_test"));

            startCreateSpellDispenserMovement(harness);
            tickCreateSpellDispenserMovement(harness);
            helper.assertTrue(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser did not start Fire Breath CONTINUOUS casting");

            for (var tick = 0; tick < 20; tick++) {
                tickCreateSpellDispenserMovement(harness);
            }

            setCreateSpellDispenserDisabled(harness, true);
            tickCreateSpellDispenserMovement(harness);
            helper.assertFalse(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser kept a CONTINUOUS session active after disable");
            helper.assertFalse(createSpellDispenserRequiresReset(harness),
                    "Create-mounted Spell Dispenser incorrectly required reset after disable cancellation");
            helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser did not enter cooldown after cancelling an already-casting CONTINUOUS spell");
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Create-mounted Spell Dispenser left proxy state behind after disable");
        });
    }

    static void spellDispenserCreateInstantCastUsesCooldown(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = SpellRegistry.COMPOUND_PHIAL.get();
            var scrollStack = createSpellScroll(spell);
            var mountedInventory = new ItemStackHandler(1);
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var harness = createSpellDispenserMovementHarness(level, castPos, mountedInventory, createSpellDispenserOwnerProfile("spell_dispenser_create_instant_test"));

            startCreateSpellDispenserMovement(harness);
            visitCreateSpellDispenserPosition(harness, castPos);
            helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser did not enter cooldown after a non-continuous cast");

            var projectileBox = new AABB(castPos).inflate(5.0D);
            var firstProjectileCount = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox).size();
            helper.assertTrue(firstProjectileCount > 0,
                    "Create-mounted Spell Dispenser did not spawn a projectile on the first cast");

            visitCreateSpellDispenserPosition(harness, castPos);
            var secondProjectileCount = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox).size();
            helper.assertTrue(secondProjectileCount == firstProjectileCount,
                    "Create-mounted Spell Dispenser ignored cooldown and fired again immediately");

            var expectedCooldown = spell.getSpellCooldown()
                    + spell.getEffectiveCastTime(1, new FakePlayer(level, createSpellDispenserOwnerProfile("spell_dispenser_create_instant_probe")));
            for (var tick = 0; tick < expectedCooldown && createSpellDispenserIsCoolingDown(harness); tick++) {
                tickCreateSpellDispenserMovement(harness);
            }

            helper.assertFalse(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser cooldown did not expire after the expected ticks");
            visitCreateSpellDispenserPosition(harness, castPos);
            var thirdProjectileCount = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox).size();
            helper.assertTrue(thirdProjectileCount > secondProjectileCount,
                    "Create-mounted Spell Dispenser did not fire again after cooldown expired");
        });
    }

    static void spellDispenserCreateAppliesCooldownMultiplier(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                    true,
                    false,
                    List.of(),
                    0.1D
            )) {
                var level = (ServerLevel) helper.getLevel();
                var castPos = new BlockPos(0, 1, 0);
                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                var scrollStack = createSpellScroll(spell);
                var mountedInventory = new ItemStackHandler(1);
                mountedInventory.setStackInSlot(0, scrollStack.copy());
                var harness = createSpellDispenserMovementHarness(level, castPos, mountedInventory, null);

                startCreateSpellDispenserMovement(harness);
                visitCreateSpellDispenserPosition(harness, castPos);
                helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                        "Create-mounted Spell Dispenser did not enter cooldown after a scaled-cooldown cast");

                var expectedCooldown = Math.max(1, (int) Math.ceil(spell.getSpellCooldown() * 0.1D));
                for (var tick = 0; tick < expectedCooldown && createSpellDispenserIsCoolingDown(harness); tick++) {
                    tickCreateSpellDispenserMovement(harness);
                }

                helper.assertFalse(createSpellDispenserIsCoolingDown(harness),
                        "Create-mounted Spell Dispenser cooldown did not expire after the scaled cooldown ticks");
                assertNoSpellDispenserProxy(helper, castPos, scrollStack,
                        "Create-mounted Spell Dispenser cooldown multiplier test left proxy state behind");
            }
        });
    }

    static void spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            var mountedInventory = new ItemStackHandler(1);
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var harness = createSpellDispenserMovementHarness(level, castPos, mountedInventory, null);

            startCreateSpellDispenserMovement(harness);
            visitCreateSpellDispenserPosition(harness, castPos);
            helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser did not cast an owner-optional spell without an owner profile");
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Create-mounted Spell Dispenser owner-optional cast left proxy state behind");
        });
    }
}
