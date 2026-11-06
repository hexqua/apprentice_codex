package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import java.util.List;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestHelper;

final class StaffrifleDenylistScenarios {
    private StaffrifleDenylistScenarios() {}

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
