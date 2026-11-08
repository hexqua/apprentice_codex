package jp.aquafactory.apprenticecodex.gametest.equipment.shield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ReflectcastShieldCalibrationGameTests {
    private ReflectcastShieldCalibrationGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldCastRestrictionsFollowCalibration(GameTestHelper helper) {
        ReflectcastShieldCalibrationScenarios.reflectcastShieldCastRestrictionsFollowCalibration(helper);
    }

    @SuppressWarnings("unchecked")
    static List<Component> collectReflectcastAbilityTooltipLines(
            GameTestHelper helper,
            ReflectcastShield item,
            ItemStack stack
    ) {
        try {
            var method = ReflectcastShield.class
                    .getDeclaredMethod("getImbueShieldAbilityTooltipSection", ItemStack.class);
            method.setAccessible(true);
            return (List<Component>) method.invoke(item, stack);
        } catch (ReflectiveOperationException exception) {
            helper.fail("Reflectcast Shield ability tooltip reflection failed: " + exception);
            return List.of();
        }
    }
}
