package jp.aquafactory.apprenticecodex.item.broom;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomConfigState;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.FloatmountBroomItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public final class FloatmountBroomItem extends AbstractBroomItem {
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            createCalibrationAdjustmentProfile(CalibrationAdjustmentRule.unique(
                    "adapt_underwater_mobility",
                    stack -> stack.is(Items.HEART_OF_THE_SEA),
                    CalibrationAdjustmentHints.heartOfTheSea()
            ).withEffectLines(CalibrationAdjustmentEffects.adaptUnderwaterMobility()));

    @Override
    protected AbstractBroomEntity createBroom(Level level) {
        return new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_PROFILE;
    }

    public static boolean isAquaticCalibrationEnabled(@NotNull ItemStack broomStack) {
        if (!(broomStack.getItem() instanceof FloatmountBroomItem)) {
            return false;
        }
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (CalibrationAdjustmentStorage.get(broomStack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT)
                    .is(Items.HEART_OF_THE_SEA)) {
                return true;
            }
        }
        return false;
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
