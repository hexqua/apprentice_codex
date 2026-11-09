package jp.aquafactory.apprenticecodex.gametest.spell.worldflatter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.MINING_SPELL_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class WorldFlatterGameTests {
    private WorldFlatterGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void worldFlatterPenetratedArmorEffectAndDamageTags(GameTestHelper helper) {
        WorldFlatterScenarios.worldFlatterPenetratedArmorEffectAndDamageTags(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(GameTestHelper helper) {
        WorldFlatterScenarios.worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(GameTestHelper helper) {
        WorldFlatterScenarios.worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(helper);
    }
}
