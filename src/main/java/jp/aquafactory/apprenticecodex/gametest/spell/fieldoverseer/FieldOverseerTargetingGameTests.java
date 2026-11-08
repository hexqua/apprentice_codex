package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.FIELD_OVERSEER_ATTACK_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FieldOverseerTargetingGameTests {
    private FieldOverseerTargetingGameTests() {}

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_ATTACK_BATCH, timeoutTicks = 100)
    public static void fieldOverseerPrioritizesHealthAndTransfersMana(GameTestHelper helper) {
        FieldOverseerTargetingScenarios.fieldOverseerPrioritizesHealthAndTransfersMana(helper);
    }
}
