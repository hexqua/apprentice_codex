package jp.aquafactory.apprenticecodex.gametest.equipment.attributes;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void swingMagicWeaponsUseBaseAttackModifierIds(GameTestHelper helper) {
        EquipmentAttributeScenarios.swingMagicWeaponsUseBaseAttackModifierIds(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void mainhandUpgradeBridgeAppliesStoredUpgradeDataToSpellGunsAndWeapons(GameTestHelper helper) {
        EquipmentAttributeScenarios.mainhandUpgradeBridgeAppliesStoredUpgradeDataToSpellGunsAndWeapons(helper);
    }
}
