package jp.aquafactory.apprenticecodex.item.broom;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
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
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            createCalibrationAdjustmentProfile(
                    CalibrationAdjustmentRule.unique(
                            "overdrive_engine_hoverride",
                            stack -> stack.is(ItemRegistry.OVERDRIVE_BROOM_ENGINE.get()),
                            CalibrationAdjustmentHints.overdriveBroomEngine()
                    ).withEffectLines(CalibrationAdjustmentEffects.overdriveHoverrideBroom()),
                    CalibrationAdjustmentRule.unique(
                            "twilight_gale_hoverride",
                            stack -> stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.TWILIGHT_GALE.get()),
                            CalibrationAdjustmentHints.twilightGale()
                    ).withEffectLines(CalibrationAdjustmentEffects.addRushStyle())
            );

    @Override
    protected AbstractBroomEntity createBroom(Level level) {
        return new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), level);
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_PROFILE;
    }

    public static boolean isRushStyleEnabled(@NotNull ItemStack broomStack) {
        if (!(broomStack.getItem() instanceof HoverrideBroomItem)) {
            return false;
        }
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (CalibrationAdjustmentStorage.get(broomStack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT)
                    .is(io.redspace.ironsspellbooks.registries.ItemRegistry.TWILIGHT_GALE.get())) {
                return true;
            }
        }
        return false;
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
