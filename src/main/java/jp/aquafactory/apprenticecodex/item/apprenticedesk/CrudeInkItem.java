package jp.aquafactory.apprenticecodex.item.apprenticedesk;

import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CrudeInkItem extends Item {
    public static final SpellRarity RARITY = SpellRarity.COMMON;

    public CrudeInkItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @NotNull TooltipContext context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.apprenticecodex.crude_ink.desc")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.apprenticecodex.crude_ink.single_use")
                .withStyle(ChatFormatting.GRAY));
    }
}
