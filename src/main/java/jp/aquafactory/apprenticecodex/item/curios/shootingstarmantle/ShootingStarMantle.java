package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

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

public class ShootingStarMantle extends Item implements ICurioItem {
    final String slotIdentifier;

    public ShootingStarMantle() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        slotIdentifier = CuriosSlotConstants.BACK;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return MantleEnergy.read(stack).energy() < MantleEnergy.MAX; }

    @Override
    public int getBarWidth(ItemStack stack) { return Math.round(13F * MantleEnergy.read(stack).energy() / MantleEnergy.MAX); }

    @Override
    public int getBarColor(ItemStack stack) { return MantleEnergy.read(stack).recovering() ? 0xFF4400 : 0xFFEEDD; }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        if (slotIdentifier != null) {
            // Curiosっぽい共通ヘッダ.
            result.add(Component.empty());
            result.add(Component.translatable("curios.modifiers." + this.slotIdentifier).withStyle(ChatFormatting.GOLD));

            // 本体.
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_1"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_2"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_3"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }

        return result;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
