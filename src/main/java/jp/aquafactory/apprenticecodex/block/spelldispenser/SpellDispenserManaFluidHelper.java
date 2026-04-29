package jp.aquafactory.apprenticecodex.block.spelldispenser;

import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class SpellDispenserManaFluidHelper {
    public static final int CAPACITY_MB = 1000;
    public static final int DOSE_MB = 250;

    // optional MOD のクラスを直接参照せず、1.20.1 で確認した液体IDとNBT形式だけで判定する。
    private static final ResourceLocation IRONS_POTION_FLUID = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "potion");
    private static final ResourceLocation CREATE_POTION_FLUID = ResourceLocation.fromNamespaceAndPath("create", "potion");
    private static final ResourceLocation IMMERSIVE_ENGINEERING_POTION_FLUID =
            ResourceLocation.fromNamespaceAndPath("immersiveengineering", "potion");
    private static final String IRONS_BOTTLE_TYPE_TAG = "irons_spellbooks:bottle_type";
    private static final String CREATE_BOTTLE_TYPE_TAG = "Bottle";

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
        return first.isFluidEqual(second) && FluidStack.areFluidStackTagsEqual(first, second);
    }

    private static @NotNull ItemStack createRegularPotionItem(@NotNull FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var fluidId = ForgeRegistries.FLUIDS.getKey(fluidStack.getFluid());
        if (IRONS_POTION_FLUID.equals(fluidId) && isRegularBottleType(fluidStack.getTag(), IRONS_BOTTLE_TYPE_TAG)) {
            return createPotionFromTag(fluidStack.getTag());
        }
        if (CREATE_POTION_FLUID.equals(fluidId) && isRegularBottleType(fluidStack.getTag(), CREATE_BOTTLE_TYPE_TAG)) {
            return createPotionFromTag(fluidStack.getTag());
        }
        if (IMMERSIVE_ENGINEERING_POTION_FLUID.equals(fluidId)) {
            return createPotionFromTag(fluidStack.getTag());
        }

        return ItemStack.EMPTY;
    }

    private static boolean isRegularBottleType(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key, Tag.TAG_STRING)) {
            return true;
        }

        return "REGULAR".equals(tag.getString(key).toUpperCase(Locale.ROOT));
    }

    private static @NotNull ItemStack createPotionFromTag(CompoundTag tag) {
        if (tag == null) {
            return ItemStack.EMPTY;
        }

        var potion = PotionUtils.getPotion(tag);
        var customEffects = PotionUtils.getCustomEffects(tag);
        if (potion == Potions.EMPTY && customEffects.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var effects = PotionUtils.getAllEffects(tag);
        if (effects.isEmpty() || !MobEffectRegistry.INSTANT_MANA.isPresent()
                || effects.stream().anyMatch(effect -> effect.getEffect() != MobEffectRegistry.INSTANT_MANA.get())) {
            return ItemStack.EMPTY;
        }

        var stack = new ItemStack(Items.POTION);
        if (potion != Potions.EMPTY) {
            PotionUtils.setPotion(stack, potion);
        }
        if (!customEffects.isEmpty()) {
            PotionUtils.setCustomEffects(stack, customEffects);
        }
        return stack;
    }
}
