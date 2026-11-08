package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowOverheatGameTests {
    private ElementalBowOverheatGameTests() {}

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        ElementalBowOverheatScenarios.elementalBowSuppressesElementalArrowCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowConsumesAdditionalManaWhileOverheated(GameTestHelper helper) {
        ElementalBowOverheatScenarios.elementalBowConsumesAdditionalManaWhileOverheated(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowSharesOverheatAcrossSpellsAndBows(GameTestHelper helper) {
        ElementalBowOverheatScenarios.elementalBowSharesOverheatAcrossSpellsAndBows(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH, timeoutTicks = 80)
    public static void elementalBowOverheatRefreshesDurationAfterRepeatedCast(GameTestHelper helper) {
        ElementalBowOverheatScenarios.elementalBowOverheatRefreshesDurationAfterRepeatedCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_MANA_CONFIG_BATCH)
    public static void elementalBowAdditionalManaUsesServerConfig(GameTestHelper helper) {
        ElementalBowOverheatScenarios.elementalBowAdditionalManaUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_DURATION_CONFIG_BATCH)
    public static void elementalBowOverheatDurationUsesServerConfig(GameTestHelper helper) {
        ElementalBowOverheatScenarios.elementalBowOverheatDurationUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowClampsPersistedFutureOverheat(GameTestHelper helper) {
        ElementalBowOverheatScenarios.elementalBowClampsPersistedFutureOverheat(helper);
    }
}
