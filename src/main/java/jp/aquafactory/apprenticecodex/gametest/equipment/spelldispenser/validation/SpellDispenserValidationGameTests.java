package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.validation;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserValidationGameTests {
    private SpellDispenserValidationGameTests() {}

    private record SupportedCase(String name, AbstractSpell spell) {}
    private record RejectedCase(String name, ItemStack input, SpellDispenserSpellValidator.FailureReason reason) {}

    @GameTest(template = "gametest/basic_floor")
    public static void supportedProfilesResolveTheOriginalSpellWithoutMutatingScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var cases = List.of(
                    new SupportedCase("instant", io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()),
                    new SupportedCase("codex_namespace", SpellRegistry.MAGE_LIGHT.get()),
                    new SupportedCase("long", SpellRegistry.COMPOUND_PHIAL.get()),
                    new SupportedCase("continuous", io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()));
            for (var testCase : cases) {
                var scroll = createSpellScroll(testCase.spell());
                var snapshot = scroll.copy();
                var result = SpellDispenserSpellValidator.validate(scroll);
                helper.assertTrue(result.isSupported() && result.spellData().getSpell() == testCase.spell()
                                && result.spellData().getLevel() == 1,
                        "Supported scroll should resolve its original spell and level: " + testCase.name());
                helper.assertTrue(ItemStack.matches(snapshot, scroll),
                        "Validation should not mutate the accepted scroll: " + testCase.name());
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void unsupportedSpellProfilesReportTheirActualFailure(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var cases = List.of(
                    new RejectedCase("unprofiled_siphoning", createSpellScroll(
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAY_OF_SIPHONING_SPELL.get()),
                            SpellDispenserSpellValidator.FailureReason.NOT_PROFILED),
                    new RejectedCase("unprofiled_continuous", createSpellScroll(SpellRegistry.LONG_STRIDE.get()),
                            SpellDispenserSpellValidator.FailureReason.NOT_PROFILED),
                    new RejectedCase("recast", createSpellScroll(SpellRegistry.QUICK_ARMS.get()),
                            SpellDispenserSpellValidator.FailureReason.HAS_RECAST));
            assertRejectedCases(helper, cases);
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void malformedScrollsAndNonScrollContainersRemainUnchanged(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var nonScroll = createInitializedPresetStack(ItemRegistry.GRIMOIRE_MANIFEST.get());
            helper.assertTrue(ISpellContainer.isSpellContainer(nonScroll), "Non-scroll fixture should contain a spell");
            var plainScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            ISpellContainer.remove(plainScroll);
            var emptyScroll = plainScroll.copy();
            ISpellContainer.set(emptyScroll, ISpellContainer.create(1, true, false));
            var multiple = plainScroll.copy();
            var spells = ISpellContainer.create(2, true, false).mutableCopy();
            helper.assertTrue(spells.addSpellAtIndex(SpellRegistry.MAGE_LIGHT.get(), 1, 0, false)
                            && spells.addSpellAtIndex(SpellRegistry.COMPOUND_PHIAL.get(), 1, 1, false),
                    "Multiple-spell fixture should contain two active spells");
            ISpellContainer.set(multiple, spells.toImmutable());
            assertRejectedCases(helper, List.of(
                    new RejectedCase("empty", ItemStack.EMPTY, SpellDispenserSpellValidator.FailureReason.EMPTY),
                    new RejectedCase("non_scroll_container", nonScroll, SpellDispenserSpellValidator.FailureReason.NOT_SCROLL),
                    new RejectedCase("missing_container", plainScroll, SpellDispenserSpellValidator.FailureReason.NOT_SPELL_CONTAINER),
                    new RejectedCase("empty_container", emptyScroll, SpellDispenserSpellValidator.FailureReason.NO_ACTIVE_SPELL),
                    new RejectedCase("multiple_spells", multiple, SpellDispenserSpellValidator.FailureReason.MULTIPLE_ACTIVE_SPELLS)));
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "spell_dispenser_server_disabled_config")
    public static void spellDispenserValidatorRejectsWhenServerDisabled(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                    false,
                    false,
                    List.of(),
                    1.0D
            )) {
                var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

                var validation = SpellDispenserSpellValidator.validate(scrollStack);
                helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a scroll while server-disabled");
                helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.SERVER_DISABLED,
                        "Spell Dispenser validator returned the wrong failure reason while server-disabled: " + validation.failureReason());
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "spell_dispenser_server_allowlist_config")
    public static void spellDispenserValidatorRequiresServerAllowlist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var magicMissileStack = createSpellScroll(magicMissile);
            try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                    true,
                    true,
                    List.of(),
                    1.0D
            )) {
                var validation = SpellDispenserSpellValidator.validate(magicMissileStack);
                helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a scroll with an empty server allowlist");
                helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.NOT_ALLOWLISTED,
                        "Spell Dispenser validator returned the wrong failure reason for an empty server allowlist: " + validation.failureReason());
            }

            try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                    true,
                    true,
                    List.of(magicMissile.getSpellResource().toString()),
                    1.0D
            )) {
                var validation = SpellDispenserSpellValidator.validate(magicMissileStack);
                helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected an allowlisted profiled spell");
            }

            var unprofiledSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAY_OF_SIPHONING_SPELL.get();
            try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                    true,
                    true,
                    List.of(unprofiledSpell.getSpellResource().toString()),
                    1.0D
            )) {
                var validation = SpellDispenserSpellValidator.validate(createSpellScroll(unprofiledSpell));
                helper.assertTrue(!validation.isSupported(), "Spell Dispenser server allowlist bypassed the profile requirement");
                helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.NOT_PROFILED,
                        "Spell Dispenser validator returned the wrong allowlist + unprofiled failure: " + validation.failureReason());
            }
        });
    }

    private static void assertRejectedCases(GameTestHelper helper, List<RejectedCase> cases) {
        for (var testCase : cases) {
            var snapshot = testCase.input().copy();
            var result = SpellDispenserSpellValidator.validate(testCase.input());
            helper.assertTrue(!result.isSupported() && result.failureReason() == testCase.reason(),
                    "Rejected input=" + testCase.name() + " expected=" + testCase.reason() + " actual=" + result.failureReason());
            helper.assertTrue(ItemStack.matches(snapshot, testCase.input()),
                    "Validation should preserve the complete rejected input: " + testCase.name());
        }
    }

    private static ItemStack createInitializedPresetStack(Item item) {
        var stack = new ItemStack(item);
        if (item instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(stack);
        }
        return stack;
    }
}
