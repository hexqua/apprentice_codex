package jp.aquafactory.apprenticecodex.gametest.equipment.armor;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.describeModifiers;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.sumModifierAmount;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.toModifierMultimap;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.toModifierMultimap;

public final class ElementMaidenTestSupport {
    private ElementMaidenTestSupport() {}

    public static ItemStack createElementMaidenRobeSchoolPowerSpellbook(GameTestHelper helper, AbstractSpell... spells) {
        var item = ItemRegistry.DIAMOND_SPELL_BOOK.get();
        var stack = new ItemStack(item);
        if (item instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(stack);
        }

        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Element Maiden Robe test spellbook did not initialize a spell container");
        helper.assertTrue(spellContainer.getMaxSpellCount() >= spells.length,
                "Element Maiden Robe test spellbook needs " + spells.length + " slots but got "
                        + spellContainer.getMaxSpellCount());

        var mutable = spellContainer.mutableCopy();
        for (var index = 0; index < spells.length; ++index) {
            helper.assertTrue(mutable.addSpellAtIndex(spells[index], 1, index, false),
                    "Failed to add Element Maiden Robe test spell " + spells[index].getSpellResource()
                            + " at index " + index);
        }
        ISpellContainer.set(stack, mutable.toImmutable());
        return stack;
    }

    public static void assertElementMaidenSchoolPowerBonusAmount(
            GameTestHelper helper,
            Map<Attribute, Double> bonuses,
            Attribute attribute,
            double expectedAmount,
            String message
    ) {
        var actualAmount = bonuses.getOrDefault(attribute, 0.0D);
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount
                        + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(attribute)
                        + " bonuses=" + bonuses);
    }

    public static void assertElementMaidenDynamicSchoolPowerBonuses(
            GameTestHelper helper,
            Player player,
            Map<Attribute, Double> expectedBonuses,
            String message
    ) {
        if (expectedBonuses.isEmpty()) {
            assertAllElementMaidenRobeStacksHaveNoStoredSchoolPower(helper, player, message);
            return;
        }

        var targetArmors = findElementMaidenDynamicBonusTargets(player);
        helper.assertTrue(!targetArmors.isEmpty(), message + ": missing Element Maiden Robe bonus target armor");
        var totalAmounts = new LinkedHashMap<Attribute, Double>();
        for (var targetArmor : targetArmors) {
            var stack = targetArmor.stack();
            var storedBonuses = ElementMaidenRobeItem.getSpellbookSchoolPowerBonuses(stack);
            for (var entry : expectedBonuses.entrySet()) {
                assertElementMaidenSchoolPowerBonusAmount(helper, storedBonuses, entry.getKey(), entry.getValue(),
                        message + " on " + targetArmor.slot());
            }

            var item = (ElementMaidenRobeItem) stack.getItem();
            var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
            for (var entry : expectedBonuses.entrySet()) {
                var attributeHolder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(entry.getKey());
                var actualAmount = sumModifierAmount(
                        modifiers.get(attributeHolder),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                helper.assertTrue(Math.abs(actualAmount - entry.getValue()) < 1.0e-9D,
                        message + ": expected armor attribute " + entry.getValue() + " but got " + actualAmount
                                + " on " + targetArmor.slot()
                                + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey())
                                + " modifiers=" + describeModifiers(modifiers));
                totalAmounts.merge(entry.getKey(), actualAmount, Double::sum);
            }

            for (var entry : storedBonuses.entrySet()) {
                helper.assertTrue(expectedBonuses.containsKey(entry.getKey()),
                        message + ": unexpected stored bonus on " + targetArmor.slot() + " "
                                + BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey())
                                + " amount=" + entry.getValue());
            }
        }

        for (var entry : expectedBonuses.entrySet()) {
            var expectedTotal = entry.getValue() * targetArmors.size();
            var actualTotal = totalAmounts.getOrDefault(entry.getKey(), 0.0D);
            helper.assertTrue(Math.abs(actualTotal - expectedTotal) < 1.0e-9D,
                    message + ": expected stacked armor attribute " + expectedTotal + " but got " + actualTotal
                            + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey())
                            + " equipped robes=" + targetArmors.size());
        }
    }

    public static void assertNoElementMaidenDynamicSchoolPower(
            GameTestHelper helper,
            Player player,
            String message
    ) {
        assertElementMaidenDynamicSchoolPowerBonuses(helper, player, Map.of(), message);
    }

    static List<EquippedArmorStack> findElementMaidenDynamicBonusTargets(Player player) {
        var result = new ArrayList<EquippedArmorStack>();
        for (var slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            var stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ElementMaidenRobeItem) {
                result.add(new EquippedArmorStack(slot, stack));
            }
        }
        return result;
    }

    static void assertAllElementMaidenRobeStacksHaveNoStoredSchoolPower(
            GameTestHelper helper,
            Player player,
            String message
    ) {
        assertOtherElementMaidenRobeStacksHaveNoStoredSchoolPower(helper, player, ItemStack.EMPTY, message);
    }

    static void assertOtherElementMaidenRobeStacksHaveNoStoredSchoolPower(
            GameTestHelper helper,
            Player player,
            ItemStack excludedStack,
            String message
    ) {
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || stack == excludedStack || !(stack.getItem() instanceof ElementMaidenRobeItem)) {
                continue;
            }

            var storedBonuses = ElementMaidenRobeItem.getSpellbookSchoolPowerBonuses(stack);
            helper.assertTrue(storedBonuses.isEmpty(),
                    message + ": unexpected stored school spell power on " + slot + " " + storedBonuses);
        }
    }

    private record EquippedArmorStack(EquipmentSlot slot, ItemStack stack) {
    }
}
