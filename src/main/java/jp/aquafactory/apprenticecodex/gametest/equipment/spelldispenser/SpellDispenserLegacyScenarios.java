package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.integration.create.CreateDispenserTestSupport;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserLegacyScenarios {
    private SpellDispenserLegacyScenarios() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsSingleMagicMissileScroll(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserValidatorAcceptsSingleMagicMissileScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsNonIronsNamespaceScroll(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserValidatorAcceptsNonIronsNamespaceScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsLongScroll(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserValidatorAcceptsLongScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsUnprofiledRayOfSiphoning(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserValidatorRejectsUnprofiledRayOfSiphoning(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsContinuousScroll(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserValidatorRejectsContinuousScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsProfiledContinuousScroll(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserValidatorAcceptsProfiledContinuousScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsRecastScroll(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserValidatorRejectsRecastScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsNonScrollSpellContainer(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserValidatorRejectsNonScrollSpellContainer(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserIsTaggedForContraptionControls(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserIsTaggedForContraptionControls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserOwnerProfilePersistsThroughNbt(GameTestHelper helper) {
        CreateDispenserTestSupport.spellDispenserOwnerProfilePersistsThroughNbt(helper);
    }
}
