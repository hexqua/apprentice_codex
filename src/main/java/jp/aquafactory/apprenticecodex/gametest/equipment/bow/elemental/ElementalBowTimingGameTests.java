package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowTimingGameTests {
    private ElementalBowTimingGameTests() {}

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        ElementalBowTimingScenarios.elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_DRAW_CONFIG_BATCH)
    public static void elementalBowMagicDrawTicksUseProfileAndServerMultiplier(GameTestHelper helper) {
        ElementalBowTimingScenarios.elementalBowMagicDrawTicksUseProfileAndServerMultiplier(helper);
    }
}
