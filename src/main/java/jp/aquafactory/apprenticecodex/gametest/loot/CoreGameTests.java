package jp.aquafactory.apprenticecodex.gametest.loot;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.runtime.CoreGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void genericLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        LootScenarios.genericLootIncludesApprenticeCurioBonusDrops(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void bonusChestLootIncludesIsekaiTravelGuidebook(GameTestHelper helper) {
        LootScenarios.bonusChestLootIncludesIsekaiTravelGuidebook(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void nonLootableApprenticeSpellsAreExcludedFromDefaultSpellFilter(GameTestHelper helper) {
        LootScenarios.nonLootableApprenticeSpellsAreExcludedFromDefaultSpellFilter(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void genericMagicTreasureLootDoesNotGenerateBlockedApprenticeScrolls(GameTestHelper helper) {
        LootScenarios.genericMagicTreasureLootDoesNotGenerateBlockedApprenticeScrolls(helper);
    }
}
