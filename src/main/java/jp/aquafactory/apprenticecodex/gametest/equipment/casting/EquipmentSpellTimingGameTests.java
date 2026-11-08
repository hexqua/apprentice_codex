package jp.aquafactory.apprenticecodex.gametest.equipment.casting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.craftsmansdelight.CraftsmansDelightLegacyScenarios;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.EQUIPMENT_SPELL_TIMING_MULTIPLIER_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentSpellTimingGameTests {
    private EquipmentSpellTimingGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightScrollcasterGauntletCooldownUsesSwordMultiplier(GameTestHelper helper) {
        CraftsmansDelightLegacyScenarios.craftsmansDelightScrollcasterGauntletCooldownUsesSwordMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magiAgentSuitBootsCooldownReducesTargetSpell(GameTestHelper helper) {
        EquipmentSpellTimingScenarios.magiAgentSuitBootsCooldownReducesTargetSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void equipmentCooldownReductionsDoNotStack(GameTestHelper helper) {
        EquipmentSpellTimingScenarios.equipmentCooldownReductionsDoNotStack(helper);
    }

    @GameTest(template = TEMPLATE, batch = EQUIPMENT_SPELL_TIMING_MULTIPLIER_CONFIG_BATCH)
    public static void equipmentSpellTimingMultipliersFollowServerConfigAndKeepOneTickMinimum(GameTestHelper helper) {
        EquipmentSpellTimingScenarios.equipmentSpellTimingMultipliersFollowServerConfigAndKeepOneTickMinimum(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magiAgentSuitBootsCooldownPreservesExistingAdditiveCooldown(GameTestHelper helper) {
        EquipmentSpellTimingScenarios.magiAgentSuitBootsCooldownPreservesExistingAdditiveCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void strongestLimitedBaseCooldownSelectionIgnoresStacking(GameTestHelper helper) {
        EquipmentSpellTimingScenarios.strongestLimitedBaseCooldownSelectionIgnoresStacking(helper);
    }
}
