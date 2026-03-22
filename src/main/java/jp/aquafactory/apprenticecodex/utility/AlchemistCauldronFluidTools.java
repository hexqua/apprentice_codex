package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class AlchemistCauldronFluidTools {
    private AlchemistCauldronFluidTools() {}

    @Nullable
    public static FluidStack findFirstFluidFromTop(@NotNull AlchemistCauldronTile cauldronTile,
                                                   @NotNull Predicate<FluidStack> predicate) {
        if (cauldronTile.fluidInventory == null) {
            return null;
        }

        // Cauldron の drain(int) は後段タンクから処理するため、こちらも上層側を優先する。
        for (var tank = cauldronTile.fluidInventory.getTanks() - 1; tank >= 0; --tank) {
            var fluidStack = cauldronTile.fluidInventory.getFluidInTank(tank);
            if (!fluidStack.isEmpty() && predicate.test(fluidStack)) {
                return fluidStack.copy();
            }
        }

        return null;
    }

    public static @NotNull FluidStack drainMatchingFluid(@NotNull AlchemistCauldronTile cauldronTile,
                                                         @NotNull FluidStack fluidStack,
                                                         int amountMb,
                                                         @NotNull IFluidHandler.FluidAction action) {
        if (cauldronTile.fluidInventory == null || fluidStack.isEmpty() || amountMb <= 0) {
            return FluidStack.EMPTY;
        }

        // 複数種が混在する鍋では量指定 drain だと対象外の液体が減るため、FluidStack を指定する。
        var requestedFluid = fluidStack.copy();
        requestedFluid.setAmount(amountMb);
        return cauldronTile.fluidInventory.drain(requestedFluid, action);
    }
}
