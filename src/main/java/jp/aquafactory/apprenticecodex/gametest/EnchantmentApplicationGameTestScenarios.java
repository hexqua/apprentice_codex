package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.armor.*;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.MALUM_ANIMATED;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.MALUM_HAUNTED;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.MALUM_REPLENISHING;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.MALUM_SPIRIT_PLUNDER;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.addExpectedMalumHauntedIfPresent;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.addExpectedMalumReplenishingIfPresent;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.addExpectedMalumSpiritPlunderIfPresent;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.assertCategoryEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.assertArmorCategoryEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.assertExactEnchantmentSurfaces;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.collectAllowedEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.createEnchantedBook;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.getRegisteredEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.isDurabilityTargetEnchantment;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.registryIdSet;

final class EnchantmentApplicationGameTestScenarios {
    private static final ResourceLocation CREATE_POTATO_RECOVERY =
            ResourceLocation.fromNamespaceAndPath("create", "potato_recovery");
    private static final Map<AttributeEnchantmentType, TagKey<Item>> ATTRIBUTE_ENCHANTABLE_TAGS = Map.of(
            AttributeEnchantmentType.ALACRITY, TagRegistry.Items.ALACRITY_ENCHANTABLE,
            AttributeEnchantmentType.REFLUX, TagRegistry.Items.REFLUX_ENCHANTABLE,
            AttributeEnchantmentType.RESERVOIR, TagRegistry.Items.RESERVOIR_ENCHANTABLE,
            AttributeEnchantmentType.SURGE, TagRegistry.Items.SURGE_ENCHANTABLE,
            AttributeEnchantmentType.ATTUNEMENT, TagRegistry.Items.ATTUNEMENT_ENCHANTABLE,
            AttributeEnchantmentType.TENSE, TagRegistry.Items.TENSE_ENCHANTABLE
    );

    private EnchantmentApplicationGameTestScenarios() {
    }

