package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class ManaShieldCharm extends Item implements ICurioItem {
    private final String slotIdentifier;

    public ManaShieldCharm() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = CuriosSlotConstants.CHARM;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        ManaShieldCharmLogic.onCurioTick(slotContext);
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        tooltips.add(Component.empty());
        tooltips.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
        tooltips.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_1"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        tooltips.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_2"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        tooltips.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_3"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
        return tooltips;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
