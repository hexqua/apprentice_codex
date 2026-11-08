package jp.aquafactory.apprenticecodex.gametest.runtime;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void ownSpellsUniqueInfoAcceptsNullCaster(GameTestHelper helper) {
        RegistrationScenarios.ownSpellsUniqueInfoAcceptsNullCaster(helper);
    }
}
