package jp.aquafactory.apprenticecodex.item.broom;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomConfigState;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FloatmountBroomItem extends AbstractBroomItem {
    @Override
    protected AbstractBroomEntity createBroom(Level level) {
        return new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        appendPlacementAndRecoveryTooltip(tooltipComponents, "item.apprenticecodex.floatmount_broom");
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.floatmount_broom.desc_3",
                Component.literal(Integer.toString(FloatmountBroomConfigState.normalFlightManaThreshold()))
                        .withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.floatmount_broom.desc_4"
        ).withStyle(ChatFormatting.GRAY));
    }
}
