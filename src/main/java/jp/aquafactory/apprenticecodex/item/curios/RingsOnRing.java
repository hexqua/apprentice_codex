package jp.aquafactory.apprenticecodex.item.curios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.compat.Curios;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

public class RingsOnRing extends Item implements ICurioItem {
    public RingsOnRing() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, UUID id, ItemStack stack) {
        var modifiers = HashMultimap.<Attribute, AttributeModifier>create();
        // Cataclysm の複数形 rings は通常の ring と別枠。汎用枠へ移しても増設効果を得られないようにする。
        if (CuriosSlotConstants.RINGS.equals(slotContext.identifier())) {
            // Curios の slot-unique ID で、複数装備の加算と個別解除を標準の属性処理へ任せる。
            CuriosApi.addSlotModifier(modifiers, Curios.RING_SLOT, id, 1, AttributeModifier.Operation.ADDITION);
            modifiers.put(AttributeRegistry.MAX_MANA.get(),
                    new AttributeModifier(id, "Rings on Ring mana penalty", -50, AttributeModifier.Operation.ADDITION));
        }
        return modifiers;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
