package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;

public class IronSpellAmplifier extends AbstractSpellAmplifierItem {
    public IronSpellAmplifier() {
        super(
                Rarity.COMMON,
                "iron_spell_amplifier",
                bonus(AttributeRegistry.MAX_MANA, 50.0, AttributeModifier.Operation.ADDITION)
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }
}
