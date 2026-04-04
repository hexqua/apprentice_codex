package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;

public class GoldSpellAmplifier extends AbstractSpellAmplifierItem {
    public GoldSpellAmplifier() {
        super(
                Rarity.UNCOMMON,
                "gold_spell_amplifier",
                bonus(AttributeRegistry.COOLDOWN_REDUCTION, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                bonus(AttributeRegistry.CAST_TIME_REDUCTION, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 22;
    }
}
