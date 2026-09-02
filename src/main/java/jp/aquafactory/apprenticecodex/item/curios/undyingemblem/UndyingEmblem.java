package jp.aquafactory.apprenticecodex.item.curios.undyingemblem;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class UndyingEmblem extends Item implements ICurioItem{
    private final String slotIdentifier;

    public UndyingEmblem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        slotIdentifier = CuriosSlotConstants.CHARM;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        var result = new java.util.ArrayList<>(tooltips);
        result.add(Component.empty());
        result.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_1"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_2"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_3"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_4"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));

        result.add(Component.empty());

        var remainingTicks = getClientRemainingCooldownTicks();
        var currentStatus = remainingTicks > 0
                ? Component.translatable(
                        getDescriptionId() + ".status.reconstruct",
                        Utils.timeFromTicks(remainingTicks, 1)
                ).withStyle(ChatFormatting.DARK_AQUA)
                : Component.translatable(getDescriptionId() + ".status.ready").withStyle(ChatFormatting.GREEN);
        result.add(Component.translatable(getDescriptionId() + ".status.label", currentStatus));
        return result;
    }

    private static int getClientRemainingCooldownTicks() {
        return FMLEnvironment.dist == Dist.CLIENT
                ? UndyingEmblemClientState.getRemainingCooldownTicks()
                : 0;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
