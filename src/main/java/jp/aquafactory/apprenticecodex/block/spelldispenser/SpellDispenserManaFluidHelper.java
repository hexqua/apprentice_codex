package jp.aquafactory.apprenticecodex.block.spelldispenser;

import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public final class SpellDispenserManaFluidHelper {
    public static final int CAPACITY_MB = 1000;
    public static final int DOSE_MB = 250;

    // optional MOD のクラスを直接参照せず、液体IDと 1.21.1 の PotionContents component だけで判定する。
    private static final ResourceLocation IRONS_POTION_FLUID = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "potion");
    private static final ResourceLocation CREATE_POTION_FLUID = ResourceLocation.fromNamespaceAndPath("create", "potion");
    private static final ResourceLocation IMMERSIVE_ENGINEERING_POTION_FLUID =
            ResourceLocation.fromNamespaceAndPath("immersiveengineering", "potion");

    private SpellDispenserManaFluidHelper() {
    }

    public static boolean isSupportedManaPotionFluid(@NotNull FluidStack fluidStack) {
        return getManaRecovery(fluidStack) > 0;
    }

    public static int getManaRecovery(@NotNull FluidStack fluidStack) {
        var potionStack = createRegularPotionItem(fluidStack);
        return potionStack.isEmpty() ? 0 : SpellDispenserManaHelper.getManaPotionRecovery(potionStack);
    }

    public static boolean isSameFluidAndTags(@NotNull FluidStack first, @NotNull FluidStack second) {
        return FluidStack.isSameFluidSameComponents(first, second);
    }

    private static @NotNull ItemStack createRegularPotionItem(@NotNull FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var fluidId = BuiltInRegistries.FLUID.getKey(fluidStack.getFluid());
        if (IRONS_POTION_FLUID.equals(fluidId)) {
            return createIronsRegularPotionFromComponents(fluidStack);
        }
        if (CREATE_POTION_FLUID.equals(fluidId) || IMMERSIVE_ENGINEERING_POTION_FLUID.equals(fluidId)) {
            return createPotionFromComponents(fluidStack);
        }

        return ItemStack.EMPTY;
    }

    private static @NotNull ItemStack createIronsRegularPotionFromComponents(@NotNull FluidStack fluidStack) {
        var bottleType = fluidStack.getOrDefault(ComponentRegistry.POTION_BOTTLE_TYPE, PotionFluid.BottleType.REGULAR);
        return bottleType == PotionFluid.BottleType.REGULAR ? createPotionFromComponents(fluidStack) : ItemStack.EMPTY;
    }

    private static @NotNull ItemStack createPotionFromComponents(@NotNull FluidStack fluidStack) {
        var potionContents = fluidStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (!potionContents.hasEffects()) {
            return ItemStack.EMPTY;
        }

        var stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, potionContents);
        return stack;
    }
}
