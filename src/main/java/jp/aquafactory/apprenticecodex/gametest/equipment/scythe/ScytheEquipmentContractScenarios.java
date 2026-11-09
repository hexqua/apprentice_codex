package jp.aquafactory.apprenticecodex.gametest.equipment.scythe;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScythe;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.ModList;

import java.util.Objects;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.assertExactEnchantmentSurfaces;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.equipment.scythe.ScytheEquipmentContractGameTests.expectedSpellReaperScytheEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_SOUL_HUNTER_WEAPON;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.*;

final class ScytheEquipmentContractScenarios {
    private ScytheEquipmentContractScenarios() {}

    static void spellReaperScytheKeepsExpectedStatsTagsEnchantmentsAndImbueContract(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellReaperScythe) ItemRegistry.SPELL_REAPER_SCYTHE.get();
            var stack = item.getDefaultInstance();
            helper.assertTrue(stack.getMaxDamage() == SpellReaperScythe.DURABILITY,
                    "Spell Reaper Scythe durability should be " + SpellReaperScythe.DURABILITY
                            + " but got " + stack.getMaxDamage());
            helper.assertTrue(item.getEnchantmentValue(stack) == SpellReaperScythe.ENCHANTMENT_VALUE,
                    "Spell Reaper Scythe enchantability should be " + SpellReaperScythe.ENCHANTMENT_VALUE
                            + " but got " + item.getEnchantmentValue(stack));

            var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    SpellReaperScythe.DISPLAY_ATTACK_DAMAGE - 1.0D,
                    "Spell Reaper Scythe attack damage modifier should display as 10 damage"
            );
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    SpellReaperScythe.DISPLAY_ATTACK_SPEED - 4.0D,
                    "Spell Reaper Scythe attack speed modifier should display as 1.0 speed"
            );

            var container = ISpellContainer.get(stack);
            helper.assertTrue(container != null && container.getMaxSpellCount() == 1
                            && container.isSpellWheel() && !container.mustEquip() && container.isEmpty(),
                    "Spell Reaper Scythe should initialize an empty one-slot spell-wheel container");
            helper.assertTrue(item.createSpellSlotUpgradeResult(
                    stack,
                    (SpellSlotUpgradeItem) io.redspace.ironsspellbooks.registries.ItemRegistry
                            .LESSER_SPELL_SLOT_UPGRADE.get()
            ).isEmpty(), "Spell Reaper Scythe should reject spell-slot upgrades");

            var mutable = Objects.requireNonNull(container, "Spell Reaper Scythe spell container").mutableCopy();
            helper.assertTrue(mutable.addSpellAtIndex(
                            SpellRegistry.HEAL_SPELL.get(), 1, 0, false),
                    "Failed to prepare an imbued Spell Reaper Scythe");
            ISpellContainer.set(stack, mutable.toImmutable());
            var shrivenStack = Utils.handleShriving(stack);
            var shrivenContainer = ISpellContainer.get(shrivenStack);
            helper.assertFalse(shrivenStack.isEmpty(),
                    "Shriving Stone should accept an imbued Spell Reaper Scythe");
            helper.assertTrue(shrivenContainer != null && shrivenContainer.getMaxSpellCount() == 1
                            && shrivenContainer.isEmpty(),
                    "Shriving Stone should clear the Spell Reaper Scythe spell while preserving its slot");

            helper.assertTrue(stack.is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Spell Reaper Scythe is missing apprenticecodex:spellcaster_workbench_extractable");
            helper.assertTrue(stack.is(MALUM_SOUL_HUNTER_WEAPON),
                    "Spell Reaper Scythe is missing malum:soul_hunter_weapon");
            helper.assertTrue(item.isValidRepairItem(stack, new ItemStack(Items.NETHERITE_INGOT)),
                    "Spell Reaper Scythe must accept netherite repairs");
            helper.assertTrue(item.canPerformAction(stack, ToolActions.SWORD_SWEEP)
                            != ModList.get().isLoaded(MALUM_MOD_ID),
                    "Spell Reaper Scythe should expose vanilla sweep only while Malum is absent");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedSpellReaperScytheEnchantments(stack),
                    "Spell Reaper Scythe"
            );
        });
    }
}
