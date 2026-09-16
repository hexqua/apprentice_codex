package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class ManaSoulTransducer extends Item implements ICurioItem{
    private final String slotIdentifier;

    public ManaSoulTransducer() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        slotIdentifier = CuriosSlotConstants.CHARM;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        result.add(Component.empty());
        result.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
        // desc_1～3は1.20.1ではlegacy.desc_1～3にする.
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_1"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_2"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_3"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        ManaSoulTransducerTooltip.appendStatus(result, getDescriptionId());
        return result;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
