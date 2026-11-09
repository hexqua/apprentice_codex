package jp.aquafactory.apprenticecodex.gametest.integration.malum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MalumLegacyScenarios {
    private MalumLegacyScenarios() {}

    @GameTest(template = TEMPLATE)
    public static void malumHauntedBonusResolvesFromSupportedMainhandWeapons(GameTestHelper helper) {
        MalumLegacyScenariosSupport.malumHauntedBonusResolvesFromSupportedMainhandWeapons(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void malumHauntedBonusUsesDedicatedDamageType(GameTestHelper helper) {
        MalumLegacyScenariosSupport.malumHauntedBonusUsesDedicatedDamageType(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magicDamageTagActuallyScalesWithLodestoneMagicProficiency(GameTestHelper helper) {
        MalumLegacyScenariosSupport.magicDamageTagActuallyScalesWithLodestoneMagicProficiency(helper);
    }
}
