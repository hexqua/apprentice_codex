package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create.CreateDispenserTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserOwnerProfile;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CreateDispenserCastingGameTests {
    private CreateDispenserCastingGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 1, 2));
                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
                );
                var scrollStack = createSpellScroll(spell);
                var mountedInventory = new ItemStackHandler(1);
                mountedInventory.setStackInSlot(0, scrollStack.copy());
                var harness = trackHarness(scope, createSpellDispenserMovementHarness(level, castPos, mountedInventory, createSpellDispenserOwnerProfile("spell_dispenser_create_reset_test")));

                startCreateSpellDispenserMovement(scope, harness);
                tickCreateSpellDispenserMovement(scope, harness);
                helper.assertTrue(hasCreateSpellDispenserContinuousCast(harness),
                        "Create-mounted Spell Dispenser did not start CONTINUOUS casting when enabled");

                var maxTicks = spell.getEffectiveCastTime(1, new FakePlayer(level, createSpellDispenserOwnerProfile("spell_dispenser_create_reset_probe")));
                for (var tick = 0; tick <= maxTicks + 40 && hasCreateSpellDispenserContinuousCast(harness); tick++) {
                    tickCreateSpellDispenserMovement(scope, harness);
                }

                helper.assertFalse(hasCreateSpellDispenserContinuousCast(harness),
                        "Create-mounted Spell Dispenser CONTINUOUS cast did not stop at its duration cap");
                helper.assertTrue(createSpellDispenserRequiresReset(harness),
                        "Create-mounted Spell Dispenser did not enter reset-required state after held completion");
                helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                        "Create-mounted Spell Dispenser did not enter cooldown after held completion");

                tickCreateSpellDispenserMovement(scope, harness);
                helper.assertFalse(hasCreateSpellDispenserContinuousCast(harness),
                        "Create-mounted Spell Dispenser restarted CONTINUOUS casting without being disabled first");

                setCreateSpellDispenserDisabled(scope, harness, true);
                tickCreateSpellDispenserMovement(scope, harness);
                helper.assertFalse(createSpellDispenserRequiresReset(harness),
                        "Create-mounted Spell Dispenser did not clear reset-required state when disabled");

                for (var tick = 0; tick < spell.getSpellCooldown() && createSpellDispenserIsCoolingDown(harness); tick++) {
                    tickCreateSpellDispenserMovement(scope, harness);
                }
                helper.assertFalse(createSpellDispenserIsCoolingDown(harness),
                        "Create-mounted Spell Dispenser cooldown did not expire after the expected ticks");

                setCreateSpellDispenserDisabled(scope, harness, false);
                tickCreateSpellDispenserMovement(scope, harness);
                helper.assertTrue(hasCreateSpellDispenserContinuousCast(harness),
                        "Create-mounted Spell Dispenser did not restart CONTINUOUS casting after disable/enable");

                stopCreateSpellDispenserMovement(scope, harness);
                scope.assertReleased(helper, "Create-mounted Spell Dispenser left proxy state behind after stopMoving");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserCreateContinuousCastStopsWhenDisabled(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 1, 2));
                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
                var scrollStack = createSpellScroll(spell);
                var mountedInventory = new ItemStackHandler(1);
                mountedInventory.setStackInSlot(0, scrollStack.copy());
                var harness = trackHarness(scope, createSpellDispenserMovementHarness(level, castPos, mountedInventory, createSpellDispenserOwnerProfile("spell_dispenser_create_disable_test")));

                startCreateSpellDispenserMovement(scope, harness);
                tickCreateSpellDispenserMovement(scope, harness);
                helper.assertTrue(hasCreateSpellDispenserContinuousCast(harness),
                        "Create-mounted Spell Dispenser did not start Fire Breath CONTINUOUS casting");

                for (var tick = 0; tick < 20; tick++) {
                    tickCreateSpellDispenserMovement(scope, harness);
                }

                setCreateSpellDispenserDisabled(scope, harness, true);
                tickCreateSpellDispenserMovement(scope, harness);
                helper.assertFalse(hasCreateSpellDispenserContinuousCast(harness),
                        "Create-mounted Spell Dispenser kept a CONTINUOUS session active after disable");
                helper.assertFalse(createSpellDispenserRequiresReset(harness),
                        "Create-mounted Spell Dispenser incorrectly required reset after disable cancellation");
                helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                        "Create-mounted Spell Dispenser did not enter cooldown after cancelling an already-casting CONTINUOUS spell");
                scope.assertReleased(helper, "Create-mounted Spell Dispenser left proxy state behind after disable");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserCreateInstantCastUsesCooldown(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 1, 2));
                var spell = SpellRegistry.COMPOUND_PHIAL.get();
                var scrollStack = createSpellScroll(spell);
                var mountedInventory = new ItemStackHandler(1);
                mountedInventory.setStackInSlot(0, scrollStack.copy());
                var harness = trackHarness(scope, createSpellDispenserMovementHarness(level, castPos, mountedInventory, createSpellDispenserOwnerProfile("spell_dispenser_create_instant_test")));

                startCreateSpellDispenserMovement(scope, harness);
                visitCreateSpellDispenserPosition(scope, harness, castPos);
                helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                        "Create-mounted Spell Dispenser did not enter cooldown after a non-continuous cast");

                var projectileBox = new AABB(castPos).inflate(5.0D);
                var firstProjectileCount = scope.entities(CompoundPhialProjectileEntity.class).size();
                helper.assertTrue(firstProjectileCount > 0,
                        "Create-mounted Spell Dispenser did not spawn a projectile on the first cast");

                visitCreateSpellDispenserPosition(scope, harness, castPos);
                var secondProjectileCount = scope.entities(CompoundPhialProjectileEntity.class).size();
                helper.assertTrue(secondProjectileCount == firstProjectileCount,
                        "Create-mounted Spell Dispenser ignored cooldown and fired again immediately");

                var expectedCooldown = spell.getSpellCooldown()
                        + spell.getEffectiveCastTime(1, new FakePlayer(level, createSpellDispenserOwnerProfile("spell_dispenser_create_instant_probe")));
                for (var tick = 0; tick < expectedCooldown && createSpellDispenserIsCoolingDown(harness); tick++) {
                    tickCreateSpellDispenserMovement(scope, harness);
                }

                helper.assertFalse(createSpellDispenserIsCoolingDown(harness),
                        "Create-mounted Spell Dispenser cooldown did not expire after the expected ticks");
                visitCreateSpellDispenserPosition(scope, harness, castPos);
                var thirdProjectileCount = scope.entities(CompoundPhialProjectileEntity.class).size();
                helper.assertTrue(thirdProjectileCount > secondProjectileCount,
                        "Create-mounted Spell Dispenser did not fire again after cooldown expired");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "spell_dispenser_create_cooldown_multiplier_config")
    public static void spellDispenserCreateAppliesCooldownMultiplier(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                        true,
                        false,
                        List.of(),
                        0.1D
                )) {
                    var level = (ServerLevel) helper.getLevel();
                    var castPos = helper.absolutePos(new BlockPos(2, 1, 2));
                    var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                    var scrollStack = createSpellScroll(spell);
                    var mountedInventory = new ItemStackHandler(1);
                    mountedInventory.setStackInSlot(0, scrollStack.copy());
                    var harness = trackHarness(scope, createSpellDispenserMovementHarness(level, castPos, mountedInventory, null));

                    startCreateSpellDispenserMovement(scope, harness);
                    visitCreateSpellDispenserPosition(scope, harness, castPos);
                    helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                            "Create-mounted Spell Dispenser did not enter cooldown after a scaled-cooldown cast");

                    var expectedCooldown = Math.max(1, (int) Math.ceil(spell.getSpellCooldown() * 0.1D));
                    for (var tick = 0; tick < expectedCooldown && createSpellDispenserIsCoolingDown(harness); tick++) {
                        tickCreateSpellDispenserMovement(scope, harness);
                    }

                    helper.assertFalse(createSpellDispenserIsCoolingDown(harness),
                            "Create-mounted Spell Dispenser cooldown did not expire after the scaled cooldown ticks");
                    scope.assertReleased(helper, "Create-mounted Spell Dispenser cooldown multiplier test left proxy state behind");
                }
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 1, 2));
                var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
                var mountedInventory = new ItemStackHandler(1);
                mountedInventory.setStackInSlot(0, scrollStack.copy());
                var harness = trackHarness(scope, createSpellDispenserMovementHarness(level, castPos, mountedInventory, null));

                startCreateSpellDispenserMovement(scope, harness);
                visitCreateSpellDispenserPosition(scope, harness, castPos);
                helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                        "Create-mounted Spell Dispenser did not cast an owner-optional spell without an owner profile");
                scope.assertReleased(helper, "Create-mounted Spell Dispenser owner-optional cast left proxy state behind");
            }
        });
    }

    private static void visitCreateSpellDispenserPosition(DispenserTestScope scope, Object harness, BlockPos pos) {
        scope.run(() -> invokeCreateGameTestHook("visitNewPosition", new Class<?>[]{Object.class, BlockPos.class}, harness, pos));
    }

    private static void stopCreateSpellDispenserMovement(DispenserTestScope scope, Object harness) {
        scope.run(() -> invokeCreateGameTestHook("stopMoving", new Class<?>[]{Object.class}, harness));
    }

    private static void setCreateSpellDispenserDisabled(DispenserTestScope scope, Object harness, boolean disabled) {
        scope.run(() -> invokeCreateGameTestHook("setDisabled", new Class<?>[]{Object.class, boolean.class}, harness, disabled));
    }

    private static boolean hasCreateSpellDispenserContinuousCast(Object harness) {
        return invokeCreateGameTestHookBoolean("hasRunningContinuousCast", harness);
    }

    private static boolean createSpellDispenserRequiresReset(Object harness) {
        return invokeCreateGameTestHookBoolean("requiresContinuousReset", harness);
    }

    private static boolean createSpellDispenserIsCoolingDown(Object harness) {
        return invokeCreateGameTestHookBoolean("isCoolingDown", harness);
    }

    private static boolean invokeCreateGameTestHookBoolean(String methodName, Object harness) {
        return (boolean) invokeCreateGameTestHook(methodName, new Class<?>[]{Object.class}, harness);
    }

}
