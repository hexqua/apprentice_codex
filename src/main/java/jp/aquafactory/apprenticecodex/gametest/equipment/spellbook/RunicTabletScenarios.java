package jp.aquafactory.apprenticecodex.gametest.equipment.spellbook;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellStainedRunicTabletServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet.SpellStainedRunicTablet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertCurioModifierAmount;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.createSpellbookSlotContext;

final class RunicTabletScenarios {
    private RunicTabletScenarios() {}

    static void spellStainedRunicTabletUsesDefaultServerConfigValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellStainedRunicTablet) ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get();
            var stack = createSpellStainedRunicTabletStack(
                    helper,
                    spellEntry(SpellRegistry.HIGANBANA.get(), SpellRarity.COMMON),
                    spellEntry(SpellRegistry.FROST_RUNE.get(), SpellRarity.UNCOMMON),
                    spellEntry(SpellRegistry.THERMAL_PROCESS.get(), SpellRarity.RARE),
                    spellEntry(SpellRegistry.FORCE_FIELD.get(), SpellRarity.EPIC),
                    spellEntry(SpellRegistry.PALETTE_SHIFT.get(), SpellRarity.LEGENDARY),
                    spellEntry(SpellRegistry.QUICK_ARMS.get(), SpellRarity.COMMON)
            );
            var slotContext = createSpellbookSlotContext(helper);
            var expected = resolveExpectedSpellStainedRunicTabletAttributes(helper, stack);

            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    AttributeRegistry.MAX_MANA.get(),
                    expected.maxMana(),
                    AttributeModifier.Operation.ADDITION,
                    "Spell-stained Runic Tablet max mana default config mismatch"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    AttributeRegistry.SPELL_POWER.get(),
                    expected.generalSpellPower(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Spell-stained Runic Tablet general spell power default config mismatch"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    AttributeRegistry.COOLDOWN_REDUCTION.get(),
                    expected.cooldownReduction(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Spell-stained Runic Tablet cooldown reduction default config mismatch"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    AttributeRegistry.CAST_TIME_REDUCTION.get(),
                    expected.castTimeReduction(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Spell-stained Runic Tablet cast time reduction should not start below duplicate threshold"
            );
            for (var entry : expected.schoolSpellPower().entrySet()) {
                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        entry.getKey(),
                        entry.getValue(),
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        "Spell-stained Runic Tablet school spell power default config mismatch"
                );
            }
        });
    }

    static void spellStainedRunicTabletAcceptsNegativeServerConfigValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var values = new SpellStainedRunicTabletServerConfig.Values(
                    sameRarityBonuses(-2.0D),
                    sameRarityBonuses(-0.07D),
                    0.05D,
                    0.05D,
                    sameRarityBonuses(-0.08D),
                    new SpellStainedRunicTabletServerConfig.ScalingBonus(1, -0.25D, 0.0D),
                    new SpellStainedRunicTabletServerConfig.ScalingBonus(1, -0.50D, 0.0D)
            );

            try (var ignored = ApprenticeCodexServerConfig.useSpellStainedRunicTabletConfigOverrideForGameTest(values)) {
                var item = (SpellStainedRunicTablet) ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get();
                var stack = createSpellStainedRunicTabletStack(
                        helper,
                        spellEntry(SpellRegistry.HIGANBANA.get(), SpellRarity.COMMON)
                );
                var slotContext = createSpellbookSlotContext(helper);

                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        AttributeRegistry.MAX_MANA.get(),
                        -2.0D,
                        AttributeModifier.Operation.ADDITION,
                        "Spell-stained Runic Tablet negative max mana config mismatch"
                );
                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        AttributeRegistry.SPELL_POWER.get(),
                        -0.08D,
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        "Spell-stained Runic Tablet negative general spell power config mismatch"
                );
                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        AttributeRegistry.COOLDOWN_REDUCTION.get(),
                        -0.25D,
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        "Spell-stained Runic Tablet negative cooldown reduction config mismatch"
                );
                assertCurioModifierAmount(
                        helper,
                        item,
                        slotContext,
                        stack,
                        AttributeRegistry.CAST_TIME_REDUCTION.get(),
                        -0.50D,
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        "Spell-stained Runic Tablet negative cast time reduction config mismatch"
                );
                assertSpellStainedRunicTabletSchoolPower(
                        helper,
                        item,
                        slotContext,
                        stack,
                        SpellRegistry.HIGANBANA.get(),
                        -0.07D
                );
            }
        });
    }

    static void spellStainedRunicTabletFiltersSchoolPowerByConfiguredThresholds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertSpellStainedRunicTabletThresholdScenario(helper, 0.04D, 0.05D, 0.05D, 0.0D,
                    "Spell-stained Runic Tablet should ignore positive school spell power below threshold");
            assertSpellStainedRunicTabletThresholdScenario(helper, 0.05D, 0.05D, 0.05D, 0.05D,
                    "Spell-stained Runic Tablet should apply positive school spell power at threshold");
            assertSpellStainedRunicTabletThresholdScenario(helper, -0.04D, 0.05D, 0.05D, 0.0D,
                    "Spell-stained Runic Tablet should ignore negative school spell power below threshold");
            assertSpellStainedRunicTabletThresholdScenario(helper, -0.05D, 0.05D, 0.05D, -0.05D,
                    "Spell-stained Runic Tablet should apply negative school spell power at threshold");
        });
    }

    static void assertSpellStainedRunicTabletThresholdScenario(
            GameTestHelper helper,
            double configuredSchoolSpellPower,
            double minimumAppliedPositiveBonus,
            double minimumAppliedNegativePenalty,
            double expectedSchoolSpellPower,
            String message
    ) {
        var values = new SpellStainedRunicTabletServerConfig.Values(
                sameRarityBonuses(0.0D),
                sameRarityBonuses(configuredSchoolSpellPower),
                minimumAppliedPositiveBonus,
                minimumAppliedNegativePenalty,
                sameRarityBonuses(0.0D),
                new SpellStainedRunicTabletServerConfig.ScalingBonus(99, 0.0D, 0.0D),
                new SpellStainedRunicTabletServerConfig.ScalingBonus(99, 0.0D, 0.0D)
        );

        try (var ignored = ApprenticeCodexServerConfig.useSpellStainedRunicTabletConfigOverrideForGameTest(values)) {
            var item = (SpellStainedRunicTablet) ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get();
            var stack = createSpellStainedRunicTabletStack(
                    helper,
                    spellEntry(SpellRegistry.HIGANBANA.get(), SpellRarity.COMMON)
            );
            assertSpellStainedRunicTabletSchoolPower(
                    helper,
                    item,
                    createSpellbookSlotContext(helper),
                    stack,
                    SpellRegistry.HIGANBANA.get(),
                    expectedSchoolSpellPower,
                    message
            );
        }
    }

    static void assertSpellStainedRunicTabletSchoolPower(
            GameTestHelper helper,
            SpellStainedRunicTablet item,
            SlotContext slotContext,
            ItemStack stack,
            AbstractSpell spell,
            double expectedAmount
    ) {
        assertSpellStainedRunicTabletSchoolPower(
                helper,
                item,
                slotContext,
                stack,
                spell,
                expectedAmount,
                "Spell-stained Runic Tablet school spell power mismatch for " + spell.getSpellResource()
        );
    }

    static void assertSpellStainedRunicTabletSchoolPower(
            GameTestHelper helper,
            SpellStainedRunicTablet item,
            SlotContext slotContext,
            ItemStack stack,
            AbstractSpell spell,
            double expectedAmount,
            String message
    ) {
        var attribute = MagicTools.resolveSchoolPowerAttribute(spell.getSchoolType());
        helper.assertTrue(attribute != null, "Could not resolve school spell power attribute for " + spell.getSpellResource());
        assertCurioModifierAmount(
                helper,
                item,
                slotContext,
                stack,
                attribute,
                expectedAmount,
                AttributeModifier.Operation.MULTIPLY_BASE,
                message
        );
    }

    static ItemStack createSpellStainedRunicTabletStack(GameTestHelper helper, SpellEntry... entries) {
        var item = (SpellStainedRunicTablet) ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        helper.assertTrue(ISpellContainer.isSpellContainer(stack),
                "Spell-stained Runic Tablet did not initialize a spell container");

        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Spell-stained Runic Tablet spell container is null");
        helper.assertTrue(spellContainer.getMaxSpellCount() >= entries.length,
                "Spell-stained Runic Tablet test needs " + entries.length + " slots but got "
                        + spellContainer.getMaxSpellCount());

        var mutable = spellContainer.mutableCopy();
        for (var index = 0; index < entries.length; ++index) {
            var entry = entries[index];
            var level = entry.spell().getMinLevelForRarity(entry.rarity());
            helper.assertTrue(level > 0,
                    "Cannot prepare " + entry.rarity() + " rarity for " + entry.spell().getSpellResource());
            helper.assertTrue(mutable.addSpellAtIndex(entry.spell(), level, index, false),
                    "Failed to add Spell-stained Runic Tablet test spell " + entry.spell().getSpellResource()
                            + " at index " + index);
        }
        ISpellContainer.set(stack, mutable.toImmutable());

        var preparedContainer = ISpellContainer.get(stack);
        helper.assertTrue(preparedContainer != null, "Prepared Spell-stained Runic Tablet spell container is null");
        for (var index = 0; index < entries.length; ++index) {
            var spellData = preparedContainer.getSpellAtIndex(index);
            var expected = entries[index];
            helper.assertTrue(spellData.getSpell() == expected.spell(),
                    "Prepared Spell-stained Runic Tablet spell mismatch at index " + index
                            + ": expected " + expected.spell().getSpellResource()
                            + " but got " + spellData.getSpell().getSpellResource() + " " + spellData.getRarity());
        }

        return stack;
    }

    static ExpectedSpellStainedRunicTabletAttributes resolveExpectedSpellStainedRunicTabletAttributes(
            GameTestHelper helper,
            ItemStack stack
    ) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing Spell-stained Runic Tablet spell container for expected attributes");
        var values = ApprenticeCodexServerConfig.spellStainedRunicTabletConfig();
        double maxMana = 0.0D;
        double generalSpellPower = 0.0D;
        var schoolSpellPower = new LinkedHashMap<Attribute, Double>();
        var schoolSpellCounts = new LinkedHashMap<String, Integer>();

        for (var spellSlot : spellContainer.getActiveSpells()) {
            var spellData = spellSlot.spellData();
            var rarity = spellData.getRarity();
            var schoolType = spellData.getSpell().getSchoolType();
            maxMana += values.maxMana().forRarity(rarity);
            generalSpellPower += values.generalSpellPower().forRarity(rarity);
            schoolSpellCounts.merge(schoolType.getId().toString(), 1, Integer::sum);

            var schoolAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);
            if (schoolAttribute != null) {
                schoolSpellPower.merge(schoolAttribute, values.schoolSpellPower().forRarity(rarity), Double::sum);
            }
        }

        var topSchoolCount = schoolSpellCounts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        return new ExpectedSpellStainedRunicTabletAttributes(
                maxMana,
                generalSpellPower,
                filterExpectedSpellStainedRunicTabletSchoolPower(schoolSpellPower, values),
                values.cooldownReduction().resolve(schoolSpellCounts.size()),
                values.castTimeReduction().resolve(topSchoolCount)
        );
    }

    static Map<Attribute, Double> filterExpectedSpellStainedRunicTabletSchoolPower(
            Map<Attribute, Double> schoolSpellPower,
            SpellStainedRunicTabletServerConfig.Values values
    ) {
        var filtered = new LinkedHashMap<Attribute, Double>();
        for (var entry : schoolSpellPower.entrySet()) {
            if (shouldExpectSpellStainedRunicTabletSchoolPower(entry.getValue(), values)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    static boolean shouldExpectSpellStainedRunicTabletSchoolPower(
            double amount,
            SpellStainedRunicTabletServerConfig.Values values
    ) {
        if (amount > 0.0D) {
            return amount >= values.minimumAppliedPositiveBonus();
        }
        if (amount < 0.0D) {
            return Math.abs(amount) >= values.minimumAppliedNegativePenalty();
        }
        return false;
    }

    static SpellEntry spellEntry(AbstractSpell spell, SpellRarity rarity) {
        return new SpellEntry(spell, rarity);
    }

    static SpellStainedRunicTabletServerConfig.RarityBonuses sameRarityBonuses(double value) {
        return new SpellStainedRunicTabletServerConfig.RarityBonuses(value, value, value, value, value, value);
    }

    record SpellEntry(AbstractSpell spell, SpellRarity rarity) {
    }

    record ExpectedSpellStainedRunicTabletAttributes(
            double maxMana,
            double generalSpellPower,
            Map<Attribute, Double> schoolSpellPower,
            double cooldownReduction,
            double castTimeReduction
    ) {
    }
}
