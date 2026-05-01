package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDeskFeatureState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ApprenticeDeskItem extends BlockItem {
    public ApprenticeDeskItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        if (!ApprenticeDeskFeatureState.areNonJobSiteFeaturesDisabled()) {
            return;
        }

        lines.add(Component.translatable("item.apprenticecodex.apprentice_desk.disabled_tooltip")
                .withStyle(ChatFormatting.YELLOW));
    }
}
