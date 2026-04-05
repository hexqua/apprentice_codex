package jp.aquafactory.apprenticecodex.item.swingstaff;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class GoldSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final double IMBUED_SPELL_POWER_BONUS = 0.15D;

    private static final SwingcastStaffTier TIER = createTier(
            Rarity.UNCOMMON,
            22,
            3.0D,
            allNonContinuousCastTypes(),
            SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
            bonus(AttributeRegistry.SPELL_POWER, 0.05, AttributeModifier.Operation.MULTIPLY_BASE)
    );

    public GoldSwingcastStaff() {
        super("gold_swingcast_staff", TIER);
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
