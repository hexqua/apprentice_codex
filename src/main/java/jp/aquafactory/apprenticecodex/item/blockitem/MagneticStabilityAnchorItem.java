package jp.aquafactory.apprenticecodex.item.blockitem;

import jp.aquafactory.apprenticecodex.block.magneticstabilityanchor.MagneticStabilityAnchorProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MagneticStabilityAnchorItem extends BlockItem {
    private static final String DESCRIPTION_KEY = "item.apprenticecodex.magnetic_stability_anchor.desc";

    public MagneticStabilityAnchorItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        var range = MagneticStabilityAnchorProtection.RANGE_DIAMETER;
        tooltipComponents.add(Component.translatable(DESCRIPTION_KEY, range, range, range)
                .withStyle(ChatFormatting.GRAY));
    }
}
