package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.variant;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.CREATIVE_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CreativeSpellDispenserGameTests {
    private CreativeSpellDispenserGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserPlacementStartsAtZeroManaAndSkipsOwnerProfile(GameTestHelper helper) {
        CreativeSpellDispenserScenarios.creativeSpellDispenserPlacementStartsAtZeroManaAndSkipsOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserIgnoresOwnerProfileNbt(GameTestHelper helper) {
        CreativeSpellDispenserScenarios.creativeSpellDispenserIgnoresOwnerProfileNbt(helper);
    }

    @GameTest(template = TEMPLATE, batch = CREATIVE_CONFIG_BATCH)
    public static void creativeSpellDispenserUsesCreativeManaConfig(GameTestHelper helper) {
        CreativeSpellDispenserScenarios.creativeSpellDispenserUsesCreativeManaConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = CREATIVE_CONFIG_BATCH)
    public static void creativeSpellDispenserAllowsZeroCooldownMultiplier(GameTestHelper helper) {
        CreativeSpellDispenserScenarios.creativeSpellDispenserAllowsZeroCooldownMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserDropsNoStoredItems(GameTestHelper helper) {
        CreativeSpellDispenserScenarios.creativeSpellDispenserDropsNoStoredItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserRestrictsMenuAccess(GameTestHelper helper) {
        CreativeSpellDispenserScenarios.creativeSpellDispenserRestrictsMenuAccess(helper);
    }
}
