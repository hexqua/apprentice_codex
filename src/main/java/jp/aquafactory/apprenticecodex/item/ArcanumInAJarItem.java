package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArcanumInAJarItem extends BlockItem {
    public ArcanumInAJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if (ArcanumInAJarBlockEntity.getStoredParameterCount(stack) <= 0) {
            return;
        }

        lines.add(Component.translatable("block.apprenticecodex.arcanum_in_a_jar.fill")
                .withStyle(ChatFormatting.GRAY));
    }
}
