package jp.aquafactory.apprenticecodex.gametest.spell.targeting.combat;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.COMBAT_TARGET_POLICY_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CombatTargetPolicyGameTests {
    private CombatTargetPolicyGameTests() {}

    @GameTest(template = TEMPLATE, batch = COMBAT_TARGET_POLICY_BATCH)
    public static void combatTargetPolicySeparatesSelfDamageFromAllyProtection(GameTestHelper helper) {
        CombatTargetPolicyScenarios.combatTargetPolicySeparatesSelfDamageFromAllyProtection(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBAT_TARGET_POLICY_BATCH)
    public static void combatTargetPolicyRespectsTeamFriendlyFireAndPvp(GameTestHelper helper) {
        CombatTargetPolicyScenarios.combatTargetPolicyRespectsTeamFriendlyFireAndPvp(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBAT_TARGET_POLICY_BATCH)
    public static void combatTargetPolicyProtectsWholeVehicleAndOwnedEntities(GameTestHelper helper) {
        CombatTargetPolicyScenarios.combatTargetPolicyProtectsWholeVehicleAndOwnedEntities(helper);
    }
}
