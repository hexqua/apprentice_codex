package jp.aquafactory.apprenticecodex.gametest.equipment.reflectcastshield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword.SpellchargedGreatswordGameTestScenarios;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ReflectcastShieldLegacyScenarios {
    private ReflectcastShieldLegacyScenarios() {}

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldKeepsExpectedItemContract(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.reflectcastShieldKeepsExpectedItemContract(helper);
    }
}
