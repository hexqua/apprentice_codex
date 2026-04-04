package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class SilverSpellAmplifier extends AbstractSpellAmplifierItem {
    public SilverSpellAmplifier() {
        super(
                Rarity.UNCOMMON,
                "silver_spell_amplifier",
                bonus(AttributeRegistry.MAX_MANA, 150, AttributeModifier.Operation.ADD_VALUE)
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 18;
    }
}
