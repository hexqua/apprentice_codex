package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.IronsArtificeCompat;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellboundArmsModuleGameTests {
    private SpellboundArmsModuleGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellbound_arms")
    public static void powerStacksAndSchoolSelection(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        SpellboundArmsModuleGameTestScenarios.powerStacksAndSchoolSelection(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellbound_arms")
    public static void resistanceAndFiredSnapshot(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        SpellboundArmsModuleGameTestScenarios.resistanceAndFiredSnapshot(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellbound_arms")
    public static void nestedSoulDamageAndChildProfiles(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        SpellboundArmsModuleGameTestScenarios.nestedSoulDamageAndChildProfiles(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellbound_arms")
    public static void combinedManaIsAtomic(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        SpellboundArmsModuleGameTestScenarios.combinedManaIsAtomic(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellbound_arms")
    public static void tooltipsColorsAndConfigSync(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        SpellboundArmsModuleGameTestScenarios.tooltipsColorsAndConfigSync(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellbound_arms")
    public static void malumExposureIncludesLethalHits(GameTestHelper helper) {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        SpellboundArmsModuleGameTestScenarios.malumExposureIncludesLethalHits(helper);
    }
}
