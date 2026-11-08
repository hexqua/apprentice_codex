package jp.aquafactory.apprenticecodex.gametest.integration.malum;

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
    public static void tinyLumberjackRecognizesMalumRunewoodAndSoulwoodLogs(GameTestHelper helper) {
        MalumLogScenarios.tinyLumberjackRecognizesMalumRunewoodAndSoulwoodLogs(helper);
    }
}
