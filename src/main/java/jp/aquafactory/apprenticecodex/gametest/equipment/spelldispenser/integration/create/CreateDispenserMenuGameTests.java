package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CreateDispenserMenuGameTests {
    private CreateDispenserMenuGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserMountedMenuKeepsContraptionInventoryAccessible(GameTestHelper helper) {
        CreateDispenserMenuScenarios.spellDispenserMountedMenuKeepsContraptionInventoryAccessible(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserMountedMenuTreatsOwnerOptionalSpellAsReadyWithoutOwnerProfile(GameTestHelper helper) {
        CreateDispenserMenuScenarios.spellDispenserMountedMenuTreatsOwnerOptionalSpellAsReadyWithoutOwnerProfile(helper);
    }
}
