package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSpellThrowableCardGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ApprenticeCodexSpellThrowableCardGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void spellThrowableCardsAcceptOnlySupportedImpactProfilesAndAllowedRecasts(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellThrowableCardsAcceptOnlySupportedImpactProfilesAndAllowedRecasts(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellThrowableCardWorkbenchRecipesImbueFromScrollWithoutConsumingScroll(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellThrowableCardWorkbenchRecipesImbueFromScrollWithoutConsumingScroll(helper);
    }
}
