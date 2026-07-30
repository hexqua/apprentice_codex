package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaff;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffCastEvent;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffCoolingHandler;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

final class CircuitHeatStaffGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private CircuitHeatStaffGameTestScenarios() {
    }

    static void circuitHeatStaffKeepsExpectedStatsAndOverheatState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var item = (CircuitHeatStaff) stack.getItem();
            var modifiers = item.getDefaultAttributeModifiers(stack);

            assertModifierAmount(
                    helper,
                    modifiers,
                    Attributes.ATTACK_DAMAGE.value(),
                    EquipmentSlotGroup.MAINHAND,
                    3.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Circuit Heat Staff attack damage modifier changed"
            );
            assertModifierAmount(
                    helper,
                    modifiers,
                    Attributes.ATTACK_SPEED.value(),
                    EquipmentSlotGroup.MAINHAND,
                    -3.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Circuit Heat Staff attack speed modifier changed"
            );
            assertModifierAmount(
                    helper,
                    modifiers,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND,
                    0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Circuit Heat Staff spell power modifier changed"
            );
            assertModifierAmount(
                    helper,
                    modifiers,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND,
                    0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Circuit Heat Staff fire spell power modifier changed"
            );
            assertModifierAmount(
                    helper,
                    modifiers,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND,
                    0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Circuit Heat Staff lightning spell power modifier changed"
            );
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Circuit Heat Staff should not expose an imbue spell container");

            CircuitHeatStaff.startStaffOverheat(stack, helper.getLevel(), 20 * 45);
            var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(stack, helper.getLevel());
            helper.assertTrue(remainingOverheatTicks == 20 * 45,
                    "Circuit Heat Staff item overheat should keep the requested duration: "
                            + remainingOverheatTicks);

            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedCircuitHeatStaffEnchantments(helper.getLevel().registryAccess(), stack),
                    "Circuit Heat Staff"
            );
        });
    }

    static void circuitHeatStaffClampsPersistedFutureItemOverheat(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var stack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(
                    "CircuitHeatStaffOverheatExpireGameTime",
                    level.getGameTime() + 72000L
            ));

            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(stack, level);
            var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();

            helper.assertTrue(remainingTicks <= ApprenticeCodexServerConfig.savedAbsoluteTickClampMaxTicks(),
                    "Circuit Heat Staff future item overheat should be clamped to the repair limit");
            helper.assertTrue(tag.getLong("CircuitHeatStaffOverheatExpireGameTime")
                            <= level.getGameTime() + ApprenticeCodexServerConfig.savedAbsoluteTickClampMaxTicks(),
                    "Circuit Heat Staff item NBT should be rewritten after clamping");
        });
    }

    static void circuitHeatStaffKeepsStoredLongItemOverheatDuration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var stack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var longOverheatTicks = (int) Math.min(
                    (long) ApprenticeCodexServerConfig.savedAbsoluteTickClampMaxTicks() + 1200L,
                    Integer.MAX_VALUE
            );

            CircuitHeatStaff.startStaffOverheat(stack, level, longOverheatTicks);
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(stack, level);

            helper.assertTrue(remainingTicks == longOverheatTicks,
                    "Circuit Heat Staff item overheat should keep stored long duration: " + remainingTicks);
            var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            helper.assertTrue(tag.getInt("CircuitHeatStaffOverheatDurationTicks") == longOverheatTicks,
                    "Circuit Heat Staff item overheat should store the applied duration");
        });
    }

    static void circuitHeatStaffAdditionalManaScalesWithSkippedCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var baseManaCost = 100;
            var step = 1;

            var referenceAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 20 * 10);
            helper.assertTrue(referenceAdditionalMana == 20,
                    "Circuit Heat Staff skipped 10 seconds should keep the old step-1 extra mana: "
                            + referenceAdditionalMana);

            var shortAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 20 * 5);
            helper.assertTrue(shortAdditionalMana == 10,
                    "Circuit Heat Staff skipped 5 seconds should halve the step-1 extra mana: "
                            + shortAdditionalMana);

            var longAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 20 * 40);
            helper.assertTrue(longAdditionalMana == 80,
                    "Circuit Heat Staff skipped 40 seconds should quadruple the step-1 extra mana: "
                            + longAdditionalMana);

            var noSkippedCooldownAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 0);
            helper.assertTrue(noSkippedCooldownAdditionalMana == 0,
                    "Circuit Heat Staff should not add mana when no cooldown is skipped: "
                            + noSkippedCooldownAdditionalMana);
        });
    }

    static void circuitHeatStaffAdditionalManaUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    100,
                    0.50D,
                    0.25D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var additionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                        .getAdditionalManaCost(100, 2, 50);
                helper.assertTrue(additionalMana == 100,
                        "Circuit Heat Staff extra mana should use server config multipliers: " + additionalMana);
            }
        });
    }

    static void circuitHeatStaffOverheatUsesCastCooldownPlusSkippedCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_overheat_duration_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGIC_SPEAR.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff overheat duration test could not resolve player mana data");

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            magicData.setPlayerCastingItem(staffStack);

            var castCooldownTicks = 20 * 120;
            var skippedCooldownTicks = 20 * 40;
            var expectedOverheatTicks = castCooldownTicks + skippedCooldownTicks;
            var plannedManaCost = Math.max(1, spell.getManaCost(1));
            CircuitHeatStaffCastEvent.reserveOverheatCast(
                    player,
                    spell.getSpellId(),
                    plannedManaCost,
                    plannedManaCost,
                    expectedOverheatTicks
            );

            magicData.setMana(plannedManaCost);
            var event = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    spell.getManaCost(1),
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            NeoForge.EVENT_BUS.post(event);

            var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(staffStack, helper.getLevel());
            helper.assertTrue(remainingOverheatTicks == expectedOverheatTicks,
                    "Circuit Heat Staff item overheat should use cast cooldown plus skipped cooldown: "
                            + remainingOverheatTicks + " / expected " + expectedOverheatTicks);

            CircuitHeatStaffCastEvent.clearReservedOverheatCast(player);
        });
    }

    static void circuitHeatStaffOverheatDurationUsesServerMinTicks(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.0D,
                    0.0D,
                    0,
                    List.of(),
                    0.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var context = createCircuitHeatStaffBypassTestContext(
                        helper,
                        "circuit_heat_staff_overheat_min_config_test",
                        SpellRegistry.MANA_SLASH.get()
                );
                var baseManaCost = context.spell().getManaCost(1);
                context.magicData().setMana(baseManaCost);

                var result = context.staffStack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.CONSUME,
                        "Circuit Heat Staff min overheat config test should cast but got " + result.getResult());
                context.magicData().setPlayerCastingItem(context.staffStack());
                postCircuitHeatStaffSpellOnCastEvent(context, baseManaCost);

                var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(
                        context.staffStack(),
                        helper.getLevel()
                );
                helper.assertTrue(remainingOverheatTicks == 20 * 10,
                        "Circuit Heat Staff item overheat should use configured minimum: " + remainingOverheatTicks);
            }
        });
    }

    static void circuitHeatStaffOverheatDurationUsesServerCapTicks(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.0D,
                    0.0D,
                    0,
                    List.of(),
                    100.0D,
                    0,
                    40,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var context = createCircuitHeatStaffBypassTestContext(
                        helper,
                        "circuit_heat_staff_overheat_cap_config_test",
                        SpellRegistry.MANA_SLASH.get()
                );
                var baseManaCost = context.spell().getManaCost(1);
                context.magicData().setMana(baseManaCost);

                var result = context.staffStack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.CONSUME,
                        "Circuit Heat Staff cap overheat config test should cast but got " + result.getResult());
                context.magicData().setPlayerCastingItem(context.staffStack());
                postCircuitHeatStaffSpellOnCastEvent(context, baseManaCost);

                var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(
                        context.staffStack(),
                        helper.getLevel()
                );
                helper.assertTrue(remainingOverheatTicks == 40,
                        "Circuit Heat Staff item overheat should use configured cap: " + remainingOverheatTicks);
            }
        });
    }

    static void circuitHeatStaffBypassKeepsBaseManaGate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_base_mana_gate_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff mana gate test could not resolve player mana data");
            var baseManaCost = spell.getManaCost(1);
            magicData.setMana(baseManaCost - 1.0F);

            var selection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null && selection.spellData.getSpell() == spell,
                    "Circuit Heat Staff mana gate test could not resolve the selected spell: " + selection);
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, selection.getCastSource());

            var result = staffStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Circuit Heat Staff should fail forced casts when base mana is insufficient but got " + result.getResult());
            helper.assertTrue(Math.abs(magicData.getMana() - (baseManaCost - 1.0F)) < 1.0e-4F,
                    "Circuit Heat Staff base mana failure should not mutate mana: " + magicData.getMana());
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Circuit Heat Staff should restore the original cooldown after base mana failure");
            helper.assertFalse(jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                            .getState(player, spell.getSpellId()).active(),
                    "Circuit Heat Staff should not store bypass overheat state after base mana failure");
            helper.assertFalse(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff item should not enter overheat cooldown after base mana failure");
        });
    }

    static void circuitHeatStaffCooldownLimitBlocksBypass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    1,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var context = createCircuitHeatStaffBypassTestContext(
                        helper,
                        "circuit_heat_staff_cooldown_limit_config_test",
                        SpellRegistry.MANA_SLASH.get()
                );
                context.magicData().setMana(context.spell().getManaCost(1) * 10.0F);

                var result = context.staffStack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Circuit Heat Staff should fail cooldown bypass above server limit but got " + result.getResult());
                helper.assertTrue(context.magicData().getPlayerCooldowns().isOnCooldown(context.spell()),
                        "Circuit Heat Staff should keep cooldown when server limit blocks bypass");
                helper.assertFalse(CircuitHeatStaff.isStaffOverheated(context.staffStack(), helper.getLevel()),
                        "Circuit Heat Staff should not overheat when server limit blocks bypass");
            }
        });
    }

    static void circuitHeatStaffSpellDenylistBlocksBypass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_SLASH.get();
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(spell.getSpellId()),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                var context = createCircuitHeatStaffBypassTestContext(
                        helper,
                        "circuit_heat_staff_spell_denylist_config_test",
                        spell
                );
                context.magicData().setMana(spell.getManaCost(1) * 10.0F);

                var result = context.staffStack().getItem().use(helper.getLevel(), context.player(), InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Circuit Heat Staff should fail cooldown bypass for denied spells but got " + result.getResult());
                helper.assertTrue(context.magicData().getPlayerCooldowns().isOnCooldown(spell),
                        "Circuit Heat Staff should keep cooldown when spell denylist blocks bypass");
                helper.assertFalse(CircuitHeatStaff.isStaffOverheated(context.staffStack(), helper.getLevel()),
                        "Circuit Heat Staff should not overheat when spell denylist blocks bypass");
            }
        });
    }

    static void circuitHeatStaffContinuousBypassKeepsOverheatManaCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_continuous_mana_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff continuous mana test could not resolve player mana data");

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            magicData.getSyncedData();
            magicData.initiateCast(
                    spell,
                    1,
                    spell.getCastTime(1),
                    CastSource.SPELLBOOK,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            magicData.setPlayerCastingItem(staffStack);

            var baseManaCost = spell.getManaCost(1);
            var plannedManaCost = baseManaCost
                    + jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, 1, 20 * 10);
            CircuitHeatStaffCastEvent.reserveOverheatCast(
                    player,
                    spell.getSpellId(),
                    plannedManaCost,
                    plannedManaCost * 3.0F,
                    60,
                    true
            );

            magicData.setMana(plannedManaCost * 3.0F);
            var firstEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            NeoForge.EVENT_BUS.post(firstEvent);
            helper.assertTrue(firstEvent.getManaCost() == plannedManaCost,
                    "Circuit Heat Staff continuous first tick should use overheated mana cost: " + firstEvent.getManaCost());
            helper.assertFalse(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff should not enter item overheat while overheated continuous mana can still be paid");

            magicData.setMana(plannedManaCost + 5.0F);
            var secondEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            NeoForge.EVENT_BUS.post(secondEvent);
            helper.assertTrue(secondEvent.getManaCost() == plannedManaCost,
                    "Circuit Heat Staff continuous later tick should keep overheated mana cost: " + secondEvent.getManaCost());
            helper.assertFalse(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff should still avoid item overheat while continuous mana remains above the overheated cost");

            magicData.setMana(plannedManaCost);
            var depletionEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            NeoForge.EVENT_BUS.post(depletionEvent);
            helper.assertTrue(depletionEvent.getManaCost() == plannedManaCost,
                    "Circuit Heat Staff continuous depletion tick should keep overheated mana cost: " + depletionEvent.getManaCost());
            helper.assertTrue(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff should enter item overheat when the overheated continuous cost depletes mana");
            helper.assertTrue(
                    CircuitHeatStaff.formatOverheatManaCostForDisplay(spell, plannedManaCost).equals(plannedManaCost * 2 + "/s"),
                    "Circuit Heat Staff continuous warning should display per-second mana"
            );

            CircuitHeatStaffCastEvent.clearReservedOverheatCast(player);
            magicData.resetCastingState();
        });
    }

    static void circuitHeatStaffRecastDoesNotTouchBypassOverheatState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_recast_neutral_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff recast test could not resolve player mana data");
            magicData.setMana(0.0F);

            jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager.applyAfterBypass(
                    player,
                    spell.getSpellId(),
                    200
            );
            jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager.applyAfterBypass(
                    player,
                    spell.getSpellId(),
                    200
            );
            var stateBefore = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getState(player, spell.getSpellId());
            helper.assertTrue(stateBefore.active() && stateBefore.chainDepth() == 2,
                    "Circuit Heat Staff recast setup should start from bypass chain depth 2 but got " + stateBefore);

            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SPELLBOOK,
                    null
            ), magicData);
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Circuit Heat Staff recast setup should create an active recast");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Circuit Heat Staff recast setup should not leave a normal cooldown");
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 100);
            var staffOverheatBefore = CircuitHeatStaff.getStaffOverheatRemainingTicks(staffStack, helper.getLevel());
            helper.assertTrue(staffOverheatBefore > 0,
                    "Circuit Heat Staff recast setup should start from item overheat cooldown");

            var result = staffStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.CONSUME,
                    "Circuit Heat Staff recast should start through the recast-neutral path during item overheat but got "
                            + result.getResult());
            var stateAfterUse = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getState(player, spell.getSpellId());
            helper.assertTrue(stateAfterUse.active()
                            && stateAfterUse.chainDepth() == stateBefore.chainDepth()
                            && stateAfterUse.expireGameTime() == stateBefore.expireGameTime(),
                    "Circuit Heat Staff recast use should not mutate bypass state: " + stateAfterUse
                            + " / before " + stateBefore);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Circuit Heat Staff recast use should not consume mana before cast resolution: " + magicData.getMana());

            spell.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Circuit Heat Staff recast resolution should keep Iron's no-mana recast behavior: " + magicData.getMana());
            helper.assertTrue(CircuitHeatStaff.getStaffOverheatRemainingTicks(staffStack, helper.getLevel()) == staffOverheatBefore,
                    "Circuit Heat Staff recast should ignore existing item overheat without clearing or refreshing it");
            var stateAfterCast = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getState(player, spell.getSpellId());
            helper.assertTrue(stateAfterCast.active()
                            && stateAfterCast.chainDepth() == stateBefore.chainDepth()
                            && stateAfterCast.expireGameTime() == stateBefore.expireGameTime(),
                    "Circuit Heat Staff recast resolution should not mutate bypass state: " + stateAfterCast
                            + " / before " + stateBefore);

            magicData.resetCastingState();
        });
    }

    static void circuitHeatStaffDropCoolingConsumesWaterSource(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterPos = new BlockPos(0, 2, 0);
            placeWaterTestBasin(helper, waterPos);
            helper.setBlock(waterPos, Blocks.WATER);

            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
            var itemEntity = spawnItem(helper, waterPos, staffStack);

            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                runDropCoolingProcesses(itemEntity, 3);
            }

            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(itemEntity.getAge() == Short.MIN_VALUE,
                    "Circuit Heat Staff drop should use unlimited lifetime while dropped: " + itemEntity.getAge());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff water-source cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(helper.getBlockState(waterPos).isAir(),
                    "Circuit Heat Staff water-source cooling should consume the source after three cycles");
        });
    }

    static void circuitHeatStaffDropCoolingDisabledByServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterPos = new BlockPos(0, 2, 0);
            placeWaterTestBasin(helper, waterPos);
            helper.setBlock(waterPos, Blocks.WATER);

            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
            var itemEntity = spawnItem(helper, waterPos, staffStack);
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    false,
                    10,
                    20 * 10,
                    3,
                    true,
                    true
            )) {
                runDropCoolingProcesses(itemEntity, 3);
                var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
                helper.assertTrue(remainingTicks > 20 * 55,
                        "Circuit Heat Staff cooling should not reduce while disabled by server config: " + remainingTicks);
                helper.assertTrue(helper.getBlockState(waterPos).is(Blocks.WATER),
                        "Circuit Heat Staff cooling should not consume water while disabled by server config");
            }
        });
    }

    static void circuitHeatStaffDropCoolingIgnoresFlowingWater(GameTestHelper helper) {
        var waterPos = new BlockPos(0, 2, 0);
        placeWaterTestBasin(helper, waterPos);
        helper.setBlock(waterPos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnItem(helper, waterPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks > 20 * 55,
                    "Circuit Heat Staff should not use flowing water for cooling: " + remainingTicks);
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingKeepsWaterSourceWhenConsumptionDisabled(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterPos = new BlockPos(0, 2, 0);
            placeWaterTestBasin(helper, waterPos);
            helper.setBlock(waterPos, Blocks.WATER);

            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
            var itemEntity = spawnItem(helper, waterPos, staffStack);
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    false,
                    true
            )) {
                runDropCoolingProcesses(itemEntity, 3);
                var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
                helper.assertTrue(remainingTicks <= 20 * 30,
                        "Circuit Heat Staff water-source cooling should still reduce when consumption is disabled: "
                                + remainingTicks);
                helper.assertTrue(helper.getBlockState(waterPos).is(Blocks.WATER),
                        "Circuit Heat Staff water-source cooling should keep water when consumption is disabled");
            }
        });
    }

    static void circuitHeatStaffDropCoolingConsumesCauldronLevel(GameTestHelper helper) {
        var cauldronPos = new BlockPos(0, 2, 0);
        helper.setBlock(
                cauldronPos,
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
        );

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnNoGravityItem(helper, cauldronPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var state = helper.getBlockState(cauldronPos);
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff cauldron cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 2,
                    "Circuit Heat Staff cauldron cooling should consume one water level after three cycles: " + state);
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingKeepsWaterCauldronWhenConsumptionDisabled(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var cauldronPos = new BlockPos(0, 2, 0);
            helper.setBlock(
                    cauldronPos,
                    Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
            );

            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
            var itemEntity = spawnNoGravityItem(helper, cauldronPos, staffStack);
            try (var ignored = ApprenticeCodexServerConfig.useCircuitHeatStaffConfigOverrideForGameTest(
                    20 * 10,
                    0.10D,
                    0.10D,
                    0,
                    List.of(),
                    1.0D,
                    20 * 10,
                    0,
                    true,
                    10,
                    20 * 10,
                    3,
                    true,
                    false
            )) {
                runDropCoolingProcesses(itemEntity, 3);
                var state = helper.getBlockState(cauldronPos);
                var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
                helper.assertTrue(remainingTicks <= 20 * 30,
                        "Circuit Heat Staff cauldron cooling should still reduce when consumption is disabled: "
                                + remainingTicks);
                helper.assertTrue(state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3,
                        "Circuit Heat Staff cauldron cooling should keep water level when consumption is disabled: "
                                + state);
            }
        });
    }

    static void circuitHeatStaffDropCoolingKeepsPowderSnowBlock(GameTestHelper helper) {
        var powderSnowPos = new BlockPos(0, 2, 0);
        helper.setBlock(powderSnowPos, Blocks.POWDER_SNOW);

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnNoGravityItem(helper, powderSnowPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff powder snow cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(helper.getBlockState(powderSnowPos).is(Blocks.POWDER_SNOW),
                    "Circuit Heat Staff powder snow cooling should not consume powder snow block");
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingKeepsPowderSnowCauldronLevel(GameTestHelper helper) {
        var cauldronPos = new BlockPos(0, 2, 0);
        helper.setBlock(
                cauldronPos,
                Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
        );

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnNoGravityItem(helper, cauldronPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var state = helper.getBlockState(cauldronPos);
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff powder snow cauldron cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(state.is(Blocks.POWDER_SNOW_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3,
                    "Circuit Heat Staff powder snow cauldron cooling should not consume cauldron level: " + state);
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaff(GameTestHelper helper) {
        var waterPos = new BlockPos(0, 2, 0);
        placeWaterTestBasin(helper, waterPos);
        helper.setBlock(waterPos, Blocks.WATER);

        var itemEntity = spawnItem(helper, waterPos, new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get()));

        helper.runAtTickTime(40, () -> {
            helper.assertTrue(itemEntity.getAge() == Short.MIN_VALUE,
                    "Circuit Heat Staff drop should use unlimited lifetime even when it is not overheated: "
                            + itemEntity.getAge());
            helper.assertTrue(helper.getBlockState(waterPos).is(Blocks.WATER),
                    "Circuit Heat Staff should not consume water when it is not overheated");
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaffInPowderSnow(GameTestHelper helper) {
        var powderSnowPos = new BlockPos(0, 2, 0);
        helper.setBlock(powderSnowPos, Blocks.POWDER_SNOW);

        var itemEntity = spawnNoGravityItem(helper, powderSnowPos, new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get()));

        helper.runAtTickTime(40, () -> {
            helper.assertTrue(itemEntity.getAge() == Short.MIN_VALUE,
                    "Circuit Heat Staff drop should use unlimited lifetime in powder snow even when it is not overheated: "
                            + itemEntity.getAge());
            helper.assertTrue(helper.getBlockState(powderSnowPos).is(Blocks.POWDER_SNOW),
                    "Circuit Heat Staff should not change powder snow when it is not overheated");
            helper.succeed();
        });
    }

    private static void runDropCoolingProcesses(ItemEntity itemEntity, int processCount) {
        for (var i = 1; i <= processCount; ++i) {
            itemEntity.tickCount = i * ApprenticeCodexServerConfig.circuitHeatStaffDropCoolingProcessIntervalTicks();
            CircuitHeatStaffCoolingHandler.onEntityItemUpdate(itemEntity.getItem(), itemEntity);
        }
    }
}
