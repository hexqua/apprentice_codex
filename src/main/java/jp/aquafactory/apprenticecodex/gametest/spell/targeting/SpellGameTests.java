package jp.aquafactory.apprenticecodex.gametest.spell.targeting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combat_target_policy")
    public static void combatTargetPolicySeparatesSelfDamageFromAllyProtection(GameTestHelper helper) {
        CombatTargetPolicyScenarios.combatTargetPolicySeparatesSelfDamageFromAllyProtection(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combat_target_policy")
    public static void combatTargetPolicyRespectsTeamFriendlyFireAndPvp(GameTestHelper helper) {
        CombatTargetPolicyScenarios.combatTargetPolicyRespectsTeamFriendlyFireAndPvp(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combat_target_policy")
    public static void combatTargetPolicyProtectsWholeVehicleAndOwnedEntities(GameTestHelper helper) {
        CombatTargetPolicyScenarios.combatTargetPolicyProtectsWholeVehicleAndOwnedEntities(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.beam_occlusion_isolated")
    public static void beamLengthIgnoresNoCollisionGrass(GameTestHelper helper) {
        BeamOcclusionScenarios.beamLengthIgnoresNoCollisionGrass(helper);
    }
}
