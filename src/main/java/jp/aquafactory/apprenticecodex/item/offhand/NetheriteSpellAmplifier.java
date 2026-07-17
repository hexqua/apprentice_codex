package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class NetheriteSpellAmplifier extends AbstractSpellAmplifierItem {
    public NetheriteSpellAmplifier() {
        super(
                Rarity.RARE,
                "netherite_spell_amplifier",
                true,
                bonus(AttributeRegistry.CASTING_MOVESPEED, 0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                bonus(Attributes.ARMOR, 4.0D, AttributeModifier.Operation.ADD_VALUE),
                bonus(Attributes.ARMOR_TOUGHNESS, 2.0D, AttributeModifier.Operation.ADD_VALUE)
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }
}
