package jp.aquafactory.apprenticecodex.gametest.storage.personalshelf;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.PERSONAL_SHELF_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = PERSONAL_SHELF_ISOLATED_BATCH)
    public static void personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(GameTestHelper helper) {
        PersonalShelfScenarios.personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(helper);
    }

    @GameTest(template = TEMPLATE, batch = PERSONAL_SHELF_ISOLATED_BATCH)
    public static void personalShelfSynchronizesExportModeBlockState(GameTestHelper helper) {
        PersonalShelfScenarios.personalShelfSynchronizesExportModeBlockState(helper);
    }

    @GameTest(template = TEMPLATE, batch = PERSONAL_SHELF_ISOLATED_BATCH, timeoutTicks = 60)
    public static void personalShelfExpireClosesOpenedChestMenu(GameTestHelper helper) {
        PersonalShelfScenarios.personalShelfExpireClosesOpenedChestMenu(helper);
    }
}
