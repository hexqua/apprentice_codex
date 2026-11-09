package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spellgun.EquipmentSpellGunGameTestScenarios.*;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.describeModifiers;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.sumModifierAmount;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.roundTripItemStack;
import static jp.aquafactory.apprenticecodex.gametest.support.UpgradeTestSupport.createUpgradeData;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createInitializedPresetStack;

final class SpellgunEquipmentAttributeScenarios {
    private SpellgunEquipmentAttributeScenarios() {}

    static void spellcasterGunsRemoveBaseSpellPowerButKeepSurge(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellPower = AttributeRegistry.SPELL_POWER.get();
            for (var item : List.of(
                    ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                    ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                    ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                    ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()
            )) {
                var stack = new ItemStack(item);
                var baseModifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
                helper.assertTrue(baseModifiers.get(spellPower).isEmpty(),
                        item.getDescriptionId() + " should not have base spell power");

                stack.enchant(EnchantmentRegistry.SURGE.get(), 1);
                var enchantedSpellPower = sumModifierAmount(
                        stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(spellPower),
                        AttributeModifier.Operation.MULTIPLY_BASE
                );
                helper.assertTrue(Math.abs(enchantedSpellPower - 0.02D) < 1.0e-9D,
                        item.getDescriptionId() + " should retain Surge spell power");
            }
        });
    }

    static void silverSpellAmplifierMovesAllSpellgunAttributesToOffhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var magicMissile = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var attunementAttribute = MagicTools.resolveSchoolPowerAttribute(magicMissile.getSchoolType());
            helper.assertTrue(attunementAttribute != null,
                    "Magic Missile school power attribute should be available for Spellgun calibration test");

            var cases = List.of(
                    new SpellgunAttributeCase(EnchantmentRegistry.ALACRITY.get(),
                            AttributeRegistry.COOLDOWN_REDUCTION.get(),
                            AttributeModifier.Operation.MULTIPLY_BASE, 0.02D),
                    new SpellgunAttributeCase(EnchantmentRegistry.REFLUX.get(),
                            AttributeRegistry.MANA_REGEN.get(),
                            AttributeModifier.Operation.MULTIPLY_BASE, 0.05D),
                    new SpellgunAttributeCase(EnchantmentRegistry.RESERVOIR.get(),
                            AttributeRegistry.MAX_MANA.get(),
                            AttributeModifier.Operation.ADDITION, 20.0D),
                    new SpellgunAttributeCase(EnchantmentRegistry.SURGE.get(),
                            AttributeRegistry.SPELL_POWER.get(),
                            AttributeModifier.Operation.MULTIPLY_BASE, 0.02D),
                    new SpellgunAttributeCase(EnchantmentRegistry.ATTUNEMENT.get(), attunementAttribute,
                            AttributeModifier.Operation.MULTIPLY_BASE,
                            AttributeEnchantmentType.ATTUNEMENT.amountPerLevel()),
                    new SpellgunAttributeCase(EnchantmentRegistry.TENSE.get(),
                            AttributeRegistry.CAST_TIME_REDUCTION.get(),
                            AttributeModifier.Operation.MULTIPLY_BASE,
                            AttributeEnchantmentType.TENSE.amountPerLevel())
            );
            for (var attributeCase : cases) {
                var stack = createInitializedPresetStack(item);
                stack.enchant(attributeCase.enchantment(), 1);
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.MAINHAND, attributeCase,
                        "Unadjusted Spellgun should apply enchanted Attribute in mainhand");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.OFFHAND, attributeCase.withAmount(0.0D),
                        "Unadjusted Spellgun should not apply enchanted Attribute in offhand");

                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                stack, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                        "Spellgun should accept Silver Spell Amplifier calibration");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.MAINHAND, attributeCase.withAmount(0.0D),
                        "Adjusted Spellgun should remove enchanted Attribute from mainhand");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.OFFHAND, attributeCase,
                        "Adjusted Spellgun should apply enchanted Attribute in offhand");

                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                stack, 0, ItemStack.EMPTY),
                        "Spellgun should allow Silver Spell Amplifier removal");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.MAINHAND, attributeCase,
                        "Spellgun should restore enchanted Attribute to mainhand after adjustment removal");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.OFFHAND, attributeCase.withAmount(0.0D),
                        "Spellgun should remove enchanted Attribute from offhand after adjustment removal");
            }

        });
    }

    static void silverSpellAmplifierKeepsDualSpellgunModifiersIndependent(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var spellPower = AttributeRegistry.SPELL_POWER.get();
            var mainhandStack = createInitializedPresetStack(item);
            mainhandStack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            var offhandStack = createInitializedPresetStack(item);
            offhandStack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            offhandStack, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                    "Offhand Spellgun should accept Silver Spell Amplifier calibration");

            var mainhandModifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, mainhandStack)
                    .get(spellPower).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE)
                    .toList();
            var offhandModifiers = item.getAttributeModifiers(EquipmentSlot.OFFHAND, offhandStack)
                    .get(spellPower).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE)
                    .toList();
            helper.assertTrue(mainhandModifiers.size() == 1 && offhandModifiers.size() == 1,
                    "Dual Spellguns should each expose one Surge modifier: main=" + mainhandModifiers
                            + ", off=" + offhandModifiers);
            helper.assertFalse(mainhandModifiers.get(0).getId().equals(offhandModifiers.get(0).getId()),
                    "Mainhand and offhand Spellgun modifiers must use different UUIDs");

            var attributeInstance = new AttributeInstance(spellPower, unused -> {
            });
            mainhandModifiers.forEach(attributeInstance::addTransientModifier);
            offhandModifiers.forEach(attributeInstance::addTransientModifier);
            helper.assertTrue(Math.abs(sumModifierAmount(
                            attributeInstance.getModifiers(), AttributeModifier.Operation.MULTIPLY_BASE) - 0.04D) < 1.0e-9D,
                    "Dual Spellgun Surge modifiers should stack to 0.04");

            mainhandModifiers.forEach(modifier -> attributeInstance.removeModifier(modifier.getId()));
            helper.assertTrue(Math.abs(sumModifierAmount(
                            attributeInstance.getModifiers(), AttributeModifier.Operation.MULTIPLY_BASE) - 0.02D) < 1.0e-9D,
                    "Removing one Spellgun should leave the other Spellgun modifier active");
        });
    }

    static void silverSpellAmplifierMovesUpgradeOrbModifiersToOffhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var maxMana = AttributeRegistry.MAX_MANA.get();

            var orbBeforeCalibration = createInitializedPresetStack(item);
            orbBeforeCalibration.enchant(EnchantmentRegistry.RESERVOIR.get(), 1);
            createUpgradeData(
                    helper.getLevel().registryAccess(),
                    orbBeforeCalibration,
                    UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            orbBeforeCalibration, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                    "Spellgun should accept Silver Spell Amplifier after Upgrade Orb data");
            assertAdjustedUpgradeOrbSlots(helper, orbBeforeCalibration, maxMana, "Orb-before-calibration");

            var calibrationBeforeOrb = createInitializedPresetStack(item);
            calibrationBeforeOrb.enchant(EnchantmentRegistry.RESERVOIR.get(), 1);
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            calibrationBeforeOrb, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                    "Spellgun should accept Silver Spell Amplifier before Upgrade Orb data");
            createUpgradeData(
                    helper.getLevel().registryAccess(),
                    calibrationBeforeOrb,
                    UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );
            assertAdjustedUpgradeOrbSlots(helper, calibrationBeforeOrb, maxMana, "Calibration-before-orb");

            var restored = roundTripItemStack(orbBeforeCalibration);
            assertAdjustedUpgradeOrbSlots(helper, restored, maxMana, "Save-load");

            var unadjusted = createInitializedPresetStack(item);
            unadjusted.enchant(EnchantmentRegistry.RESERVOIR.get(), 1);
            createUpgradeData(
                    helper.getLevel().registryAccess(),
                    unadjusted,
                    UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );
            var unadjustedMainhand = resolveRuntimeSpellgunModifiers(unadjusted, EquipmentSlot.MAINHAND);
            helper.assertTrue(Math.abs(sumModifierAmount(
                            unadjustedMainhand.get(maxMana), AttributeModifier.Operation.ADDITION) - 70.0D) < 1.0e-9D,
                    "Unadjusted Spellgun should retain Reservoir plus Upgrade Orb in mainhand: "
                            + describeModifiers(unadjustedMainhand));
            helper.assertTrue(resolveRuntimeSpellgunModifiers(unadjusted, EquipmentSlot.OFFHAND).get(maxMana).isEmpty(),
                    "Unadjusted Spellgun should not expose Upgrade Orb in offhand");
        });
    }
}
