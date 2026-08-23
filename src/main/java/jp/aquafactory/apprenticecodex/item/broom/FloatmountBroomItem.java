package jp.aquafactory.apprenticecodex.item.broom;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomConfigState;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.FloatmountBroomItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public final class FloatmountBroomItem extends AbstractBroomItem {
    @Override
    protected AbstractBroomEntity createBroom(Level level) {
        return new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
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

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private FloatmountBroomItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new FloatmountBroomItemRenderer();
                }
                return renderer;
            }
        });
    }
}
