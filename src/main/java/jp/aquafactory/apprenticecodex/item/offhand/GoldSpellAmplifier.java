package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;

public class GoldSpellAmplifier extends AbstractOffhandMagicItem {
    public GoldSpellAmplifier() {
        super(
                Rarity.UNCOMMON,
                "gold_spell_amplifier",
                bonus(AttributeRegistry.MANA_REGEN, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 22;
    }
}
