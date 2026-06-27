package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.armor.ApprenticeMageRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressCastEvent;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressItem;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressStats;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeSchoolPowerBonusEvents;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeStats;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeStats;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitStats;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.ApprenticeEnchantmentAvailability;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

final class EquipmentEnchantmentSurfaceGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private EquipmentEnchantmentSurfaceGameTestScenarios() {
    }

    static void scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

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
            helper.assertTrue(emptyResult.getResult() == net.minecraft.world.InteractionResult.PASS,
                    "Scrollcaster Gauntlet offhand use without a selected scroll should pass but got "
                            + emptyResult.getResult());
        });
    }

    static void scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
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
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
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
        helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.PASS,
                "Scrollcaster Gauntlet mainhand use should pass to supported offhand use item "
                        + offhandStack + " but got " + result.getResult());
        helper.assertFalse(magicData.isCasting(),
                "Scrollcaster Gauntlet mainhand use should not cast before supported offhand use item "
                        + offhandStack);
    }

    private static ItemStack createIronAutoloaderCrossbowStack(GameTestHelper helper) {
        var autoloaderCrossbow = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "autoloader_crossbow")
        );
        helper.assertTrue(autoloaderCrossbow != null,
                "Missing irons_spellbooks:autoloader_crossbow for Scrollcaster Gauntlet offhand priority test");
        return new ItemStack(autoloaderCrossbow);
    }

    static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Spell Gun",
                // 1.21.1申し送り事項:
                // enchantable / book / anvil の面は Item 定義と Forge 側フックの移植差で崩れやすい。
                // 1.20.1 の通りに見えても、1.21.1 では spell gun 系をそのまま持ち込める前提にしないこと。
                item -> item instanceof AbstractSpellGunItem,
                ApprenticeCodexGameTestScenarios::expectedSpellGunEnchantments
        ));
    }
    static void reflectcastShieldKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertTrue(stack.is(MALUM_SOUL_HUNTER_WEAPON),
                    "Reflectcast Shield is missing malum:soul_hunter_weapon");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedReflectcastShieldEnchantments(stack),
                    "Reflectcast Shield"
            );
        });
    }
    static void spellchargedGreatswordKeepsExpectedStatsTagsAndEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var stack = new ItemStack(item);
            helper.assertTrue(stack.getMaxDamage() == SpellchargedGreatsword.DURABILITY,
                    "Spellcharged Greatsword durability should be " + SpellchargedGreatsword.DURABILITY
                            + " but got " + stack.getMaxDamage());
            helper.assertTrue(item.getEnchantmentValue(stack) == SpellchargedGreatsword.ENCHANTMENT_VALUE,
                    "Spellcharged Greatsword enchantability should be " + SpellchargedGreatsword.ENCHANTMENT_VALUE
                            + " but got " + item.getEnchantmentValue(stack));
            helper.assertTrue(item instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                    "Spellcharged Greatsword should be a UniqueItem");

            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    SpellchargedGreatsword.DISPLAY_ATTACK_DAMAGE - 1.0D,
                    "Spellcharged Greatsword attack damage modifier should display as 8 damage"
            );
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    SpellchargedGreatsword.DISPLAY_ATTACK_SPEED - 4.0D,
                    "Spellcharged Greatsword attack speed modifier should display as 1.1 speed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(ForgeMod.ENTITY_REACH.get()),
                    AttributeModifier.Operation.ADDITION,
                    SpellchargedGreatsword.ENTITY_REACH_BONUS,
                    "Spellcharged Greatsword entity reach modifier should add 0.5 blocks"
            );

            helper.assertTrue(stack.is(MALUM_SOUL_HUNTER_WEAPON),
                    "Spellcharged Greatsword is missing malum:soul_hunter_weapon");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedSpellchargedGreatswordEnchantments(stack),
                    "Spellcharged Greatsword"
            );
        });
    }
    static void spellchargedGreatswordChargeMathDecayAndAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var stack = new ItemStack(item);

            helper.assertTrue(SpellchargedGreatsword.computeChargeGainTicks(20, 20) == 20.0D,
                    "Spellcharged Greatsword should halve charge gain at 40 ticks or less");
            helper.assertTrue(SpellchargedGreatsword.computeChargeGainTicks(80, 200) == 200.0D,
                    "Spellcharged Greatsword charge gain should be capped to 200 ticks");

            SpellchargedGreatsword.addCharge(stack, 0L, 200.0D);
            assertSpellchargedGreatswordChargeState(helper, stack, 0L, 200.0D, 1,
                    "Spellcharged Greatsword should reach level 1 at 200 charge ticks");
            assertSpellchargedGreatswordAttackAttributes(helper, item, stack, 2.0D, -0.1D,
                    "Spellcharged Greatsword level 1 attributes");

            SpellchargedGreatsword.addCharge(stack, 0L, 200.0D);
            assertSpellchargedGreatswordChargeState(helper, stack, 0L, 400.0D, 2,
                    "Spellcharged Greatsword should reach level 2 at 400 charge ticks");
            assertSpellchargedGreatswordAttackAttributes(helper, item, stack, 5.0D, -0.2D,
                    "Spellcharged Greatsword level 2 attributes");

            SpellchargedGreatsword.addCharge(stack, 0L, 400.0D);
            assertSpellchargedGreatswordChargeState(helper, stack, 100L, 800.0D, 3,
                    "Spellcharged Greatsword should keep full charge during the 5 second decay delay");
            assertSpellchargedGreatswordAttackAttributes(helper, item, stack, 10.0D, -0.4D,
                    "Spellcharged Greatsword level 3 attributes");

            helper.assertTrue(Math.abs(SpellchargedGreatsword.getEffectiveChargeTicks(stack, 101L) - 796.0D) < 1.0e-9D,
                    "Spellcharged Greatsword should decay 4 charge ticks per tick after the delay");
            helper.assertFalse(SpellchargedGreatsword.refreshDecay(stack, 299L),
                    "Spellcharged Greatsword should not reset before charge reaches zero");
            helper.assertTrue(SpellchargedGreatsword.getChargeLevel(stack) == 3,
                    "Spellcharged Greatsword decay should not lower level before charge reaches zero");
            helper.assertTrue(SpellchargedGreatsword.refreshDecay(stack, 300L),
                    "Spellcharged Greatsword should reset when decay reaches zero");
            assertSpellchargedGreatswordChargeState(helper, stack, 300L, 0.0D, 0,
                    "Spellcharged Greatsword should clear charge and level after full decay");
        });
    }

    static void spellchargedGreatswordChargeEventRequiresMainhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.ARCANE_BLAST.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_mainhand_charge_test");
            var greatsword = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, greatsword);

            postSpellOnCast(player, spell, 1);
            var chargedMainhand = player.getMainHandItem();
            helper.assertTrue(SpellchargedGreatsword.getEffectiveChargeTicks(
                            chargedMainhand,
                            helper.getLevel().getGameTime()) > 0.0D,
                    "Spellcharged Greatsword should charge from spell casts while held in mainhand");

            var offhandOnly = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, offhandOnly);
            postSpellOnCast(player, spell, 1);
            helper.assertTrue(SpellchargedGreatsword.getEffectiveChargeTicks(
                            player.getOffhandItem(),
                            helper.getLevel().getGameTime()) == 0.0D,
                    "Spellcharged Greatsword should not charge from offhand-only casts");
        });
    }
    static void spellcastersFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Spellcasters Flask",
                item -> item.getClass() == SpellcastersFlask.class,
                expectedFlaskEnchantments()
        ));
    }

    private static void assertSpellchargedGreatswordChargeState(
            GameTestHelper helper,
            ItemStack stack,
            long gameTime,
            double expectedChargeTicks,
            int expectedChargeLevel,
            String message
    ) {
        var actualChargeTicks = SpellchargedGreatsword.getEffectiveChargeTicks(stack, gameTime);
        helper.assertTrue(Math.abs(actualChargeTicks - expectedChargeTicks) < 1.0e-9D,
                message + ": expected charge " + expectedChargeTicks + " but got " + actualChargeTicks);
        helper.assertTrue(SpellchargedGreatsword.getChargeLevel(stack) == expectedChargeLevel,
                message + ": expected level " + expectedChargeLevel + " but got "
                        + SpellchargedGreatsword.getChargeLevel(stack));
    }

    private static void assertSpellchargedGreatswordAttackAttributes(
            GameTestHelper helper,
            SpellchargedGreatsword item,
            ItemStack stack,
            double expectedDamageBonus,
            double expectedSpeedBonus,
            String message
    ) {
        var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
        assertModifierWithId(
                helper,
                modifiers.get(Attributes.ATTACK_DAMAGE),
                VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                AttributeModifier.Operation.ADDITION,
                SpellchargedGreatsword.DISPLAY_ATTACK_DAMAGE - 1.0D + expectedDamageBonus,
                message + " attack damage"
        );
        assertModifierWithId(
                helper,
                modifiers.get(Attributes.ATTACK_SPEED),
                VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                AttributeModifier.Operation.ADDITION,
                SpellchargedGreatsword.DISPLAY_ATTACK_SPEED - 4.0D + expectedSpeedBonus,
                message + " attack speed"
        );
    }
    static void alchemistsFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Alchemists Flask",
                item -> item.getClass() == AlchemistsFlask.class,
                expectedAlchemistsFlaskEnchantments()
        ));
    }
    static void apprenticeEnchantmentsKeepExpectedAcquisitionFlags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            // 1.21.1申し送り事項:
            // treasure / tradeable / discoverable は定義形式の変更で見落としやすい。
            // フラグだけ移したつもりでも司書取引や戦利品生成がズレるので、移植時は個別に再検証すること。
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.ALACRITY, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.REFLUX, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.RESERVOIR, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.SURGE, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.ATTUNEMENT, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.TENSE, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.WISDOM, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.PLUNDER, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.TRANSCENDENCE, true, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.GUZZLE, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.LARGE_MUG, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.RED_ENERGY, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.GLOW_ENERGY, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.SYNTHESIS, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.SHELL, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.SYNCHRONIZATION, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.NEUTRALIZATION, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.COMPRESS, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.RELEASE, false, false, true);
        });
    }
    static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            // 1.21.1申し送り事項:
            // 本棚由来の抽選可否は enchanting table 可否だけでは追えず、book/anvil 側の定義差分でも崩れる。
            // Flask 系除外は「今も本から引けないか」を seed 探索込みで見直し、そのまま移植前提にしない。
            var function = EnchantRandomlyFunction.randomApplicableEnchantment().build();
            var seenApprenticeEnchantments = new LinkedHashSet<ResourceLocation>();

            for (long seed = 0L; seed < 4096L; ++seed) {
                var result = function.apply(new ItemStack(Items.BOOK), createEmptyLootContext(helper, seed));
                var enchantments = EnchantmentHelper.getEnchantments(result);
                helper.assertTrue(result.is(Items.ENCHANTED_BOOK),
                        "Random applicable enchantment loot should convert books into enchanted books");
                helper.assertTrue(enchantments.size() == 1,
                        "Random applicable enchantment loot should apply exactly one enchantment: " + enchantments);

                for (var enchantment : enchantments.keySet()) {
                    var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                    if (enchantmentId == null || !ApprenticeCodex.MODID.equals(enchantmentId.getNamespace())) {
                        continue;
                    }

                    helper.assertFalse(ApprenticeEnchantmentAvailability.isExcludedFromRandomBookLoot(enchantment),
                            "Random applicable enchantment loot included excluded enchantment: " + enchantmentId + " at seed " + seed);
                    seenApprenticeEnchantments.add(enchantmentId);
                }
            }

            var expectedEnchantments = expectedRandomBookLootEnchantments();
            helper.assertTrue(seenApprenticeEnchantments.containsAll(expectedEnchantments),
                    "Random applicable enchantment loot lost apprentice enchantments: "
                            + describeEnchantmentDifference(expectedEnchantments, seenApprenticeEnchantments));
        });
    }
    static void magicArmorKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCategoryEnchantments(
                    helper,
                    "Enchantress Robe",
                    item -> item instanceof EnchantressRobeItem,
                    ApprenticeCodexGameTestScenarios::expectedEnchantressRobeEnchantments
            );
            assertCategoryEnchantments(
                    helper,
                    "Stealth Rune Armor",
                    item -> item instanceof StealthRuneArmorItem,
                    ApprenticeCodexGameTestScenarios::expectedStealthRuneArmorEnchantments
            );
            assertCategoryEnchantments(
                    helper,
                    "Chromatic Magia Dress",
                    item -> item instanceof ChromaticMagiaDressItem,
                    ApprenticeCodexGameTestScenarios::expectedChromaticMagiaDressEnchantments
            );
            assertCategoryEnchantments(
                    helper,
                    "Element Maiden Robe",
                    item -> item instanceof ElementMaidenRobeItem,
                    ApprenticeCodexGameTestScenarios::expectedElementMaidenRobeEnchantments
            );
            assertCategoryEnchantments(
                    helper,
                    "Magi Agent Suit",
                    item -> item instanceof MagiAgentSuitItem,
                    ApprenticeCodexGameTestScenarios::expectedMagiAgentSuitEnchantments
            );
        });
    }
    static void scrollcasterGauntletKeepsExpectedStatsAndBenchEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    Set.of(),
                    "Scrollcaster Gauntlet"
            );

            ScrollcasterGauntlet.setCalibrationScroll(
                    stack,
                    0,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get())
            );
            stack.enchant(EnchantmentRegistry.ALACRITY.get(), 1);
            stack.enchant(EnchantmentRegistry.REFLUX.get(), 1);
            stack.enchant(EnchantmentRegistry.RESERVOIR.get(), 1);
            stack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            stack.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);
            stack.enchant(EnchantmentRegistry.TENSE.get(), 1);

            var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
            var epicFightLoaded = ModList.get().isLoaded(EpicFightCompat.MOD_ID);
            var expectedAttackDamageBonus = epicFightLoaded ? 2.0D : 5.0D;
            var expectedAttackSpeedBonus = epicFightLoaded ? 0.0D : -2.2D;
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    expectedAttackDamageBonus,
                    "Scrollcaster Gauntlet attack damage modifier should match the loaded combat environment"
            );
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    expectedAttackSpeedBonus,
                    "Scrollcaster Gauntlet attack speed modifier should match the loaded combat environment"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.02D,
                    "Scrollcaster Gauntlet Alacrity modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.05D,
                    "Scrollcaster Gauntlet Reflux modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get()),
                    AttributeModifier.Operation.ADDITION,
                    20.0D,
                    "Scrollcaster Gauntlet Reservoir modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.07D,
                    "Scrollcaster Gauntlet base + Surge spell power modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.05D,
                    "Scrollcaster Gauntlet Tense modifier changed"
            );

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Scrollcaster Gauntlet test could not resolve the selected spell school");
            var attunementAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Scrollcaster Gauntlet test could not resolve the Attunement spell power attribute: " + imbuedSchool.getId());
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(attunementAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.04D,
                    "Scrollcaster Gauntlet Attunement modifier changed"
            );
        });
    }
    static void apprenticeMageRobeKeepsExpectedAttributeBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.apprenticeMageRobeSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                    ArmorItem.Type.CHESTPLATE, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                    ArmorItem.Type.LEGGINGS, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - 50.0D) < 1.0e-9D,
                        "Apprentice Mage Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Apprentice Mage Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == (armorType == ArmorItem.Type.CHESTPLATE),
                        "Apprentice Mage Robe " + armorType + " imbue surface regression");
            }
        });
    }
    static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var lightningSpellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.enchantressRobeSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_HAT.get(),
                    ArmorItem.Type.CHESTPLATE, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_ROBE.get(),
                    ArmorItem.Type.LEGGINGS, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - EnchantressRobeStats.MAX_MANA_BONUS_PER_PIECE) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Enchantress Robe " + armorType + " imbue surface regression: hasImbueSlot="
                                + item.hasImbueSlot() + " stack=" + stack);

                var lightningSpellPowerBonus = sumModifierAmount(
                        modifiers.get(lightningSpellPowerAttribute),
                        AttributeModifier.Operation.MULTIPLY_BASE
                );
                helper.assertTrue(Math.abs(lightningSpellPowerBonus) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " should not gain school spell power before imbue: "
                                + describeModifiers(modifiers));
            }
        });
    }
    static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_ROBE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, stack);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Enchantress Robe chestplate test could not resolve imbued school");
            var imbuedSpellPowerAttribute =
                    jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Enchantress Robe chestplate test could not resolve school spell power attribute");

            var modifiers = item.getAttributeModifiers(EquipmentSlot.CHEST, stack);
            var globalSpellPowerBonus = sumModifierAmount(
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            var expectedGlobalSpellPower = ApprenticeCodexServerConfig.enchantressRobeSpellPowerBonusPerPiece();
            helper.assertTrue(Math.abs(globalSpellPowerBonus - expectedGlobalSpellPower) < 1.0e-9D,
                    "Enchantress Robe chestplate should keep configured spell power after imbue: " + describeModifiers(modifiers));

            var imbuedSchoolSpellPowerBonus = sumModifierAmount(
                    modifiers.get(imbuedSpellPowerAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(imbuedSchoolSpellPowerBonus - 0.05D) < 1.0e-9D,
                    "Enchantress Robe chestplate should add +0.05 imbued school spell power: " + describeModifiers(modifiers));
        });
    }
    static void chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.chromaticMagiaDressSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                    ArmorItem.Type.CHESTPLATE, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                    ArmorItem.Type.LEGGINGS, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item.getMaterial().getDefenseForType(armorType) == ArmorMaterials.IRON.getDefenseForType(armorType),
                        "Chromatic Magia Dress " + armorType + " defense should match iron");
                helper.assertTrue(Math.abs(item.getMaterial().getToughness() - 1.0F) < 1.0e-6F,
                        "Chromatic Magia Dress " + armorType + " toughness should be 1");
                helper.assertTrue(item.getEnchantmentValue(stack) == ChromaticMagiaDressStats.enchantmentValue(),
                        "Chromatic Magia Dress " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                        ),
                        "Chromatic Magia Dress " + armorType + " should repair with mithril scrap");

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - 125.0D) < 1.0e-9D,
                        "Chromatic Magia Dress " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Chromatic Magia Dress " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Chromatic Magia Dress " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && (item.getDescriptionId() + ".desc").equals(contents.getKey())),
                        "Chromatic Magia Dress " + armorType + " should show its lang desc key");
            }
        });
    }
    static void elementMaidenRobeKeepsExpectedStatsImbueAndMagicEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.elementMaidenRobeSpellPowerBonus();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(),
                    ArmorItem.Type.CHESTPLATE, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                    ArmorItem.Type.LEGGINGS, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                        "Element Maiden Robe " + armorType + " should be a unique item");
                helper.assertTrue(stack.getRarity() == Rarity.EPIC,
                        "Element Maiden Robe " + armorType + " rarity should be epic");
                helper.assertTrue(item.getMaterial().getDefenseForType(armorType) == ArmorMaterials.LEATHER.getDefenseForType(armorType),
                        "Element Maiden Robe " + armorType + " defense should match leather");
                helper.assertTrue(Math.abs(item.getMaterial().getToughness() - 4.0F) < 1.0e-6F,
                        "Element Maiden Robe " + armorType + " toughness should be 4");
                helper.assertTrue(item.getEnchantmentValue(stack) == ElementMaidenRobeStats.enchantmentValue(),
                        "Element Maiden Robe " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                        ),
                        "Element Maiden Robe " + armorType + " should repair with mithril scrap");

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - ElementMaidenRobeStats.MAX_MANA_BONUS) < 1.0e-9D,
                        "Element Maiden Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Element Maiden Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Element Maiden Robe " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.element_maiden_robe.desc".equals(contents.getKey())),
                        "Element Maiden Robe " + armorType + " should show its common lang desc key");
            }

            var chestplate = (ElementMaidenRobeItem) ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get();
            var chestStack = new ItemStack(chestplate);
            chestplate.initializeSpellContainer(chestStack);
            var initialContainer = ISpellContainer.get(chestStack);
            helper.assertTrue(initialContainer != null
                            && initialContainer.getSpellAtIndex(0).getSpell() == SpellRegistry.DIVINE_POSSESSION.get(),
                    "Element Maiden Robe chestplate should initialize Divine Possession as its imbue spell");
            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, chestStack);
            chestStack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            chestStack.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);

            var imbuedSchool = MagicTools.getImbuedSpellSchool(chestStack);
            helper.assertTrue(imbuedSchool != null,
                    "Element Maiden Robe chestplate test could not resolve imbued school");
            var imbuedSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Element Maiden Robe chestplate test could not resolve school spell power attribute");

            var enchantedModifiers = chestplate.getAttributeModifiers(EquipmentSlot.CHEST, chestStack);
            var enchantedGlobalSpellPower = sumModifierAmount(
                    enchantedModifiers.get(spellPowerAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(enchantedGlobalSpellPower
                            - (expectedSpellPower + ElementMaidenRobeStats.SURGE_SPELL_POWER_PER_LEVEL)) < 1.0e-9D,
                    "Element Maiden Robe chestplate should add Surge spell power: " + describeModifiers(enchantedModifiers));

            var attunementSpellPower = sumModifierAmount(
                    enchantedModifiers.get(imbuedSpellPowerAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(attunementSpellPower
                            - ElementMaidenRobeStats.ATTUNEMENT_SPELL_POWER_PER_LEVEL) < 1.0e-9D,
                    "Element Maiden Robe chestplate should add Attunement school spell power: "
                            + describeModifiers(enchantedModifiers));
        });
    }

    static void magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var fireSpellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.magiAgentSuitSpellPowerBonus();
            var expectedSchoolSpellPower = ApprenticeCodexServerConfig.magiAgentSuitSchoolSpellPowerBonus();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_HOOD.get(),
                    ArmorItem.Type.CHESTPLATE, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_COAT.get(),
                    ArmorItem.Type.LEGGINGS, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item.getMaterial().getDefenseForType(armorType) == expectedMagiAgentSuitDefense(armorType),
                        "Magi Agent Suit " + armorType + " defense changed");
                helper.assertTrue(item.getEnchantmentValue(stack) == MagiAgentSuitStats.enchantmentValue(),
                        "Magi Agent Suit " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                        ),
                        "Magi Agent Suit " + armorType + " should repair with magic cloth");
                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Magi Agent Suit " + armorType + " imbue surface regression");

                var hintLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), hintLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(hintLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.magi_agent_suit.rune_hint".equals(contents.getKey())),
                        "Magi Agent Suit " + armorType + " should show its rune hint before calibration");

                MagiAgentSuitItem.setCalibrationAdjustment(
                        stack,
                        0,
                        new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get())
                );
                helper.assertTrue(MagiAgentSuitItem.getCalibrationAdjustment(stack, 0)
                                .is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                        "Magi Agent Suit " + armorType + " should store the calibration rune");

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - MagiAgentSuitStats.MAX_MANA_BONUS) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                var schoolSpellPowerBonus = sumModifierAmount(modifiers.get(fireSpellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(schoolSpellPowerBonus - expectedSchoolSpellPower) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " school rune spell power regression: " + describeModifiers(modifiers));

                var toughnessBonus = sumModifierAmount(modifiers.get(Attributes.ARMOR_TOUGHNESS), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(toughnessBonus - expectedMagiAgentSuitToughness(armorType)) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " toughness regression: " + describeModifiers(modifiers));

                var tunedLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tunedLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tunedLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.magi_agent_suit.school_rune".equals(contents.getKey())),
                        "Magi Agent Suit " + armorType + " should show its tuned school tooltip");
            }
        });
    }

    private static int expectedMagiAgentSuitDefense(ArmorItem.Type armorType) {
        return switch (armorType) {
            case HELMET, BOOTS -> 3;
            case CHESTPLATE, LEGGINGS -> 6;
        };
    }

    private static double expectedMagiAgentSuitToughness(ArmorItem.Type armorType) {
        return armorType == ArmorItem.Type.LEGGINGS ? 2.0D : 1.0D;
    }

    static void elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                // GameTest では SpellConfig 経由の school 解決が Evocation に寄ることがあるため、
                // 分配ルール自体は SchoolRegistry から直接検証する.
                var directBonuses = ElementMaidenRobeSchoolPowerBonusEvents.resolveSchoolPowerBonuses(10, Map.of(
                        SchoolRegistry.FIRE.get(), 4,
                        SchoolRegistry.ICE.get(), 3
                ), 0.20D);
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get(),
                        0.14D,
                        "Element Maiden Robe should distribute empty slots to the strongest spellbook school");
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_SPELL_POWER.get(),
                        0.06D,
                        "Element Maiden Robe should keep lower spellbook school share");

                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "element_maiden_robe_school_power_distribution_test");
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()));

                equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                        createElementMaidenRobeSchoolPowerSpellbook(helper,
                                io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()));

                helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElementMaidenRobeItem,
                        "Element Maiden Robe dynamic test player is not wearing the robe");
                var resolvedBonuses = ElementMaidenRobeSchoolPowerBonusEvents.resolveSchoolPowerBonuses(player, 0.20D);
                helper.assertTrue(!resolvedBonuses.isEmpty(),
                        "Element Maiden Robe dynamic test could not resolve spellbook schools from Curios slot");
                helper.assertTrue(Math.abs(ApprenticeCodexServerConfig.elementMaidenRobeSchoolSpellPowerBonus() - 0.20D) < 1.0e-9D,
                        "Element Maiden Robe dynamic test config override did not apply");
                var appliedBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(player);
                assertElementMaidenDynamicSchoolPowerBonuses(helper, player, appliedBonuses,
                        "Element Maiden Robe should apply Curios spellbook-derived school spell power");
            }
        });
    }
    static void elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                var directBonuses = ElementMaidenRobeSchoolPowerBonusEvents.resolveSchoolPowerBonuses(10, Map.of(
                        SchoolRegistry.FIRE.get(), 3,
                        SchoolRegistry.ICE.get(), 3,
                        SchoolRegistry.NATURE.get(), 1
                ), 0.20D);
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get(),
                        0.09D,
                        "Element Maiden Robe should split empty slots between tied strongest schools");
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_SPELL_POWER.get(),
                        0.09D,
                        "Element Maiden Robe should split empty slots between tied strongest schools");
                assertElementMaidenSchoolPowerBonusAmount(helper, directBonuses,
                        io.redspace.ironsspellbooks.api.registry.AttributeRegistry.NATURE_SPELL_POWER.get(),
                        0.02D,
                        "Element Maiden Robe should floor smaller spellbook school shares to 1% units");
            }
        });
    }
    static void elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var fire = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "element_maiden_robe_school_power_ignore_hand_test");
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
            player.setItemInHand(InteractionHand.MAIN_HAND, createElementMaidenRobeSchoolPowerSpellbook(helper, fire));

            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                ElementMaidenRobeSchoolPowerBonusEvents.refresh(player);
                assertNoElementMaidenDynamicSchoolPower(helper, player,
                        "Element Maiden Robe should ignore spell containers outside the Curios spellbook slot");
            }

            equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                    createElementMaidenRobeSchoolPowerSpellbook(helper, fire));
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.0D)) {
                ElementMaidenRobeSchoolPowerBonusEvents.refresh(player);
                assertNoElementMaidenDynamicSchoolPower(helper, player,
                        "Element Maiden Robe school spell power config 0 should disable the dynamic bonus");
            }
        });
    }
    static void elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(0.20D)) {
                var fire = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
                var ice = SpellRegistry.FROST_RUNE.get();

                var archivistsPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "element_maiden_robe_archivists_source_test");
                archivistsPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
                var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
                ArchivistsGrimoire.setUpgradeCount(grimoireStack, 2);
                var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
                inventory.setStackInSlot(0, createSpellScroll(fire));
                inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT, createSpellScroll(ice));
                ArchivistsGrimoire.setSelectedRow(grimoireStack, 0);
                equipCurio(archivistsPlayer, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT, grimoireStack);

                var firstArchivistsBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(archivistsPlayer);
                helper.assertTrue(!firstArchivistsBonuses.isEmpty(),
                        "Element Maiden Robe should resolve the selected Archivists Grimoire page");
                assertElementMaidenDynamicSchoolPowerBonuses(helper, archivistsPlayer, firstArchivistsBonuses,
                        "Element Maiden Robe should read the selected Archivists Grimoire page");
                ArchivistsGrimoire.setSelectedRow(grimoireStack, 1);
                var secondArchivistsBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(archivistsPlayer);
                helper.assertTrue(!secondArchivistsBonuses.isEmpty(),
                        "Element Maiden Robe should resolve the new Archivists Grimoire page");
                assertElementMaidenDynamicSchoolPowerBonuses(helper, archivistsPlayer, secondArchivistsBonuses,
                        "Element Maiden Robe should apply the new Archivists Grimoire page bonus");

                var enderPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0),
                        "element_maiden_robe_ender_source_test");
                enderPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()));
                var enderData = Capabilities.getEnderGrimoireSpellbookOrNull(enderPlayer);
                helper.assertTrue(enderData != null, "Ender Grimoire school spell power test is missing player capability");
                var mutable = ISpellContainer.create(15, true, true).mutableCopy();
                helper.assertTrue(mutable.addSpellAtIndex(fire, 1, 0, false),
                        "Failed to prepare Ender Grimoire fire spell");
                enderData.setSpellContainer(mutable.toImmutable());
                equipCurio(enderPlayer, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                        new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get()));

                var enderBonuses = ElementMaidenRobeSchoolPowerBonusEvents.refresh(enderPlayer);
                helper.assertTrue(!enderBonuses.isEmpty(),
                        "Element Maiden Robe should resolve Ender Grimoire spells from the player capability");
                assertElementMaidenDynamicSchoolPowerBonuses(helper, enderPlayer, enderBonuses,
                        "Element Maiden Robe should read Ender Grimoire spells from the player capability");
            }
        });
    }
    static void stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.stealthRuneArmorSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                    ArmorItem.Type.CHESTPLATE, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                    ArmorItem.Type.LEGGINGS, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                    ArmorItem.Type.BOOTS, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - 50.0D) < 1.0e-9D,
                        "Stealth Rune Armor " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Stealth Rune Armor " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Stealth Rune Armor " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && ("item." + ApprenticeCodex.MODID + ".stealth_rune_armor.desc").equals(contents.getKey())),
                        "Stealth Rune Armor " + armorType + " should show its lang desc key");
            }
        });
    }
    static void chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "chromatic_magia_dress_history_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Chromatic Magia Dress test could not resolve player mana data");

            var helmet = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get());
            var chestplate = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get());
            var leggings = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
            var boots = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get());
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            player.setItemSlot(EquipmentSlot.LEGS, leggings);
            player.setItemSlot(EquipmentSlot.FEET, boots);
            var schoolSpellPowerBonusPerHistory =
                    ApprenticeCodexServerConfig.chromaticMagiaDressSchoolSpellPowerBonusPerHistory();

            var longSpell = SpellRegistry.COMPOUND_PHIAL.get();
            for (int i = 0; i < 21; ++i) {
                postSpellOnCast(player, longSpell, 1);
            }
            assertSchoolSpellPowerBonus(helper, helmet, EquipmentSlot.HEAD, longSpell,
                    20.0D * schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress helmet should keep only the latest 20 LONG histories");
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, longSpell, 0.0D,
                    "Chromatic Magia Dress chestplate should ignore non-recast LONG spells");

            var continuousSpell = SpellRegistry.FORCE_FIELD.get();
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, leggings, EquipmentSlot.LEGS, continuousSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record CONTINUOUS spells");

            var heldContinuousLeggings = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
            player.setItemSlot(EquipmentSlot.LEGS, heldContinuousLeggings);
            magicData.initiateCast(continuousSpell, 1, 60, CastSource.SPELLBOOK, "gametest");
            postSpellOnCast(player, continuousSpell, 1);
            postSpellOnCast(player, continuousSpell, 1);
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, heldContinuousLeggings, EquipmentSlot.LEGS, continuousSpell,
                    schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record only once during one CONTINUOUS hold");

            magicData.resetCastingState();
            ChromaticMagiaDressCastEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            magicData.initiateCast(continuousSpell, 1, 60, CastSource.SPELLBOOK, "gametest");
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, heldContinuousLeggings, EquipmentSlot.LEGS, continuousSpell,
                    2.0D * schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record a new CONTINUOUS hold after the previous cast ends");
            magicData.resetCastingState();
            ChromaticMagiaDressCastEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));

            var instantSpell = SpellRegistry.MANA_SLASH.get();
            postSpellOnCast(player, instantSpell, 1);
            assertSchoolSpellPowerBonus(helper, boots, EquipmentSlot.FEET, instantSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress boots should record INSTANT spells");

            var recastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
            postSpellOnCast(player, recastSpell, 1);
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress chestplate should record initial recast-capable casts");

            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    recastSpell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SPELLBOOK,
                    null
            ), magicData);
            postSpellOnCast(player, recastSpell, 1);
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress chestplate should ignore casts while the same spell is in Recast");
        });
    }
    static void pastelStaffKeepsItsLocalEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pastelStack = new ItemStack(ItemRegistry.PASTEL_STAFF.get());
            var multicastStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());

            assertStaffKeepsExpectedEnchantingRules(helper, pastelStack, "Pastel Staff");
            assertStaffKeepsExpectedEnchantingRules(helper, multicastStack, "Multicast Echo Staff");
            assertEnchantingSurfacesMatch(helper, pastelStack, multicastStack, "Pastel Staff", "Multicast Echo Staff");
        });
    }

    private static Set<ResourceLocation> expectedSpellchargedGreatswordEnchantments(ItemStack stack) {
        var expectedEnchantments = new LinkedHashSet<>(collectAllowedEnchantments(
                new ItemStack(Items.DIAMOND_SWORD),
                enchantment -> enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD))
        ));
        expectedEnchantments.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }
}
