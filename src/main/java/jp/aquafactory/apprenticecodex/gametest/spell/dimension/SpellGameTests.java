package jp.aquafactory.apprenticecodex.gametest.spell.dimension;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.rift_hole_isolated")
    public static void riftHoleDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.riftHoleDimensionDenylistRejectsCurrentDimension(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.rift_hole_isolated")
    public static void riftHoleDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.riftHoleDimensionAllowlistRequiresCurrentDimension(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.rift_hole_isolated")
    public static void riftHoleDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        DimensionRestrictionScenarios.riftHoleDimensionDenylistOverridesAllowlist(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.demicreator_wings_isolated")
    public static void demicreatorWingsDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.demicreatorWingsDimensionDenylistRejectsCurrentDimension(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.demicreator_wings_isolated")
    public static void demicreatorWingsDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.demicreatorWingsDimensionAllowlistRequiresCurrentDimension(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.demicreator_wings_isolated")
    public static void demicreatorWingsDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        DimensionRestrictionScenarios.demicreatorWingsDimensionDenylistOverridesAllowlist(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.demicreator_wings_isolated")
    public static void demicreatorWingsDimensionRestrictionAllowsCloseCast(GameTestHelper helper) {
        DimensionRestrictionScenarios.demicreatorWingsDimensionRestrictionAllowsCloseCast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.remote_eye_isolated")
    public static void remoteEyeDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeDimensionDenylistRejectsCurrentDimension(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.remote_eye_isolated")
    public static void remoteEyeDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeDimensionAllowlistRequiresCurrentDimension(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.remote_eye_isolated")
    public static void remoteEyeDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeDimensionDenylistOverridesAllowlist(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.remote_eye_isolated")
    public static void remoteEyeSanitizerKeepsStoredLongDuration(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeSanitizerKeepsStoredLongDuration(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.remote_eye_isolated")
    public static void remoteEyeSanitizerUsesLegacyFallbackWhenDurationMissing(GameTestHelper helper) {
        DimensionRestrictionScenarios.remoteEyeSanitizerUsesLegacyFallbackWhenDurationMissing(helper);
    }
}
