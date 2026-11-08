package jp.aquafactory.apprenticecodex.gametest.equipment.spellsideedge;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeStartsWithLockedEdgeDancerAndPassesRightClick(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeStartsWithLockedEdgeDancerAndPassesRightClick(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeKeepsExpectedTags(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeKeepsExpectedTags(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeUsesArcaneIngotRepairAndMirrorIsNotRepairable(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeUsesArcaneIngotRepairAndMirrorIsNotRepairable(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBridgeUsesHigherComparableMainhandAttribute(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeUsesHigherComparableMainhandAttribute(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBridgeSkipsMultiplyTotalWhenOffhandAlreadyHasAttribute(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeSkipsMultiplyTotalWhenOffhandAlreadyHasAttribute(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBridgeIgnoresVanillaAttackAttributes(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeIgnoresVanillaAttackAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBridgeIncludesStackAttributeModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeIncludesStackAttributeModifiers(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBridgeIncludesEnchantmentAttributeModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeIncludesEnchantmentAttributeModifiers(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBridgeIncludesMalumHauntedMagicDamage(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeIncludesMalumHauntedMagicDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBridgeDoesNotDoubleApplyUpgradeOrbModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeDoesNotDoubleApplyUpgradeOrbModifiers(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBridgeResyncsChangedStackAttributeAmounts(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeResyncsChangedStackAttributeAmounts(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBridgeSyncsOnlyWhileHeldInMainhand(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeSyncsOnlyWhileHeldInMainhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void edgeDancerRequiresMainhandSpellSideEdge(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerRequiresMainhandSpellSideEdge(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void edgeDancerGeneratesMirrorAndRestoresEmptyOffhand(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerGeneratesMirrorAndRestoresEmptyOffhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void edgeDancerMirrorCopiesEnchantmentsAndReplacesSpell(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerMirrorCopiesEnchantmentsAndReplacesSpell(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void edgeDancerRestoresOccupiedOffhand(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerRestoresOccupiedOffhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void edgeDancerDeactivatesWhenSpellSideEdgeLeavesInventory(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerDeactivatesWhenSpellSideEdgeLeavesInventory(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void edgeDancerMainhandDropCancelsAndOnlyDeactivates(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerMainhandDropCancelsAndOnlyDeactivates(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void edgeDancerOffhandSwapCancelsAndOnlyDeactivates(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerOffhandSwapCancelsAndOnlyDeactivates(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeMirrorOffhandDoesNotApplyVanillaAttackModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeMirrorOffhandDoesNotApplyVanillaAttackModifiers(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeMirrorPairBypassesVanillaTargetIframe(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeMirrorPairBypassesVanillaTargetIframe(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeRequiresMirrorForVanillaTargetIframeBypass(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeRequiresMirrorForVanillaTargetIframeBypass(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeVanillaIframeBypassOnlyAppliesToRecordedTarget(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeVanillaIframeBypassOnlyAppliesToRecordedTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeVanillaIframeBypassDisabledWithCombatOverhauls(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeVanillaIframeBypassDisabledWithCombatOverhauls(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void anchorBlinkRequiresGeneratedMirrorAndRestoresOffhandOnCast(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkRequiresGeneratedMirrorAndRestoresOffhandOnCast(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void anchorBlinkReadyAnchorOnlyMatchesOwner(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkReadyAnchorOnlyMatchesOwner(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void anchorBlinkImpactedDaggerDoesNotDamageAgain(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkImpactedDaggerDoesNotDamageAgain(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void anchorBlinkPostTeleportProtectionOnlyBlocksEnemyDamage(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkPostTeleportProtectionOnlyBlocksEnemyDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void anchorBlinkPostTeleportProtectionSurvivesOtherDimensionCleanup(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkPostTeleportProtectionSurvivesOtherDimensionCleanup(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void anchorBlinkImpactBeyondMaximumRangeDiscardsDagger(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkImpactBeyondMaximumRangeDiscardsDagger(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void anchorBlinkMaximumRangeOnlyChecksAtImpact(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkMaximumRangeOnlyChecksAtImpact(helper);
    }
}