    static void itemSurfacesKeepExpectedMatrix(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCategoryEnchantments(helper, "Spell Gun",
                    item -> item instanceof AbstractSpellGunItem,
                    EnchantmentApplicationGameTestScenarios::expectedSpellGunEnchantments);
            assertCategoryEnchantments(helper, "Swingcast Staff",
                    item -> item instanceof AbstractSwingcastStaffItem,
                    EnchantmentApplicationGameTestScenarios::expectedSwingcastStaffEnchantments);
            var soulstainedStaff = new ItemStack(ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get());
            assertExactEnchantmentSurfaces(
                    helper,
                    soulstainedStaff,
                    expectedSwingcastStaffEnchantments(soulstainedStaff),
                    "Soulstained Steel Swingcast Staff"
            );
            assertCategoryEnchantments(helper, "Spellcasters Flask",
                    item -> item.getClass() == SpellcastersFlask.class,
                    stack -> expectedFlaskEnchantments());
            assertCategoryEnchantments(helper, "Alchemists Flask",
                    item -> item.getClass() == AlchemistsFlask.class,
                    stack -> expectedAlchemistsFlaskEnchantments());
            assertArmorCategoryEnchantments(helper, "Enchantress Robe",
                    item -> item instanceof EnchantressRobeItem,
                    EnchantmentApplicationGameTestScenarios::expectedEnchantressRobeEnchantments,
                    EnchantmentApplicationGameTestScenarios::expectedArmorAnvilEnchantments);
            assertArmorCategoryEnchantments(helper, "Soulcollector Robe",
                    item -> item instanceof SoulcollectorRobeItem,
                    EnchantmentApplicationGameTestScenarios::expectedSoulcollectorRobeEnchantments,
                    EnchantmentApplicationGameTestScenarios::expectedArmorAnvilEnchantments);
            assertArmorCategoryEnchantments(helper, "Stealth Rune Armor",
                    item -> item instanceof StealthRuneArmorItem,
                    EnchantmentApplicationGameTestScenarios::expectedStealthRuneArmorEnchantments,
                    EnchantmentApplicationGameTestScenarios::expectedArmorAnvilEnchantments);
            assertArmorCategoryEnchantments(helper, "Chromatic Magia Dress",
                    item -> item instanceof ChromaticMagiaDressItem,
                    EnchantmentApplicationGameTestScenarios::expectedChromaticMagiaDressEnchantments,
                    EnchantmentApplicationGameTestScenarios::expectedArmorAnvilEnchantments);
            assertArmorCategoryEnchantments(helper, "Element Maiden Robe",
                    item -> item instanceof ElementMaidenRobeItem,
                    EnchantmentApplicationGameTestScenarios::expectedElementMaidenRobeEnchantments,
                    EnchantmentApplicationGameTestScenarios::expectedArmorAnvilEnchantments);
            assertArmorCategoryEnchantments(helper, "Magi Agent Suit",
                    item -> item instanceof MagiAgentSuitItem,
                    EnchantmentApplicationGameTestScenarios::expectedMagiAgentSuitEnchantments,
                    EnchantmentApplicationGameTestScenarios::expectedArmorAnvilEnchantments);

            assertExactEnchantmentSurfaces(helper,
                    new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get()),
                    expectedReflectcastShieldEnchantments(), "Reflectcast Shield");
            assertExactEnchantmentSurfaces(helper,
                    new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get()),
                    expectedSpellchargedGreatswordEnchantments(new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get())),
                    "Spellcharged Greatsword");
            assertExactEnchantmentSurfaces(helper,
                    new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                    expectedChargedTwinBladeStaffEnchantments(new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get())),
                    "Charged Twin Blade Staff");
            assertExactEnchantmentSurfaces(helper,
                    new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get()),
                    expectedManaForceBladeEnchantments(new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get())),
                    "Mana Force Blade");
            assertExactEnchantmentSurfaces(helper,
                    new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get()),
                    expectedSpellSideEdgeEnchantments(new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get())),
                    "Spell Side Edge");
            assertCircuitHeatStaffSurfaces(helper);
            assertExactEnchantmentSurfaces(helper,
                    new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get()),
                    expectedMultipurposeStaffrifleEnchantments(new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get())),
                    "Multipurpose Staffrifle");
            assertExactEnchantmentSurfaces(helper,
                    new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get()),
                    expectedScrollcasterGauntletEnchantments(new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get())),
                    "Scrollcaster Gauntlet");
            assertExactEnchantmentSurfaces(helper,
                    new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()),
                    registryIdSet(EnchantmentRegistry.SHELL, EnchantmentRegistry.SYNCHRONIZATION,
                            EnchantmentRegistry.NEUTRALIZATION),
                    "Mana Shield Charm");
            assertMithrilFreecastStaffSurfaces(helper);
            assertRevolvercastStaffSurfaces(helper);
            assertCrystalBladedStaffSurfaces(helper);
            assertIlluminateStellarStaffSurfaces(helper);
            assertUniteLunaStaffSurfaces(helper);
            assertSmashcastScepterSurfaces(helper);
            assertElementalBowSurfaces(helper);
            assertOffhandSurfaces(helper);
        });
    }

    static void directApplicationPoliciesKeepExpectedMatrix(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertEnchantableTagIds(helper);
            assertWisdomPlunderPoliciesAndTags(helper);
            assertAttributePoliciesAndTags(helper);
            assertTranscendencePoliciesAndTag(helper);
        });
    }

    static void specialApplicationRulesStayExplicit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertFocusStaffbowRules(helper);
            assertMagiCompressorRules(helper);
            assertBulwarkAndParrycastRules(helper);
            assertElementalBowSynthesisRules(helper);
            assertLocalStaffRules(helper);
            assertReplenishingRules(helper);
        });
    }

    static void acquisitionFlagsKeepExpectedValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertAcquisitionFlags(helper, EnchantmentRegistry.ALACRITY, false, true, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.REFLUX, false, true, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.RESERVOIR, false, true, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.SURGE, false, true, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.ATTUNEMENT, false, true, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.TENSE, false, true, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.WISDOM, false, true, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.PLUNDER, false, true, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.TRANSCENDENCE, true, true, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.GUZZLE, false, false, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.LARGE_MUG, false, false, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.RED_ENERGY, false, false, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.GLOW_ENERGY, false, false, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.SYNTHESIS, false, false, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.SHELL, false, false, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.SYNCHRONIZATION, false, false, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.NEUTRALIZATION, false, false, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.COMPRESS, false, false, true);
            assertAcquisitionFlags(helper, EnchantmentRegistry.RELEASE, false, false, true);
        });
    }

    private static void assertMithrilFreecastStaffSurfaces(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
        assertExactEnchantmentSurfaces(helper, stack,
                expectedMithrilFreecastStaffEnchantments(stack), "Mithril Freecast Staff");
    }

    private static void assertRevolvercastStaffSurfaces(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        assertExactEnchantmentSurfaces(helper, stack,
                expectedRevolvercastStaffEnchantments(stack), "Revolvercast Staff");
    }

    private static void assertCrystalBladedStaffSurfaces(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
        assertExactEnchantmentSurfaces(helper, stack,
                expectedCrystalBladedStaffEnchantments(stack), "Crystal Bladed Staff");
    }

    private static void assertIlluminateStellarStaffSurfaces(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get());
        assertExactEnchantmentSurfaces(helper, stack,
                expectedIlluminateStellarStaffEnchantments(stack), "Illuminate Stellar Staff");
    }

    private static void assertUniteLunaStaffSurfaces(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.UNITE_LUNA_STAFF.get());
        assertExactEnchantmentSurfaces(helper, stack,
                expectedUniteLunaStaffEnchantments(stack), "Unite Luna Staff");
    }

    private static void assertCircuitHeatStaffSurfaces(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        var expected = expectedCircuitHeatStaffEnchantments(stack);
        var lootingId = ForgeRegistries.ENCHANTMENTS.getKey(Enchantments.MOB_LOOTING);
        var plunderId = EnchantmentRegistry.PLUNDER.getId();
        helper.assertTrue(lootingId != null && expected.contains(lootingId),
                "Circuit Heat Staff should keep vanilla Looting in its expected application surface");
        helper.assertTrue(plunderId != null && !expected.contains(plunderId),
                "Circuit Heat Staff should keep Plunder outside its expected application surface");
        assertExactEnchantmentSurfaces(helper, stack, expected, "Circuit Heat Staff");
    }

    private static void assertSmashcastScepterSurfaces(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get());
        assertExactEnchantmentSurfaces(helper, stack,
                expectedSmashcastScepterEnchantingTableEnchantments(stack),
                expectedSmashcastScepterBookEnchantments(stack),
                expectedSmashcastScepterBookEnchantments(stack),
                "Smashcast Scepter");
    }

    private static void assertElementalBowSurfaces(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        var table = expectedElementalBowEnchantments();
        var anvil = new LinkedHashSet<>(table);
        anvil.remove(CREATE_POTATO_RECOVERY);
        assertExactEnchantmentSurfaces(helper, stack, table, expectedElementalBowBookEnchantments(), anvil,
                "Elemental Bow");
        var potatoRecovery = ForgeRegistries.ENCHANTMENTS.getValue(CREATE_POTATO_RECOVERY);
        if (potatoRecovery != null) {
            helper.assertFalse(potatoRecovery.canEnchant(stack),
                    "Create Potato Recovery should reject Elemental Bow at the anvil");
            assertElementalBowMixedBookAnvilApplication(helper, potatoRecovery);
        }
    }

    private static void assertElementalBowMixedBookAnvilApplication(
            GameTestHelper helper, Enchantment potatoRecovery) {
        var mixedBook = createEnchantedBook(Enchantments.POWER_ARROWS);
        EnchantedBookItem.addEnchantment(mixedBook, new EnchantmentInstance(potatoRecovery, 1));
        var player = new FakePlayer(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "elemental_bow_mixed_enchantment_book_test"));
        var menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()));
        menu.getSlot(1).set(mixedBook);
        menu.createResult();

        var result = menu.getSlot(2).getItem();
        helper.assertFalse(result.isEmpty(),
                "Elemental Bow should accept applicable enchantments from a mixed enchanted book");
        helper.assertTrue(result.getEnchantmentLevel(Enchantments.POWER_ARROWS) == 1,
                "Elemental Bow should retain Power from a mixed enchanted book");
        helper.assertTrue(result.getEnchantmentLevel(potatoRecovery) == 0,
                "Elemental Bow should discard Create Potato Recovery from a mixed enchanted book");
    }

    private static void assertOffhandSurfaces(GameTestHelper helper) {
        var allEnchantments = collectAllowedEnchantments(enchantment -> true);
        var offhandStacks = ItemRegistry.ITEMS.getEntries().stream()
                .map(RegistryObject::get)
                .filter(item -> item instanceof AbstractOffhandMagicItem)
                .map(ItemStack::new)
                .toList();
        helper.assertFalse(offhandStacks.isEmpty(), "No items matched enchantment test category: Offhand Magic Item");
        for (var stack : offhandStacks) {
            var expected = expectedOffhandEnchantments();
            assertExactEnchantmentSurfaces(helper, stack, expected, allEnchantments, expected,
                    "Offhand Magic Item " + ForgeRegistries.ITEMS.getKey(stack.getItem()));
        }

        var circlet = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
        var expectedCirclet = expectedEnchantedCircletEnchantments();
        assertExactEnchantmentSurfaces(helper, circlet, expectedCirclet, allEnchantments, expectedCirclet,
                "Enchanted Circlet");
    }

    private static void assertWisdomPlunderPoliciesAndTags(GameTestHelper helper) {
        var wisdom = EnchantmentRegistry.WISDOM.get();
        var plunder = EnchantmentRegistry.PLUNDER.get();
        var mismatches = new ArrayList<String>();
        for (var entry : ItemRegistry.ITEMS.getEntries()) {
            var item = entry.get();
            var stack = new ItemStack(item);
            verifyPolicySurface(mismatches, entry.getId() + " Wisdom",
                    WisdomPolicy.supportsDirectApplication(item),
                    item.canApplyAtEnchantingTable(stack, wisdom),
                    stack.is(TagRegistry.Items.WISDOM_ENCHANTABLE));
            verifyPolicySurface(mismatches, entry.getId() + " Plunder",
                    item instanceof PlunderTarget,
                    item.canApplyAtEnchantingTable(stack, plunder),
                    stack.is(TagRegistry.Items.PLUNDER_ENCHANTABLE));
        }
        helper.assertTrue(mismatches.isEmpty(),
                "Wisdom/Plunder policy surface mismatches: " + String.join(", ", mismatches));
    }

    private static void assertAttributePoliciesAndTags(GameTestHelper helper) {
        var all = AttributeEnchantmentPolicy.ALL_ATTRIBUTE_ENCHANTMENTS;
        var cases = List.of(
                new AttributePolicyCase(ItemRegistry.COPPER_SPELL_AMPLIFIER.get(), all),
                new AttributePolicyCase(ItemRegistry.ENCHANTED_CIRCLET.get(), all),
                new AttributePolicyCase(ItemRegistry.IRON_SPELLCASTER_GUN.get(), all),
                new AttributePolicyCase(ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get(),
                        Set.of(AttributeEnchantmentType.ALACRITY, AttributeEnchantmentType.REFLUX,
                                AttributeEnchantmentType.RESERVOIR, AttributeEnchantmentType.TENSE)),
                new AttributePolicyCase(ItemRegistry.SCROLLCASTER_GAUNTLET.get(),
                        Set.of(AttributeEnchantmentType.ALACRITY, AttributeEnchantmentType.REFLUX,
                                AttributeEnchantmentType.RESERVOIR, AttributeEnchantmentType.TENSE)),
                new AttributePolicyCase(ItemRegistry.MANA_FORCE_BLADE.get(),
                        Set.of(AttributeEnchantmentType.SURGE, AttributeEnchantmentType.ATTUNEMENT)),
                new AttributePolicyCase(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(),
                        Set.of(AttributeEnchantmentType.ALACRITY, AttributeEnchantmentType.REFLUX,
                                AttributeEnchantmentType.RESERVOIR, AttributeEnchantmentType.SURGE,
                                AttributeEnchantmentType.TENSE)),
                new AttributePolicyCase(ItemRegistry.PARRYCAST_BUCKLER.get(),
                        Set.of(AttributeEnchantmentType.ALACRITY, AttributeEnchantmentType.TENSE)),
                new AttributePolicyCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                        Set.of(AttributeEnchantmentType.SURGE, AttributeEnchantmentType.ATTUNEMENT)),
                new AttributePolicyCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(), Set.of()),
                new AttributePolicyCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(), Set.of()),
                new AttributePolicyCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get(), Set.of())
        );
        for (var testCase : cases) {
            helper.assertTrue(testCase.item() instanceof AttributeEnchantmentPolicy,
                    testCase.item().getDescriptionId() + " should participate in the attribute enchantment policy");
            var policy = (AttributeEnchantmentPolicy) testCase.item();
            helper.assertTrue(policy.directlyApplicableAttributeEnchantments().equals(testCase.directlyApplicable()),
                    testCase.item().getDescriptionId() + " direct attribute enchantment policy changed");
            for (var type : AttributeEnchantmentType.values()) {
                var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(type.enchantmentId());
                helper.assertTrue(enchantment != null, "Attribute enchantment was not registered: " + type.enchantmentId());
                helper.assertTrue(enchantment.canEnchant(new ItemStack(testCase.item()))
                                == testCase.directlyApplicable().contains(type),
                        testCase.item().getDescriptionId() + " category result changed for " + type);
            }
        }

        var mismatches = new ArrayList<String>();
        for (var entry : ItemRegistry.ITEMS.getEntries()) {
            var item = entry.get();
            var stack = new ItemStack(item);
            for (var type : AttributeEnchantmentType.values()) {
                var expected = AttributeEnchantmentPolicy.supportsDirectApplication(item, type);
                if (expected != stack.is(ATTRIBUTE_ENCHANTABLE_TAGS.get(type))) {
                    mismatches.add(entry.getId() + " " + type);
                }
            }
        }
        helper.assertTrue(mismatches.isEmpty(), "Attribute enchantment tags differ from policy: " + mismatches);
    }

    private static void assertTranscendencePoliciesAndTag(GameTestHelper helper) {
        var transcendence = EnchantmentRegistry.TRANSCENDENCE.get();
        var mithril = ItemRegistry.MITHRIL_FREECAST_STAFF.get();
        var revolver = ItemRegistry.REVOLVERCAST_STAFF.get();
        var gauntlet = ItemRegistry.SCROLLCASTER_GAUNTLET.get();
        var elementalBow = ItemRegistry.ELEMENTAL_BOW.get();
        helper.assertFalse(mithril.canApplyAtEnchantingTable(new ItemStack(mithril), transcendence),
                "Mithril Freecast Staff should reject Transcendence");
        helper.assertTrue(((TranscendencePolicy) mithril).transcendenceHandling()
                        == TranscendencePolicy.Handling.DISABLED,
                "Mithril Freecast Staff should keep Transcendence disabled");
        helper.assertTrue(revolver.canApplyAtEnchantingTable(new ItemStack(revolver), transcendence),
                "Revolvercast Staff should accept Transcendence like Swingcast Staffs");
        helper.assertTrue(ItemRegistry.MANA_FORCE_BLADE.get().canApplyAtEnchantingTable(
                        new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get()), transcendence),
                "Mana Force Blade should accept Transcendence");
        helper.assertTrue(((TranscendencePolicy) gauntlet).supportsDirectTranscendenceApplication(),
                "Scrollcaster Gauntlet should accept Transcendence through normal enchanting");
        helper.assertTrue(((TranscendencePolicy) elementalBow).transcendenceHandling()
                        == TranscendencePolicy.Handling.INTERNAL,
                "Elemental Bow should keep internal Transcendence handling");

        var mismatches = new ArrayList<String>();
        for (var entry : ItemRegistry.ITEMS.getEntries()) {
            var expected = TranscendencePolicy.supportsDirectApplication(entry.get());
            var actual = new ItemStack(entry.get()).is(TagRegistry.Items.TRANSCENDENCE_ENCHANTABLE);
            if (expected != actual) {
                mismatches.add(entry.getId() + " expected=" + expected + " actual=" + actual);
            }
        }
        helper.assertTrue(mismatches.isEmpty(),
                "Transcendence direct-application tag differs from policy: " + mismatches);
    }

    private static void assertEnchantableTagIds(GameTestHelper helper) {
        for (var entry : ATTRIBUTE_ENCHANTABLE_TAGS.entrySet()) {
            assertEnchantableTagId(helper, entry.getValue(), entry.getKey().enchantmentId());
        }
        assertEnchantableTagId(helper, TagRegistry.Items.TRANSCENDENCE_ENCHANTABLE,
                EnchantmentRegistry.TRANSCENDENCE.getId());
        assertEnchantableTagId(helper, TagRegistry.Items.WISDOM_ENCHANTABLE,
                EnchantmentRegistry.WISDOM.getId());
        assertEnchantableTagId(helper, TagRegistry.Items.PLUNDER_ENCHANTABLE,
                EnchantmentRegistry.PLUNDER.getId());
    }

    private static void assertEnchantableTagId(
            GameTestHelper helper,
            TagKey<Item> tag,
            ResourceLocation enchantmentId
    ) {
        var expected = ResourceLocation.fromNamespaceAndPath(
                enchantmentId.getNamespace(),
                enchantmentId.getPath() + "_enchantable"
        );
        helper.assertTrue(tag.location().equals(expected),
                "Enchantable tag id should match the 1.21.1 contract: expected="
                        + expected + " actual=" + tag.location());
    }

    private static void assertFocusStaffbowRules(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var item = (FocusStaffbow) stack.getItem();
        assertRule(helper, stack, item, EnchantmentRegistry.SYNTHESIS.get(), true, "Synthesis");
        assertRule(helper, stack, item, Enchantments.INFINITY_ARROWS, false, "Infinity");
        assertRule(helper, stack, item, EnchantmentRegistry.TRANSCENDENCE.get(), false, "Transcendence");
        if (ModList.get().isLoaded(MALUM_MOD_ID)) {
            var haunted = ForgeRegistries.ENCHANTMENTS.getValue(
                    EnchantmentApplicationGameTestSupport.MALUM_HAUNTED);
            var animated = ForgeRegistries.ENCHANTMENTS.getValue(MALUM_ANIMATED);
            helper.assertTrue(haunted != null && animated != null, "Malum enchantments are not registered");
            assertRule(helper, stack, item, haunted, true, "malum:haunted");
            assertRule(helper, stack, item, animated, false, "malum:animated");
            var replenishing = ForgeRegistries.ENCHANTMENTS.getValue(MALUM_REPLENISHING);
            helper.assertTrue(replenishing != null, "Malum Replenishing is not registered");
            assertRule(helper, stack, item, replenishing, true, "malum:replenishing");
        }
    }

    private static void assertRule(
            GameTestHelper helper,
            ItemStack stack,
            FocusStaffbow item,
            Enchantment enchantment,
            boolean expected,
            String name
    ) {
        helper.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack, enchantment) == expected,
                "Focus Staffbow enchanting-table rule changed for " + name);
        helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(enchantment)) == expected,
                "Focus Staffbow enchanted-book rule changed for " + name);
        helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, enchantment) == expected,
                "Focus Staffbow anvil rule changed for " + name);
    }

    private static void assertMagiCompressorRules(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get());
        helper.assertFalse(stack.isEnchantable(), "Magi-Compressor Gadget should not be enchantable");
        helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, Enchantments.UNBREAKING),
                "Magi-Compressor Gadget should reject vanilla enchanting table enchantments");
        helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(Enchantments.UNBREAKING)),
                "Magi-Compressor Gadget should reject enchanted books");
        var capacity = ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.fromNamespaceAndPath("create", "capacity"));
        if (capacity != null) {
            helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, capacity),
                    "Magi-Compressor Gadget should reject Create Capacity");
        }
    }

    private static void assertBulwarkAndParrycastRules(GameTestHelper helper) {
        var bulwark = ItemRegistry.BULWARK_GREATSHIELD.get();
        var bulwarkStack = new ItemStack(bulwark);
        for (var enchantment : List.of(Enchantments.UNBREAKING, EnchantmentRegistry.RESERVOIR.get(),
                EnchantmentRegistry.REFLUX.get(), EnchantmentRegistry.TRANSCENDENCE.get(),
                EnchantmentRegistry.WISDOM.get())) {
            helper.assertTrue(bulwark.canApplyAtEnchantingTable(bulwarkStack, enchantment),
                    "Bulwark Greatshield Buckler should accept " + ForgeRegistries.ENCHANTMENTS.getKey(enchantment));
        }

        var parrycast = ItemRegistry.PARRYCAST_BUCKLER.get();
        var parrycastStack = new ItemStack(parrycast);
        for (var enchantment : List.of(Enchantments.UNBREAKING, EnchantmentRegistry.TENSE.get(),
                EnchantmentRegistry.ALACRITY.get(), EnchantmentRegistry.TRANSCENDENCE.get(),
                EnchantmentRegistry.WISDOM.get())) {
            helper.assertTrue(parrycast.canApplyAtEnchantingTable(parrycastStack, enchantment),
                    "Parrycast Buckler should accept " + ForgeRegistries.ENCHANTMENTS.getKey(enchantment));
        }
    }

    private static void assertElementalBowSynthesisRules(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        helper.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack, EnchantmentRegistry.SYNTHESIS.get()),
                "Elemental Bow should accept Synthesis at the enchanting table");
        helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(EnchantmentRegistry.SYNTHESIS.get())),
                "Elemental Bow should accept Synthesis from enchanted books");
        helper.assertFalse(EnchantmentRegistry.SYNTHESIS.get().isCompatibleWith(Enchantments.INFINITY_ARROWS),
                "Synthesis should be incompatible with Infinity");
        helper.assertFalse(EnchantmentRegistry.SYNTHESIS.get().isCompatibleWith(Enchantments.MENDING),
                "Synthesis should be incompatible with Mending");
    }

    private static void assertLocalStaffRules(GameTestHelper helper) {
        var pastel = new ItemStack(ItemRegistry.PASTEL_STAFF.get());
        var multicast = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var zenith = new ItemStack(ItemRegistry.ZENITH_STAFF.get());
        var circuitHeat = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        assertLocalStaffRules(helper, pastel, "Pastel Staff");
        assertLocalStaffRules(helper, multicast, "Multicast Echo Staff");
        assertLocalStaffRules(helper, zenith, "Zenith Staff");
        assertLocalStaffRules(helper, circuitHeat, "Circuit Heat Staff");

        var pastelTable = collectAllowedEnchantments(enchantment ->
                pastel.getItem().canApplyAtEnchantingTable(pastel, enchantment));
        var pastelBooks = collectAllowedEnchantments(enchantment ->
                pastel.getItem().isBookEnchantable(pastel, createEnchantedBook(enchantment)));
        for (var entry : List.of(
                Map.entry("Multicast Echo Staff", multicast),
                Map.entry("Zenith Staff", zenith),
                Map.entry("Circuit Heat Staff", circuitHeat)
        )) {
            var stack = entry.getValue();
            var table = collectAllowedEnchantments(enchantment ->
                    stack.getItem().canApplyAtEnchantingTable(stack, enchantment));
            helper.assertTrue(pastelTable.equals(table),
                    entry.getKey() + " enchanting-table surface should match Pastel Staff");

            var books = collectAllowedEnchantments(enchantment ->
                    stack.getItem().isBookEnchantable(stack, createEnchantedBook(enchantment)));
            helper.assertTrue(pastelBooks.equals(books),
                    entry.getKey() + " book surface should match Pastel Staff");
        }
    }

    private static void assertLocalStaffRules(GameTestHelper helper, ItemStack stack, String itemName) {
        var expectedVanilla = Set.of(
                ResourceLocation.withDefaultNamespace("bane_of_arthropods"),
                ResourceLocation.withDefaultNamespace("fire_aspect"),
                ResourceLocation.withDefaultNamespace("knockback"),
                ResourceLocation.withDefaultNamespace("looting"),
                ResourceLocation.withDefaultNamespace("sharpness"),
                ResourceLocation.withDefaultNamespace("smite"),
                ResourceLocation.withDefaultNamespace("sweeping")
        );
        for (var enchantment : getRegisteredEnchantments()) {
            var id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (id == null) {
                continue;
            }
            var table = stack.getItem().canApplyAtEnchantingTable(stack, enchantment);
            var book = stack.getItem().isBookEnchantable(stack, createEnchantedBook(enchantment));
            if ("minecraft".equals(id.getNamespace())) {
                var expected = expectedVanilla.contains(id);
                helper.assertTrue(table == expected, itemName + " vanilla table rule changed for " + id);
                helper.assertTrue(book == expected, itemName + " vanilla book rule changed for " + id);
            }
            if (isDurabilityTargetEnchantment(enchantment)) {
                helper.assertFalse(table, itemName + " should reject durability enchantment " + id);
                helper.assertFalse(book, itemName + " should reject durability book " + id);
            }
            if (MALUM_HAUNTED.equals(id) || MALUM_SPIRIT_PLUNDER.equals(id)
                    || enchantment == EnchantmentRegistry.WISDOM.get()) {
                helper.assertTrue(table && book, itemName + " should allow " + id);
            }
            if (MALUM_REPLENISHING.equals(id)) {
                helper.assertTrue(table && book, itemName + " should allow " + id);
            }
            if (MALUM_ANIMATED.equals(id)) {
                helper.assertFalse(table || book, itemName + " should reject " + id);
            }
        }
    }

    private static void assertReplenishingRules(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
            return;
        }

        var replenishing = ForgeRegistries.ENCHANTMENTS.getValue(MALUM_REPLENISHING);
        helper.assertTrue(replenishing != null, "Malum Replenishing is not registered");

        for (var entry : List.of(
                Map.entry("Pastel Staff", new ItemStack(ItemRegistry.PASTEL_STAFF.get())),
                Map.entry("Multicast Echo Staff", new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get())),
                Map.entry("Zenith Staff", new ItemStack(ItemRegistry.ZENITH_STAFF.get())),
                Map.entry("Circuit Heat Staff", new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get()))
        )) {
            assertReplenishingRule(helper, entry.getValue(), replenishing, entry.getKey());
        }

        var rightClickWeapons = ItemRegistry.ITEMS.getEntries().stream()
                .map(RegistryObject::get)
                .filter(item -> item instanceof AbstractRightClickMagicWeaponItem)
                .map(ItemStack::new)
                .toList();
        helper.assertFalse(rightClickWeapons.isEmpty(), "No items matched Replenishing right-click weapon coverage");
        for (var stack : rightClickWeapons) {
            assertReplenishingRule(helper, stack, replenishing,
                    "Right-click magic weapon " + ForgeRegistries.ITEMS.getKey(stack.getItem()));
        }
        assertReplenishingRule(helper, new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()), replenishing,
                "Charged Twin Blade Staff");
    }

    private static void assertReplenishingRule(
            GameTestHelper helper,
            ItemStack stack,
            Enchantment replenishing,
            String itemName
    ) {
        var item = stack.getItem();
        helper.assertTrue(item.canApplyAtEnchantingTable(stack, replenishing),
                itemName + " should accept Replenishing at the enchanting table");
        helper.assertTrue(item.isBookEnchantable(stack, createEnchantedBook(replenishing)),
                itemName + " should accept a Replenishing enchanted book");
        if (item instanceof NonDamageableAnvilMergeItem mergeItem) {
            helper.assertTrue(mergeItem.isAnvilMergeEnchantmentAllowed(stack, replenishing),
                    itemName + " should accept Replenishing in an anvil merge");
        }
    }

    private static void verifyPolicySurface(
            List<String> mismatches,
            String name,
            boolean policy,
            boolean directApplication,
            boolean generatedTag
    ) {
        if (policy != directApplication || policy != generatedTag) {
            mismatches.add(name + " policy=" + policy + " direct=" + directApplication + " tag=" + generatedTag);
        }
    }

    private static void assertAcquisitionFlags(
            GameTestHelper helper,
            RegistryObject<Enchantment> enchantmentRegistryObject,
            boolean expectedTreasureOnly,
            boolean expectedTradeable,
            boolean expectedDiscoverable
    ) {
        var enchantment = enchantmentRegistryObject.get();
        var enchantmentId = String.valueOf(enchantmentRegistryObject.getId());
        helper.assertTrue(enchantment.isTreasureOnly() == expectedTreasureOnly,
                "Treasure flag changed for " + enchantmentId);
        helper.assertTrue(enchantment.isTradeable() == expectedTradeable,
                "Tradeable flag changed for " + enchantmentId);
        helper.assertTrue(enchantment.isDiscoverable() == expectedDiscoverable,
                "Discoverable flag changed for " + enchantmentId);
    }

    private static Set<ResourceLocation> expectedSpellGunEnchantments(ItemStack stack) {
        var expected = registryIdSet(EnchantmentRegistry.ALACRITY, EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR, EnchantmentRegistry.SURGE, EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.TENSE, EnchantmentRegistry.TRANSCENDENCE, EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER);
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedOffhandEnchantments() {
        return registryIdSet(EnchantmentRegistry.ALACRITY, EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR, EnchantmentRegistry.SURGE, EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.TENSE, EnchantmentRegistry.TRANSCENDENCE);
    }

    private static Set<ResourceLocation> expectedEnchantedCircletEnchantments() {
        var expected = new LinkedHashSet<>(expectedOffhandEnchantments());
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        return expected;
    }

    private static Set<ResourceLocation> expectedSwingcastStaffEnchantments(ItemStack stack) {
        return expectedSwingcastStavesEnchantments(stack, true);
    }

    private static Set<ResourceLocation> expectedMithrilFreecastStaffEnchantments(ItemStack stack) {
        return expectedSwingcastStavesEnchantments(stack, false);
    }

    private static Set<ResourceLocation> expectedRevolvercastStaffEnchantments(ItemStack stack) {
        return expectedSwingcastStavesEnchantments(stack, true);
    }

    private static Set<ResourceLocation> expectedCrystalBladedStaffEnchantments(ItemStack stack) {
        return expectedSwordBasedMagicWeaponEnchantments(stack, true);
    }

    private static Set<ResourceLocation> expectedIlluminateStellarStaffEnchantments(ItemStack stack) {
        return expectedSwordBasedMagicWeaponEnchantments(stack, true);
    }

    private static Set<ResourceLocation> expectedUniteLunaStaffEnchantments(ItemStack stack) {
        return expectedSwordBasedMagicWeaponEnchantments(stack, true);
    }

    private static Set<ResourceLocation> expectedSwingcastStavesEnchantments(
            ItemStack stack,
            boolean includeTranscendence
    ) {
        var expected = expectedSwordBasedMagicWeaponEnchantments(stack, includeTranscendence);
        expected.addAll(registryIdSet(EnchantmentRegistry.RESERVOIR, EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.TENSE, EnchantmentRegistry.ALACRITY));
        return expected;
    }

    private static Set<ResourceLocation> expectedSwordBasedMagicWeaponEnchantments(
            ItemStack stack,
            boolean includeTranscendence
    ) {
        var expected = swordEnchantments(false);
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        if (includeTranscendence) {
            expected.addAll(registryIdSet(EnchantmentRegistry.TRANSCENDENCE));
        }
        addExpectedMalumHauntedIfPresent(stack, expected);
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        addExpectedMalumReplenishingIfPresent(expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedSmashcastScepterEnchantingTableEnchantments(ItemStack stack) {
        var expected = collectAllowedEnchantments(enchantment -> enchantment.category.canEnchant(stack.getItem()));
        expected.add(ResourceLocation.withDefaultNamespace("smite"));
        expected.add(ResourceLocation.withDefaultNamespace("bane_of_arthropods"));
        expected.add(ResourceLocation.withDefaultNamespace("fire_aspect"));
        expected.addAll(registryIdSet(EnchantmentRegistry.COMPRESS, EnchantmentRegistry.RELEASE,
                EnchantmentRegistry.WISDOM, EnchantmentRegistry.PLUNDER, EnchantmentRegistry.TRANSCENDENCE));
        addExpectedMalumHauntedIfPresent(stack, expected);
        addExpectedMalumReplenishingIfPresent(expected);
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedSmashcastScepterBookEnchantments(ItemStack stack) {
        return new LinkedHashSet<>(expectedSmashcastScepterEnchantingTableEnchantments(stack));
    }

    private static Set<ResourceLocation> expectedChargedTwinBladeStaffEnchantments(ItemStack stack) {
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        var trident = new ItemStack(Items.TRIDENT);
        var expected = collectAllowedEnchantments(enchantment ->
                enchantment.canApplyAtEnchantingTable(sword) && !isDurabilityTargetEnchantment(enchantment));
        expected.addAll(collectAllowedEnchantments(enchantment ->
                enchantment.canApplyAtEnchantingTable(trident) && !isDurabilityTargetEnchantment(enchantment)));
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        addExpectedMalumHauntedIfPresent(stack, expected);
        addExpectedMalumReplenishingIfPresent(expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedManaForceBladeEnchantments(ItemStack stack) {
        var expected = swordEnchantments(true);
        expected.addAll(registryIdSet(EnchantmentRegistry.SURGE, EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.WISDOM, EnchantmentRegistry.TRANSCENDENCE));
        addExpectedMalumHauntedIfPresent(stack, expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedSpellSideEdgeEnchantments(ItemStack stack) {
        var expected = swordEnchantments(true);
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM, EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.ALACRITY, EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR, EnchantmentRegistry.TENSE));
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedCircuitHeatStaffEnchantments(ItemStack stack) {
        var expected = swordEnchantments(false);
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        addExpectedMalumHauntedIfPresent(stack, expected);
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        addExpectedMalumReplenishingIfPresent(expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedMultipurposeStaffrifleEnchantments(ItemStack stack) {
        var expected = registryIdSet(EnchantmentRegistry.ALACRITY, EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR, EnchantmentRegistry.SURGE, EnchantmentRegistry.TENSE,
                EnchantmentRegistry.WISDOM, EnchantmentRegistry.PLUNDER);
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedScrollcasterGauntletEnchantments(ItemStack stack) {
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        var durability = new ItemStack(Items.ELYTRA);
        var expected = collectAllowedEnchantments(enchantment ->
                (enchantment.canApplyAtEnchantingTable(sword)
                        || enchantment.canApplyAtEnchantingTable(pickaxe))
                        && !enchantment.canApplyAtEnchantingTable(durability));
        expected.addAll(collectAllowedEnchantments(enchantment -> enchantment.category.canEnchant(stack.getItem())));
        expected.addAll(registryIdSet(
                EnchantmentRegistry.ALACRITY,
                EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR,
                EnchantmentRegistry.TENSE,
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM
        ));
        expected.remove(ResourceLocation.withDefaultNamespace("sweeping"));
        addExpectedMalumHauntedIfPresent(stack, expected);
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        addExpectedMalumReplenishingIfPresent(expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedReflectcastShieldEnchantments() {
        var shield = new ItemStack(Items.SHIELD);
        var expected = collectAllowedEnchantments(enchantment -> enchantment.canApplyAtEnchantingTable(shield));
        expected.addAll(registryIdSet(EnchantmentRegistry.TRANSCENDENCE, EnchantmentRegistry.WISDOM));
        return expected;
    }

    private static Set<ResourceLocation> expectedElementalBowEnchantments() {
        var bow = new ItemStack(Items.BOW);
        var expected = collectAllowedEnchantments(enchantment -> Items.BOW.canApplyAtEnchantingTable(bow, enchantment));
        expected.addAll(registryIdSet(EnchantmentRegistry.TRANSCENDENCE, EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER, EnchantmentRegistry.SYNTHESIS));
        return expected;
    }

    private static Set<ResourceLocation> expectedElementalBowBookEnchantments() {
        var bow = new ItemStack(Items.BOW);
        var expected = collectAllowedEnchantments(enchantment ->
                Items.BOW.isBookEnchantable(bow, createEnchantedBook(enchantment)));
        expected.addAll(registryIdSet(EnchantmentRegistry.TRANSCENDENCE, EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER, EnchantmentRegistry.SYNTHESIS));
        return expected;
    }

    private static Set<ResourceLocation> expectedFlaskEnchantments() {
        return registryIdSet(EnchantmentRegistry.GUZZLE, EnchantmentRegistry.LARGE_MUG,
                EnchantmentRegistry.RED_ENERGY, EnchantmentRegistry.GLOW_ENERGY);
    }

    private static Set<ResourceLocation> expectedAlchemistsFlaskEnchantments() {
        return registryIdSet(EnchantmentRegistry.LARGE_MUG, EnchantmentRegistry.RED_ENERGY,
                EnchantmentRegistry.GLOW_ENERGY, EnchantmentRegistry.TRANSCENDENCE, EnchantmentRegistry.WISDOM);
    }

    private static Set<ResourceLocation> expectedSpellchargedGreatswordEnchantments(ItemStack stack) {
        var expected = new LinkedHashSet<>(swordEnchantments(true));
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        return expected;
    }

    private static Set<ResourceLocation> expectedEnchantressRobeEnchantments(ItemStack stack) {
        return expectedNormalMagicArmorEnchantments(stack);
    }

    private static Set<ResourceLocation> expectedSoulcollectorRobeEnchantments(ItemStack stack) {
        return expectedNormalMagicArmorEnchantments(stack);
    }

    private static Set<ResourceLocation> expectedStealthRuneArmorEnchantments(ItemStack stack) {
        var expected = expectedNormalMagicArmorEnchantments(stack);
        expected.addAll(registryIdSet(EnchantmentRegistry.RESERVOIR, EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.TENSE, EnchantmentRegistry.ALACRITY));
        if (stack.getItem() instanceof StealthRuneArmorItem armor && armor.hasImbueSlot()) {
            expected.addAll(registryIdSet(EnchantmentRegistry.TRANSCENDENCE));
        }
        return expected;
    }

    private static Set<ResourceLocation> expectedChromaticMagiaDressEnchantments(ItemStack stack) {
        return expectedNormalMagicArmorEnchantments(stack);
    }

    private static Set<ResourceLocation> expectedElementMaidenRobeEnchantments(ItemStack stack) {
        var expected = expectedNormalMagicArmorEnchantments(stack);
        if (stack.getItem() instanceof ElementMaidenRobeItem robe && robe.hasImbueSlot()) {
            expected.addAll(registryIdSet(EnchantmentRegistry.SURGE, EnchantmentRegistry.ATTUNEMENT,
                    EnchantmentRegistry.TRANSCENDENCE));
        }
        return expected;
    }

    private static Set<ResourceLocation> expectedMagiAgentSuitEnchantments(ItemStack stack) {
        return expectedNormalMagicArmorEnchantments(stack);
    }

    private static Set<ResourceLocation> expectedNormalMagicArmorEnchantments(ItemStack stack) {
        var probe = createArmorProbeStack(stack);
        var expected = collectAllowedEnchantments(enchantment -> enchantment.canApplyAtEnchantingTable(probe));
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        return expected;
    }

    private static Set<ResourceLocation> expectedArmorAnvilEnchantments(ItemStack stack) {
        var probe = createArmorProbeStack(stack);
        var expected = collectAllowedEnchantments(enchantment -> enchantment.canEnchant(probe));
        expected.addAll(collectAllowedEnchantments(enchantment -> {
            var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            return enchantmentId != null
                    && ApprenticeCodex.MODID.equals(enchantmentId.getNamespace())
                    && stack.canApplyAtEnchantingTable(enchantment);
        }));
        return expected;
    }

    private static Set<ResourceLocation> swordEnchantments(boolean includeDurability) {
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        return collectAllowedEnchantments(enchantment -> enchantment.canApplyAtEnchantingTable(sword)
                && (includeDurability || !isDurabilityTargetEnchantment(enchantment)));
    }

    private static ItemStack createArmorProbeStack(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            throw new IllegalArgumentException("Expected armor item for enchantment probe: " + stack);
        }
        return switch (armorItem.getType()) {
            case HELMET -> new ItemStack(Items.LEATHER_HELMET);
            case CHESTPLATE -> new ItemStack(Items.LEATHER_CHESTPLATE);
            case LEGGINGS -> new ItemStack(Items.LEATHER_LEGGINGS);
            case BOOTS -> new ItemStack(Items.LEATHER_BOOTS);
        };
    }

    private record AttributePolicyCase(Item item, Set<AttributeEnchantmentType> directlyApplicable) {
    }
}
