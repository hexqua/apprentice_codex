package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.EnumSet;

public class IronSpellcasterGun extends AbstractSpellGunItem {
    private static final SpellGunConfig SPELL_GUN_CONFIG = new SpellGunConfig(
            EnumSet.of(SpellGunCastType.INSTANT),
            20 * 10,
            true,
            10,
            null
    );

    public IronSpellcasterGun() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.COMMON),
                SPELL_GUN_CONFIG,
                SpellRegistry.MAGIC_MISSILE_SPELL,
                1,
                "IronSpellcasterGun",
                bonus(AttributeRegistry.SPELL_POWER, 0.10, AttributeModifier.Operation.MULTIPLY_BASE)
        );
    }
    
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }
}
