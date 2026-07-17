package jp.aquafactory.apprenticecodex.item.curios.circlets;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
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
        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        builder.putAll(baseModifiers);

        // Curios が保証する slot-unique ID を使い、増設枠や汎用枠でも modifier を衝突させない。
        // Iron's の AttributeContainer は受け取った文字列を ResourceLocation の path に連結するため、
        // Curios の namespace 区切りをそのまま渡さず、有効な path 文字だけへ正規化する。
        var modifierSlotName = (id.getNamespace() + "_" + id.getPath()).replace('/', '_');
        for (var attributeContainer : circletAttributes) {
            builder.put(attributeContainer.attribute(), attributeContainer.createModifier(modifierSlotName));
        }
        addAdditionalModifiers(builder, stack, id);
        return builder.build();
    }

    protected void addAdditionalModifiers(
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder,
            ItemStack stack,
            ResourceLocation slotId
    ) {
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        initializeSpellContainer(stack);
        super.appendHoverText(stack, context, lines, flag);
    }
}
