package jp.aquafactory.apprenticecodex.gametest.equipment.throwablecard;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSpellThrowableCardGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ApprenticeCodexSpellThrowableCardGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void spellThrowableCardsAcceptOnlySupportedImpactProfilesAndAllowedRecasts(GameTestHelper helper) {
        ThrowableCardScenarios.spellThrowableCardsAcceptOnlySupportedImpactProfilesAndAllowedRecasts(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellThrowableCardWorkbenchRecipesImbueFromScrollWithoutConsumingScroll(GameTestHelper helper) {
        ThrowableCardScenarios.spellThrowableCardWorkbenchRecipesImbueFromScrollWithoutConsumingScroll(helper);
    }
}
