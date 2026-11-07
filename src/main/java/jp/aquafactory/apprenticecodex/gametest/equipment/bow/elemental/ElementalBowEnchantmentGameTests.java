package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowEnchantmentGameTests {
    private ElementalBowEnchantmentGameTests() {}

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowKeepsVanillaBowEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            assertReferenceItemEnchantmentsWithRequiredExtras(
                    helper,
                    stack,
                    new ItemStack(Items.BOW),
                    requiredElementalBowExtraEnchantments(),
                    "Elemental Bow"
            );
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowScrollLevelUsesOnlyTranscendence(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowSpellConfig(helper)) {
                var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
                var lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                stack.enchant(lookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER), 5);
                stack.enchant(lookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FLAME), 1);
                stack.enchant(lookup.getOrThrow(Enchantments.TRANSCENDENCE), 3);
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

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, SchoolRegistry.FIRE_RESOURCE.toString());
            var scrollStack = createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertTrue(
                    SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Elemental Bow should reject Arcane Anvil spell imbuing regardless of scroll spell"
            );
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowAcceptsSynthesisEnchantmentsAndTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var synthesis = enchantmentLookup.getOrThrow(Enchantments.SYNTHESIS);
            var infinity = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var mending = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING);
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());

            helper.assertTrue(stack.getItem().supportsEnchantment(stack, synthesis),
                    "Elemental Bow should accept Synthesis at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(synthesis)),
                    "Elemental Bow should accept Synthesis from enchanted books");
            helper.assertTrue(synthesis.value().canEnchant(stack),
                    "Elemental Bow should be included in the Synthesis supported_items tag");
            helper.assertFalse(Enchantment.areCompatible(synthesis, infinity),
                    "Synthesis should be incompatible with Infinity");
            helper.assertFalse(Enchantment.areCompatible(synthesis, mending),
                    "Synthesis should be incompatible with Mending");

            assertTooltipKeyAt(helper, stack, 0, "item.apprenticecodex.elemental_bow.mode",
                    "Elemental Bow should always show the current mode tooltip line");
            assertTooltipKeyUsesColor(helper, stack, "item.apprenticecodex.elemental_bow.desc", ChatFormatting.GRAY,
                    "Elemental Bow should always show the description tooltip line");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment",
                    "Elemental Bow should not show spell ammo tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show Infinity spell tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_synthesis",
                    "Elemental Bow should not show Synthesis spell tooltip while not in magic mode");

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            assertTooltipKeyAt(helper, stack, 1, "item.apprenticecodex.elemental_bow.desc",
                    "Elemental Bow should show the description below the mode tooltip line");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment", "Spellgun details must omit the ammo section");

            var infinityStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(infinityStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            infinityStack.enchant(infinity, 1);
            assertTooltipKeyAbsent(helper, infinityStack, "item.apprenticecodex.elemental_bow.spell.with_infinity", "Spellgun details must omit the ammo section");

            var synthesisStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(synthesisStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            synthesisStack.enchant(synthesis, 1);
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", "Spellgun details must omit the ammo section");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.with_synthesis",
                    "Elemental Bow should no longer show the legacy Synthesis tooltip key");

            synthesisStack.enchant(infinity, 1);
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", "Spellgun details must omit the ammo section");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show the Infinity spell tooltip when Synthesis is also present");
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_power_config")
    public static void elementalBowPowerDoesNotIncreaseSpellLevel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            var power = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER);
            stack.enchant(power, 5);
            helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack).spellLevel() == 1,
                    "Power must not bypass scroll progression");
        });
    }
}
