package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.storage;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserStorageGameTests {
    private SpellDispenserStorageGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(GameTestHelper helper) {
        SpellDispenserStorageScenarios.spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSneakPlacementStartsAtZeroMana(GameTestHelper helper) {
        SpellDispenserStorageScenarios.spellDispenserSneakPlacementStartsAtZeroMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserOwnerProfileCanBeReadFromSavedTag(GameTestHelper helper) {
        SpellDispenserStorageScenarios.spellDispenserOwnerProfileCanBeReadFromSavedTag(helper);
    }
}
