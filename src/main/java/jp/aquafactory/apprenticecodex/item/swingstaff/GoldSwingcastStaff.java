package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class GoldSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final double IMBUED_SPELL_POWER_BONUS = 0.15D;

    private static final SwingcastStaffTier TIER = createTier(
            Rarity.UNCOMMON,
            22,
            3.0D,
            2.0D,
            instantOnlyCastTypes(),
            SwingcastCooldownMode.IMBUED_ONLY,
            bonus((Holder<Attribute>) AttributeRegistry.SPELL_POWER, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
    );

    public GoldSwingcastStaff() {
        super("gold_swingcast_staff", TIER);
    }

    @Override
    protected boolean addStackDependentModifiers(
            ItemAttributeModifiers.Builder builder,
            ItemStack stack,
            String modifierSeedPrefix
    ) {
        return addImbuedSchoolSpellPowerModifier(builder, stack, modifierSeedPrefix, IMBUED_SPELL_POWER_BONUS);
    }
}
