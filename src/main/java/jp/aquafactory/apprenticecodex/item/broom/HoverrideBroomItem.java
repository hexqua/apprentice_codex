package jp.aquafactory.apprenticecodex.item.broom;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.HoverrideBroomItemRenderer;
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

public final class HoverrideBroomItem extends AbstractBroomItem {
    @Override
    protected AbstractBroomEntity createBroom(Level level) {
        return new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), level);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        appendPlacementAndRecoveryTooltip(tooltipComponents, "item.apprenticecodex.hoverride_broom");
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.hoverride_broom.desc_3"
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.hoverride_broom.desc_4"
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HoverrideBroomItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new HoverrideBroomItemRenderer();
                }
                return renderer;
            }
        });
    }
}
