package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.casting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserContinuousCastGameTests {
    private SpellDispenserContinuousCastGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperStopsContinuousCastAtDurationCap(GameTestHelper helper) {
        SpellDispenserContinuousCastScenarios.spellDispenserCastHelperStopsContinuousCastAtDurationCap(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousBreathBindsProjectileToTrackedAnchor(GameTestHelper helper) {
        SpellDispenserContinuousCastScenarios.spellDispenserContinuousBreathBindsProjectileToTrackedAnchor(helper);
    }
}
