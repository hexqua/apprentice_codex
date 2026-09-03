package jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear;

import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterFlightManager;
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

public class ManaManeuverGear extends Item implements ICurioItem {
    final String slotIdentifier;

    public ManaManeuverGear() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = CuriosSlotConstants.FEET;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        if (slotIdentifier != null) {
            // Curiosっぽい共通ヘッダ.
            result.add(Component.empty());
            result.add(Component.translatable("curios.modifiers." + this.slotIdentifier).withStyle(ChatFormatting.GOLD));

            // 本体.
            result.add(Component.literal(" ")
                    .append(Component.translatable(
                            getDescriptionId() + ".desc_1",
                            ImbueTooltipHelper.getJumpKeyName()
                    ))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ").append(Component.translatable(getDescriptionId() + ".desc_2")).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ").append(Component.translatable(getDescriptionId() + ".desc_3")).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }

        return result;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
