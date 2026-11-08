package jp.aquafactory.apprenticecodex.gametest.equipment.offhand;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.rightclickweapon.RightClickMagicWeaponGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RightClickWeaponContractGameTests {
    private RightClickWeaponContractGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponTooltipsStartWithOffhandPriorityHint(GameTestHelper helper) {
        RightClickMagicWeaponGameTests.rightClickMagicWeaponTooltipsStartWithOffhandPriorityHint(helper);
    }
}
