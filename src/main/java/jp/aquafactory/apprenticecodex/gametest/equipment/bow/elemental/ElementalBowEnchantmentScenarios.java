package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.useElementalBowSpellConfig;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.ElementalBowStateTestSupport.setElementalBowMode;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.ElementalBowStateTestSupport.setElementalBowShotSelection;

final class ElementalBowEnchantmentScenarios {
    private ElementalBowEnchantmentScenarios() {}

    static void elementalBowScrollLevelUsesOnlyTranscendence(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowSpellConfig(helper)) {
                var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
                var lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                stack.enchant(Enchantments.POWER_ARROWS, 5);
                stack.enchant(Enchantments.FLAMING_ARROWS, 1);
                stack.enchant(EnchantmentRegistry.TRANSCENDENCE.get(), 3);
                setElementalBowMode(stack, "fire");
                helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack).spellLevel() == 2,
                        "Only Transcendence may increase the stored scroll level: " + ElementalBow.getDisplayedSpellProfile(stack));
                var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(SpellRegistry.FIRE_ARROW_SPELL.get(), SpellRegistry.FIRE_ARROW_SPELL.get().getMaxLevel(), scroll);
                ElementalBow.setCalibrationScroll(stack, 0, scroll, helper.getLevel().registryAccess());
                helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack).spellLevel() == SpellRegistry.FIRE_ARROW_SPELL.get().getMaxLevel() + 1, "Level must exceed the normal maximum");
                setElementalBowShotSelection(stack, "normal", null);
                helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack) == null, "Physical mode must hide active spell");
                helper.assertFalse(ISpellContainer.isSpellContainer(stack), "Mode changes must never create a spell container");
            }
        });
    }

    static void elementalBowBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            var scrollStack = createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertTrue(
                    SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Elemental Bow should reject Arcane Anvil spell imbuing regardless of scroll spell"
            );
        });
    }

    static void elementalBowPowerDoesNotIncreaseSpellLevel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            var power = Enchantments.POWER_ARROWS;
            stack.enchant(power, 5);
            helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack).spellLevel() == 1,
                    "Power must not bypass scroll progression");
        });
    }
}
