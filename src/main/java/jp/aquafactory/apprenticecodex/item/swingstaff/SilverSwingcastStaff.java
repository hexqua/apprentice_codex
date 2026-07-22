package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;

public class SilverSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.UNCOMMON,
            18,
            3.0D,
            allNonContinuousCastTypes(),
            SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
            RecastTypes.NoRecastRestriction,
            bonus(AttributeRegistry.SPELL_POWER, 0.05, AttributeModifier.Operation.MULTIPLY_BASE),
            bonus(AttributeRegistry.MAX_MANA, 50, AttributeModifier.Operation.ADDITION));

    public SilverSwingcastStaff() {
        super("silver_swingcast_staff", TIER);
    }
}
