package jp.aquafactory.apprenticecodex.item.offhand;

import com.google.common.collect.ImmutableMultimap;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;

public class CopperSpellAmplifier extends AbstractSpellAmplifierItem {
    private static final double IMBUED_SPELL_POWER_BONUS = 0.10D;

    public CopperSpellAmplifier() {
        super(
                SpellRegistry.SHOCK,
                1,
                Rarity.UNCOMMON,
                "copper_spell_amplifier"
        );
    }

    @Override
    protected boolean addStackDependentModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack,
            String modifierSeedPrefix
    ) {
        var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
        var imbuedSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
        if (imbuedSpellPowerAttribute == null) {
            return false;
        }

        addEquippedModifier(
                builder,
                imbuedSpellPowerAttribute,
                IMBUED_SPELL_POWER_BONUS,
                AttributeModifier.Operation.MULTIPLY_BASE,
                modifierSeedPrefix + ".imbued_spell_power"
        );
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }
}
