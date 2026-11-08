package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.validation;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.SpellScrollTestSupport.createSpellScroll;

final class SpellDispenserValidationScenarios {
    private SpellDispenserValidationScenarios() {}

    static void spellDispenserValidatorRejectsWhenServerDisabled(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellDispenserConfigOverrideForGameTest(
                    false,
                    false,
                    List.of(),
                    1.0D
            )) {
                var scrollStack = createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get());

                var validation = SpellDispenserSpellValidator.validate(scrollStack);
                helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a scroll while server-disabled");
                helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.SERVER_DISABLED,
                        "Spell Dispenser validator returned the wrong failure reason while server-disabled: " + validation.failureReason());
            }
        });
    }

    static void spellDispenserValidatorRequiresServerAllowlist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var magicMissile = SpellRegistry.MAGIC_MISSILE_SPELL.get();
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

            var unprofiledSpell = SpellRegistry.RAY_OF_SIPHONING_SPELL.get();
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
}
