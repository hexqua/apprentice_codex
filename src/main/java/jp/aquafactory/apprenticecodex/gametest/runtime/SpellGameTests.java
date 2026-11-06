package jp.aquafactory.apprenticecodex.gametest.runtime;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void ownSpellsUniqueInfoAcceptsNullCaster(GameTestHelper helper) {
        RegistrationScenarios.ownSpellsUniqueInfoAcceptsNullCaster(helper);
    }
}
