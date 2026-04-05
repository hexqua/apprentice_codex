package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;

public class NetheriteSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.RARE,
            15,
            7.0D,
            allNonContinuousCastTypes(),
            SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
            bonus(AttributeRegistry.SPELL_POWER, 0.1, AttributeModifier.Operation.MULTIPLY_BASE)
    );

    public NetheriteSwingcastStaff() {
        super("netherite_swingcast_staff", TIER);
    }
}
