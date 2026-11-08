package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellgunEquipmentAttributeGameTests {
    private SpellgunEquipmentAttributeGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunsRemoveBaseSpellPowerButKeepSurge(GameTestHelper helper) {
        SpellgunEquipmentAttributeScenarios.spellcasterGunsRemoveBaseSpellPowerButKeepSurge(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void silverSpellAmplifierMovesAllSpellgunAttributesToOffhand(GameTestHelper helper) {
        SpellgunEquipmentAttributeScenarios.silverSpellAmplifierMovesAllSpellgunAttributesToOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void silverSpellAmplifierKeepsDualSpellgunModifiersIndependent(GameTestHelper helper) {
        SpellgunEquipmentAttributeScenarios.silverSpellAmplifierKeepsDualSpellgunModifiersIndependent(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void silverSpellAmplifierMovesUpgradeOrbModifiersToOffhand(GameTestHelper helper) {
        SpellgunEquipmentAttributeScenarios.silverSpellAmplifierMovesUpgradeOrbModifiersToOffhand(helper);
    }
}
