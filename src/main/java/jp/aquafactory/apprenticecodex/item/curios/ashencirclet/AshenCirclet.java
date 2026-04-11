package jp.aquafactory.apprenticecodex.item.curios.ashencirclet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class AshenCirclet extends Item implements ICurioItem, IPresetSpellContainer {
    private static final AttributeContainer[] CIRCLET_ATTRIBUTES = {
            new AttributeContainer(
                    AttributeRegistry.MAX_MANA,
                    100,
                    AttributeModifier.Operation.ADDITION
            ),
            new AttributeContainer(
                    AttributeRegistry.MANA_REGEN,
                    0.20D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ),
            new AttributeContainer(
                    () -> Attributes.ATTACK_DAMAGE,
                    -0.20D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            )
    };

    public AshenCirclet() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, true));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        // 右クリックや専用メニューを持たないCurioでも、入手直後からImbue枠が使えるようにする。
        initializeSpellContainer(stack);
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        var baseModifiers = ICurioItem.super.getAttributeModifiers(slotContext, uuid, stack);
        if (!CuriosSlotConstants.HEAD.equals(slotContext.identifier())) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);

        var modifierSlotName = String.format("%s_%s", CuriosSlotConstants.HEAD, slotContext.index());
        for (var attributeContainer : CIRCLET_ATTRIBUTES) {
            builder.put(attributeContainer.attribute().get(), attributeContainer.createModifier(modifierSlotName));
        }
        return builder.build();
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                net.minecraft.world.item.@NotNull TooltipFlag flag) {
        initializeSpellContainer(stack);
        super.appendHoverText(stack, level, lines, flag);
    }
}
