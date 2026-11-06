package jp.aquafactory.apprenticecodex.gametest.loot;

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
    public static void bonusChestLootIncludesIsekaiTravelGuidebook(GameTestHelper helper) {
        LootScenarios.bonusChestLootIncludesIsekaiTravelGuidebook(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void genericLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        LootScenarios.genericLootIncludesApprenticeCurioBonusDrops(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void ironsStructureLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        LootScenarios.ironsStructureLootIncludesApprenticeCurioBonusDrops(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void nonLootableApprenticeSpellsAreExcludedFromDefaultSpellFilter(GameTestHelper helper) {
        LootScenarios.nonLootableApprenticeSpellsAreExcludedFromDefaultSpellFilter(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void genericMagicTreasureLootDoesNotGenerateBlockedApprenticeScrolls(GameTestHelper helper) {
        LootScenarios.genericMagicTreasureLootDoesNotGenerateBlockedApprenticeScrolls(helper);
    }
}
