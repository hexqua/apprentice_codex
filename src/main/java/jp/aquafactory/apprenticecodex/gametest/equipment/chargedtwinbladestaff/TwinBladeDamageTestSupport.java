package jp.aquafactory.apprenticecodex.gametest.equipment.chargedtwinbladestaff;

import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;

final class TwinBladeDamageTestSupport {
    private TwinBladeDamageTestSupport() {}

    static void assertChargedTwinBladeStaffThrownDamage(
            GameTestHelper helper,
            ItemStack stack,
            MobType mobType,
            double expectedDamage,
            String failureMessage
    ) {
        var actualDamage = ChargedTwinBladeStaff.resolveThrownDamage(stack, mobType);
        helper.assertTrue(Math.abs(actualDamage - expectedDamage) < 1.0e-9D,
                failureMessage + ": mobType=" + mobType + ", expected=" + expectedDamage + ", actual=" + actualDamage);
    }
}
