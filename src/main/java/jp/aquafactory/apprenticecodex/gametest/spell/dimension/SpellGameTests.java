package jp.aquafactory.apprenticecodex.gametest.spell.dimension;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = RIFT_HOLE_ISOLATED_BATCH)
    public static void riftHoleDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.riftHoleDimensionDenylistRejectsCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = RIFT_HOLE_ISOLATED_BATCH)
    public static void riftHoleDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.riftHoleDimensionAllowlistRequiresCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = RIFT_HOLE_ISOLATED_BATCH)
    public static void riftHoleDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        DimensionRestrictionScenarios.riftHoleDimensionDenylistOverridesAllowlist(helper);
    }

    @GameTest(template = TEMPLATE, batch = DEMICREATOR_WINGS_ISOLATED_BATCH)
    public static void demicreatorWingsDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.demicreatorWingsDimensionDenylistRejectsCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = DEMICREATOR_WINGS_ISOLATED_BATCH)
    public static void demicreatorWingsDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.demicreatorWingsDimensionAllowlistRequiresCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = DEMICREATOR_WINGS_ISOLATED_BATCH)
    public static void demicreatorWingsDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        DimensionRestrictionScenarios.demicreatorWingsDimensionDenylistOverridesAllowlist(helper);
    }

    @GameTest(template = TEMPLATE, batch = DEMICREATOR_WINGS_ISOLATED_BATCH)
    public static void demicreatorWingsDimensionRestrictionAllowsCloseCast(GameTestHelper helper) {
        DimensionRestrictionScenarios.demicreatorWingsDimensionRestrictionAllowsCloseCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeDimensionDenylistRejectsCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeDimensionAllowlistRequiresCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeDimensionDenylistOverridesAllowlist(helper);
    }

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeSanitizerKeepsStoredLongDuration(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeSanitizerKeepsStoredLongDuration(helper);
    }

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeSanitizerUsesLegacyFallbackWhenDurationMissing(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeSanitizerUsesLegacyFallbackWhenDurationMissing(helper);
    }
}
