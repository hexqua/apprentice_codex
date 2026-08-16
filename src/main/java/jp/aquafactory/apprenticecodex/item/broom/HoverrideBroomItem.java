package jp.aquafactory.apprenticecodex.item.broom;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class HoverrideBroomItem extends AbstractBroomItem {
    @Override
    protected AbstractBroomEntity createBroom(Level level) {
        return new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), level);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        appendPlacementAndRecoveryTooltip(tooltipComponents, "item.apprenticecodex.hoverride_broom");
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.hoverride_broom.desc_3"
        ).withStyle(ChatFormatting.GRAY));
    }
}
