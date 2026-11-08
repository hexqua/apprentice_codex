package jp.aquafactory.apprenticecodex.gametest.equipment.chargedtwinbladestaff;

import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

final class TwinBladeDamageTestSupport {
    private TwinBladeDamageTestSupport() {}

    static void assertChargedTwinBladeStaffThrownDamage(
            GameTestHelper helper,
            ItemStack stack,
            Entity target,
            DamageSource damageSource,
            String failureMessage
    ) {
        var level = helper.getLevel();
        var expectedDamage = EnchantmentHelper.modifyDamage(level, stack, target, damageSource, (float) ChargedTwinBladeStaff.resolveThrownDamage(stack));
        var actualDamage = ChargedTwinBladeStaff.resolveThrownDamage(level, stack, target, damageSource);
        helper.assertTrue(Math.abs(actualDamage - expectedDamage) < 1.0e-9D,
                failureMessage + ": target=" + EntityType.getKey(target.getType()) + ", expected=" + expectedDamage + ", actual=" + actualDamage);
    }
}
