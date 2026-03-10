package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class IronSpellcasterDevice extends AbstractSpellGunItem {
    private static final SpellGunConfig SPELL_GUN_CONFIG = new SpellGunConfig(
            EnumSet.of(SpellGunCastType.INSTANT),
            30 * 20,
            true,
            10,
            null
    );

    public IronSpellcasterDevice() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.COMMON),
                SPELL_GUN_CONFIG,
                SpellRegistry.MAGIC_MISSILE_SPELL,
                1,
                "iron_spellcaster_device",
                bonus(AttributeRegistry.SPELL_POWER, 0.10, AttributeModifier.Operation.MULTIPLY_BASE)
        );
    }

    @Override
    public @Nullable Item getAmmoItem(ItemStack stack, @Nullable SpellData spellData) {
        if (spellData == null) {
            return null;
        }

        var spellRarity = spellData.getSpell().getRarity(spellData.getLevel());
        if (spellRarity.getValue() <= SpellRarity.RARE.getValue()) {
            return Items.GUNPOWDER;
        }

        return super.getAmmoItem(stack, spellData);
    }
}
