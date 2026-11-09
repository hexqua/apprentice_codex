package jp.aquafactory.apprenticecodex.gametest.spell.longstride;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.enchantment.attribute.LegacyAttributeGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LongStrideMovementGameTests {
    private LongStrideMovementGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void longStrideBuoyancyRisesWhileSneakingAndPreservesHorizontalMovement(GameTestHelper helper) {
        LegacyAttributeGameTests.longStrideBuoyancyRisesWhileSneakingAndPreservesHorizontalMovement(helper);
    }
}
