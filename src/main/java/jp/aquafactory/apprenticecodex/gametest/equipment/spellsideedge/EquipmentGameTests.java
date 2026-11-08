package jp.aquafactory.apprenticecodex.gametest.equipment.spellsideedge;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeKeepsExpectedTags(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeKeepsExpectedTags(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeUsesArcaneIngotRepairAndMirrorIsNotRepairable(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeUsesArcaneIngotRepairAndMirrorIsNotRepairable(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeUsesHigherComparableMainhandAttribute(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeUsesHigherComparableMainhandAttribute(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeSkipsMultiplyTotalWhenOffhandAlreadyHasAttribute(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeSkipsMultiplyTotalWhenOffhandAlreadyHasAttribute(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeIgnoresVanillaAttackAttributes(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeIgnoresVanillaAttackAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeIncludesStackAttributeModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeIncludesStackAttributeModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeDoesNotDoubleApplyUpgradeOrbModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeDoesNotDoubleApplyUpgradeOrbModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeResyncsChangedStackAttributeAmounts(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeResyncsChangedStackAttributeAmounts(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeSyncsOnlyWhileHeldInMainhand(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeSyncsOnlyWhileHeldInMainhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerRequiresMainhandSpellSideEdge(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerRequiresMainhandSpellSideEdge(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerGeneratesMirrorAndRestoresEmptyOffhand(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerGeneratesMirrorAndRestoresEmptyOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerMirrorCopiesEnchantmentsAndReplacesSpell(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerMirrorCopiesEnchantmentsAndReplacesSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerRestoresOccupiedOffhand(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerRestoresOccupiedOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerDeactivatesWhenSpellSideEdgeLeavesInventory(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerDeactivatesWhenSpellSideEdgeLeavesInventory(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerMainhandDropCancelsAndOnlyDeactivates(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerMainhandDropCancelsAndOnlyDeactivates(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerOffhandSwapCancelsAndOnlyDeactivates(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerOffhandSwapCancelsAndOnlyDeactivates(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeMirrorOffhandDoesNotApplyVanillaAttackModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeMirrorOffhandDoesNotApplyVanillaAttackModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeMirrorPairBypassesVanillaTargetIframe(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeMirrorPairBypassesVanillaTargetIframe(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeRequiresMirrorForVanillaTargetIframeBypass(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeRequiresMirrorForVanillaTargetIframeBypass(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeVanillaIframeBypassOnlyAppliesToRecordedTarget(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeVanillaIframeBypassOnlyAppliesToRecordedTarget(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeVanillaIframeBypassDisabledWithCombatOverhauls(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeVanillaIframeBypassDisabledWithCombatOverhauls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkRequiresGeneratedMirrorAndRestoresOffhandOnCast(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkRequiresGeneratedMirrorAndRestoresOffhandOnCast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkReadyAnchorOnlyMatchesOwner(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkReadyAnchorOnlyMatchesOwner(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkImpactedDaggerDoesNotDamageAgain(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkImpactedDaggerDoesNotDamageAgain(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkPostTeleportProtectionOnlyBlocksEnemyDamage(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkPostTeleportProtectionOnlyBlocksEnemyDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkPostTeleportProtectionSurvivesOtherDimensionCleanup(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkPostTeleportProtectionSurvivesOtherDimensionCleanup(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkImpactBeyondMaximumRangeDiscardsDagger(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkImpactBeyondMaximumRangeDiscardsDagger(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkMaximumRangeOnlyChecksAtImpact(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkMaximumRangeOnlyChecksAtImpact(helper);
    }
}
