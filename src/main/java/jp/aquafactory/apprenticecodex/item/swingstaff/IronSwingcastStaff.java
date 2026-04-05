package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;

public class IronSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.COMMON,
            14,
            5.0D,
            instantOnlyCastTypes(),
            SwingcastCooldownMode.IMBUED_ONLY,
            bonus(AttributeRegistry.SPELL_POWER, 0.1, AttributeModifier.Operation.MULTIPLY_BASE)
    );

    public IronSwingcastStaff() {
        super("iron_swingcast_staff", TIER);
    }
}
