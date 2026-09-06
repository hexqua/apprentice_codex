package jp.aquafactory.apprenticecodex.gametest.malum;

import com.sammy.malum.core.handlers.SoulDataHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class MalumScytheGameTestHelper {
    private MalumScytheGameTestHelper() {
    }

    public static ItemStack getScytheWeapon(DamageSource source, LivingEntity attacker) {
        return SoulDataHandler.getScytheWeapon(source, attacker);
    }
}
