package jp.aquafactory.apprenticecodex.item.offhand;

import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

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
            ItemAttributeModifiers.Builder builder,
            ItemStack stack,
            String modifierKeyPrefix
    ) {
        var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
        var imbuedSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
        if (imbuedSpellPowerAttribute == null) {
            return false;
        }

        addStackDependentModifier(
                builder,
                imbuedSpellPowerAttribute,
                IMBUED_SPELL_POWER_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                modifierKeyPrefix + "_imbued_spell_power"
        );
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }
}
