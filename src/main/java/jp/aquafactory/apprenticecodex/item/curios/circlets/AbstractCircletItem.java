package jp.aquafactory.apprenticecodex.item.curios.circlets;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public abstract class AbstractCircletItem extends Item implements ICurioItem, IPresetSpellContainer {
    private final AttributeContainer[] circletAttributes;

    protected AbstractCircletItem(Rarity rarity, AttributeContainer... circletAttributes) {
        super(new Item.Properties().stacksTo(1).rarity(rarity));
        this.circletAttributes = circletAttributes;
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
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext,
            ResourceLocation id,
            ItemStack stack
    ) {
        var baseModifiers = ICurioItem.super.getAttributeModifiers(slotContext, id, stack);
        if (!CuriosSlotConstants.HEAD.equals(slotContext.identifier())) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        builder.putAll(baseModifiers);

        var modifierSlotName = String.format("%s_%s", CuriosSlotConstants.HEAD, slotContext.index());
        for (var attributeContainer : circletAttributes) {
            builder.put(attributeContainer.attribute(), attributeContainer.createModifier(modifierSlotName));
        }
        addAdditionalHeadModifiers(builder, slotContext, stack, modifierSlotName);
        return builder.build();
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext,
            UUID uuid,
            ItemStack stack
    ) {
        // 1.21.1 Curios は UUID 経路で属性を引く呼び出しが残っているため、
        // ResourceLocation 側の実装へ寄せて head 用補正が欠落しないようにする。
        return getAttributeModifiers(
                slotContext,
                ResourceLocation.withDefaultNamespace(uuid.toString()),
                stack
        );
    }

    protected void addAdditionalHeadModifiers(
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder,
            SlotContext slotContext,
            ItemStack stack,
            String modifierSlotName
    ) {
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        initializeSpellContainer(stack);
        super.appendHoverText(stack, context, lines, flag);
    }
}
