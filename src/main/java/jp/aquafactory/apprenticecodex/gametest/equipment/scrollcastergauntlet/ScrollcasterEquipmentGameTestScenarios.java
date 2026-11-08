package jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import java.util.LinkedHashSet;
import java.util.Set;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.addExpectedMalumReplenishingIfPresent;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.addExpectedMalumSpiritPlunderIfPresent;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.assertExactEnchantmentSurfaces;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.collectAllowedEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet.ScrollcasterGauntletScenarios.assertScrollcasterGauntletOffhandUseCasts;
import static jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet.ScrollcasterGauntletScenarios.assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertModifierWithId;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertSingleModifierAmount;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.toModifierMultimap;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;

final class ScrollcasterEquipmentGameTestScenarios {
    private ScrollcasterEquipmentGameTestScenarios() {}

    public static void scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();

            assertScrollcasterGauntletOffhandUseCasts(
                    helper,
                    ItemStack.EMPTY,
                    spell,
                    "scrollcaster_gauntlet_offhand_empty_mainhand_test"
            );
            assertScrollcasterGauntletOffhandUseCasts(
                    helper,
                    new ItemStack(Items.STONE_SWORD),
                    spell,
                    "scrollcaster_gauntlet_offhand_stone_sword_test"
            );
            assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem(
                    helper,
                    new ItemStack(ItemRegistry.PASTEL_STAFF.get()),
                    spell,
                    "scrollcaster_gauntlet_offhand_mainhand_staff_test"
            );
            assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem(
                    helper,
                    new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get()),
                    spell,
                    "scrollcaster_gauntlet_offhand_mainhand_casting_item_test"
            );

            var emptyGauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var emptyPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_offhand_empty_selection_test");
            emptyPlayer.setItemInHand(InteractionHand.OFF_HAND, emptyGauntlet);
            var emptyResult = emptyGauntlet.getItem().use(helper.getLevel(), emptyPlayer, InteractionHand.OFF_HAND);
            helper.assertTrue(emptyResult.getResult() == InteractionResult.PASS,
                    "Scrollcaster Gauntlet offhand use without a selected scroll should pass but got "
                            + emptyResult.getResult());
        });
    }

    public static void scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    new ItemStack(Items.SHIELD),
                    "scrollcaster_gauntlet_mainhand_offhand_shield_test"
            );
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()),
                    "scrollcaster_gauntlet_mainhand_offhand_elemental_bow_test"
            );
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    createIronAutoloaderCrossbowStack(helper),
                    "scrollcaster_gauntlet_mainhand_offhand_autoloader_crossbow_test"
            );
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    "scrollcaster_gauntlet_mainhand_offhand_spellgun_test"
            );
        });
    }

    private static void assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
            GameTestHelper helper,
            ItemStack offhandStack,
            String profileName
    ) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(spell));
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStack.copy());
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Scrollcaster Gauntlet mainhand offhand priority test could not resolve player mana data");
        magicData.setMana(100.0F);

        var result = gauntlet.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult() == InteractionResult.PASS,
                "Scrollcaster Gauntlet mainhand use should pass to supported offhand use item "
                        + offhandStack + " but got " + result.getResult());
        helper.assertFalse(magicData.isCasting(),
                "Scrollcaster Gauntlet mainhand use should not cast before supported offhand use item "
                        + offhandStack);
    }

    private static ItemStack createIronAutoloaderCrossbowStack(GameTestHelper helper) {
        var autoloaderCrossbow = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "autoloader_crossbow")
        );
        helper.assertTrue(autoloaderCrossbow != Items.AIR,
                "Missing irons_spellbooks:autoloader_crossbow for Scrollcaster Gauntlet offhand priority test");
        return new ItemStack(autoloaderCrossbow);
    }

    public static void scrollcasterGauntletKeepsExpectedStatsAndBenchEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var expectedTaggedEnchantments = new LinkedHashSet<>(Set.of(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alacrity"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "reflux"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "reservoir"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "tense"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wisdom"),
                    ResourceLocation.withDefaultNamespace("bane_of_arthropods"),
                    ResourceLocation.withDefaultNamespace("fire_aspect"),
                    ResourceLocation.withDefaultNamespace("fortune"),
                    ResourceLocation.withDefaultNamespace("knockback"),
                    ResourceLocation.withDefaultNamespace("looting"),
                    ResourceLocation.withDefaultNamespace("sharpness"),
                    ResourceLocation.withDefaultNamespace("silk_touch"),
                    ResourceLocation.withDefaultNamespace("smite")
            ));
            var expectedDefinitionEnchantments = collectAllowedEnchantments(
                    helper.getLevel().registryAccess(),
                    enchantment -> enchantment.value().isSupportedItem(stack)
            );
            expectedTaggedEnchantments.addAll(expectedDefinitionEnchantments);
            expectedTaggedEnchantments.remove(ResourceLocation.withDefaultNamespace("sweeping_edge"));
            addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedTaggedEnchantments);
            addExpectedMalumSpiritPlunderIfPresent(stack, expectedTaggedEnchantments);
            addExpectedMalumReplenishingIfPresent(stack, expectedTaggedEnchantments);
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedTaggedEnchantments,
                    expectedTaggedEnchantments,
                    expectedDefinitionEnchantments,
                    expectedTaggedEnchantments,
                    expectedTaggedEnchantments,
                    "Scrollcaster Gauntlet"
            );

            var expectedSpell = SpellRegistry.GUIDING_BOLT_SPELL.get();
            ScrollcasterGauntlet.setCalibrationScroll(
                    stack,
                    0,
                    createSpellScroll(expectedSpell)
            );
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.ALACRITY), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.REFLUX), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.RESERVOIR), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.SURGE), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.ATTUNEMENT), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.TENSE), 1);

            var modifiers = toModifierMultimap(stack.getItem().getDefaultAttributeModifiers(stack));
            var epicFightLoaded = ModList.get().isLoaded(EpicFightCompat.MOD_ID);
            var expectedAttackDamageBonus = epicFightLoaded ? 2.0D : 5.0D;
            var expectedAttackSpeedBonus = epicFightLoaded ? 0.0D : -2.2D;
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                    AttributeModifier.Operation.ADD_VALUE,
                    expectedAttackDamageBonus,
                    "Scrollcaster Gauntlet attack damage modifier should match the loaded combat environment"
            );
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                    AttributeModifier.Operation.ADD_VALUE,
                    expectedAttackSpeedBonus,
                    "Scrollcaster Gauntlet attack speed modifier should match the loaded combat environment"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(AttributeRegistry.COOLDOWN_REDUCTION),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    AttributeEnchantmentType.ALACRITY.amountPerLevel(),
                    "Scrollcaster Gauntlet Alacrity modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(AttributeRegistry.MANA_REGEN),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    AttributeEnchantmentType.REFLUX.amountPerLevel(),
                    "Scrollcaster Gauntlet Reflux modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(AttributeRegistry.MAX_MANA),
                    AttributeModifier.Operation.ADD_VALUE,
                    AttributeEnchantmentType.RESERVOIR.amountPerLevel(),
                    "Scrollcaster Gauntlet Reservoir modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.10D,
                    "Scrollcaster Gauntlet spell power should ignore Surge"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(AttributeRegistry.CAST_TIME_REDUCTION),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    AttributeEnchantmentType.TENSE.amountPerLevel(),
                    "Scrollcaster Gauntlet Tense modifier changed"
            );

            var selectedSpell = ScrollcasterGauntlet.getSelectedSpellData(stack);
            helper.assertTrue(selectedSpell != SpellData.EMPTY
                            && selectedSpell.getSpell() == expectedSpell,
                    "Scrollcaster Gauntlet must expose the inserted Guiding Bolt before checking school modifiers");
            var imbuedSchool = selectedSpell.getSpell().getSchoolType();
            helper.assertTrue(imbuedSchool != null,
                    "Scrollcaster Gauntlet test could not resolve the selected spell school");
            var attunementAttribute = MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Scrollcaster Gauntlet test could not resolve the Attunement spell power attribute: " + imbuedSchool.getId());
            helper.assertTrue(modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementAttribute)).isEmpty(),
                    "Scrollcaster Gauntlet spell power should ignore Attunement");
        });
    }
}
