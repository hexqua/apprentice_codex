package jp.aquafactory.apprenticecodex.item.swingstaff;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class CopperSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final double IMBUED_SPELL_POWER_BONUS = 0.15D;

    private static final SwingcastStaffTier TIER = createTier(
            Rarity.COMMON,
            14,
            3.0D,
            instantOnlyCastTypes(),
            SwingcastCooldownMode.IMBUED_ONLY,
            RecastTypes.RequireZeroRecast,
            bonus(AttributeRegistry.SPELL_POWER, 0.05, AttributeModifier.Operation.MULTIPLY_BASE));

    public CopperSwingcastStaff() {
        super("copper_swingcast_staff", TIER, SpellRegistry.BALL_LIGHTNING_SPELL, 1);
    }

    @Override
    protected boolean addStackDependentModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack,
            String modifierSeedPrefix
    ) {
        return addImbuedSchoolSpellPowerModifier(builder, stack, modifierSeedPrefix, IMBUED_SPELL_POWER_BONUS);
    }
}
