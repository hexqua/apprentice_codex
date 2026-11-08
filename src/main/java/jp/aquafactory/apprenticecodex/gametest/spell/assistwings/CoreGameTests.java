package jp.aquafactory.apprenticecodex.gametest.spell.assistwings;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void assistWingsOnlyJumpItemsTagIncludesSmashcastScepter(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsOnlyJumpItemsTagIncludesSmashcastScepter(helper);
    }
}
