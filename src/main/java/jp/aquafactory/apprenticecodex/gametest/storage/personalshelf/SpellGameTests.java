package jp.aquafactory.apprenticecodex.gametest.storage.personalshelf;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.personal_shelf_isolated")
    public static void personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(GameTestHelper helper) {
        PersonalShelfScenarios.personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.personal_shelf_isolated")
    public static void personalShelfSynchronizesExportModeBlockState(GameTestHelper helper) {
        PersonalShelfScenarios.personalShelfSynchronizesExportModeBlockState(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.personal_shelf_isolated", timeoutTicks = 60)
    public static void personalShelfExpireClosesOpenedChestMenu(GameTestHelper helper) {
        PersonalShelfScenarios.personalShelfExpireClosesOpenedChestMenu(helper);
    }
}
