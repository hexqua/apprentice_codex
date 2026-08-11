package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluids;
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

    public static int getTotalFluidAmount(@NotNull AlchemistCauldronTile cauldronTile) {
        if (cauldronTile.fluidInventory == null) {
            return 0;
        }

        var amount = 0;
        for (var tank = 0; tank < cauldronTile.fluidInventory.getTanks(); ++tank) {
            amount += cauldronTile.fluidInventory.getFluidInTank(tank).getAmount();
        }
        return amount;
    }

    public static boolean containsOnlyWater(@NotNull AlchemistCauldronTile cauldronTile) {
        if (cauldronTile.fluidInventory == null) {
            return false;
        }

        for (var tank = 0; tank < cauldronTile.fluidInventory.getTanks(); ++tank) {
            var fluid = cauldronTile.fluidInventory.getFluidInTank(tank);
            if (!fluid.isEmpty() && fluid.getFluid() != Fluids.WATER) {
                return false;
            }
        }
        return true;
    }

    public static int fillWater(@NotNull AlchemistCauldronTile cauldronTile, int amountMb,
                                @NotNull IFluidHandler.FluidAction action) {
        if (cauldronTile.fluidInventory == null || amountMb <= 0) {
            return 0;
        }
        return cauldronTile.fluidInventory.fill(new FluidStack(Fluids.WATER, amountMb), action);
    }
}
