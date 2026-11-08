package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LegacySpellgunGameTests {
    private LegacySpellgunGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellgunCooldownPoliciesApplyToWeapons(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunCooldownPoliciesApplyToWeapons(helper);
    }







    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(helper);
    }

    static void multipurposeStaffrifleServerDenylistBlocksSpecialCastSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            helper.assertFalse(MultipurposeStaffrifle.isSpecialCastSpellDenied(spell),
                    "Multipurpose Staffrifle should not deny Magic Missile by default");

            try (var ignored = ApprenticeCodexServerConfig.useMultipurposeStaffrifleSpellDenylistOverrideForGameTest(
                    List.of(spell.getSpellResource().toString())
            )) {
                helper.assertTrue(MultipurposeStaffrifle.isSpecialCastSpellDenied(spell),
                        "Multipurpose Staffrifle server denylist should deny Magic Missile");
            }

            helper.assertFalse(MultipurposeStaffrifle.isSpecialCastSpellDenied(spell),
                    "Multipurpose Staffrifle server denylist override should be restored");
        });
    }
}
