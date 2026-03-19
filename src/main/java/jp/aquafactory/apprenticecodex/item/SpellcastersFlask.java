package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.item.consumables.DrinkableItem;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.FillAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellcastersFlask extends Item {
    private static final int MAX_STORED_DOSES = 8;
    private static final int MILLIBUCKETS_PER_DOSE = 250;
    private static final int BAR_COLOR = 0x4F88E8;
    private static final String STORAGE_TAG = "SpellcastersFlask";
    private static final String STORED_ITEM_TAG = "StoredItem";
    private static final String STORED_DOSES_TAG = "StoredDoses";

    public SpellcastersFlask() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context) {
        var level = context.getLevel();
        var blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof AlchemistCauldronTile cauldronTile) || cauldronTile.fluidInventory == null) {
            return InteractionResult.PASS;
        }

        var preview = previewTransfer(level, stack, cauldronTile.fluidInventory.getFluidInTank(0));
        if (preview == null) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        applyTransfer(stack, cauldronTile, preview);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);

        var storedItem = getStoredItem(stack);
        if (!storedItem.isEmpty()) {
            lines.add(Component.translatable("item.apprenticecodex.flask_system.kind", storedItem.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }

        lines.add(Component.translatable(
                "item.apprenticecodex.flask_system.amount",
                getStoredDoseCount(stack),
                MAX_STORED_DOSES
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return getStoredDoseCount(stack) > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var storedDoseCount = getStoredDoseCount(stack);
        return Math.max(1, Math.round(13.0F * storedDoseCount / (float) MAX_STORED_DOSES));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return BAR_COLOR;
    }

    public static boolean isFilled(ItemStack stack) {
        return getStoredDoseCount(stack) > 0;
    }

    public static int getStoredDoseCount(ItemStack stack) {
        var storageTag = stack.getTagElement(STORAGE_TAG);
        if (storageTag == null) {
            return 0;
        }

        return Math.max(0, Math.min(MAX_STORED_DOSES, storageTag.getInt(STORED_DOSES_TAG)));
    }

    public static ItemStack getStoredItem(ItemStack stack) {
        var storageTag = stack.getTagElement(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(STORED_ITEM_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }

        var storedItem = ItemStack.of(storageTag.getCompound(STORED_ITEM_TAG));
        return storedItem.isEmpty() ? ItemStack.EMPTY : storedItem;
    }

    @Nullable
    private static TransferPreview previewTransfer(Level level, ItemStack flaskStack, FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            return null;
        }

        var representativeItem = createRepresentativeItem(level, fluidStack);
        if (representativeItem.isEmpty()) {
            return null;
        }

        var storedDoseCount = getStoredDoseCount(flaskStack);
        var storedItem = getStoredItem(flaskStack);
        if (!storedItem.isEmpty() && !ItemStack.isSameItemSameTags(storedItem, representativeItem)) {
            return null;
        }

        var transferableDoseCount = Math.min(MAX_STORED_DOSES - storedDoseCount, fluidStack.getAmount() / MILLIBUCKETS_PER_DOSE);
        if (transferableDoseCount <= 0) {
            return null;
        }

        return new TransferPreview(representativeItem, transferableDoseCount);
    }

    private static ItemStack createRepresentativeItem(Level level, FluidStack fluidStack) {
        var representativeItem = createRepresentativeItemFromRecipe(level, fluidStack);
        if (!representativeItem.isEmpty()) {
            return representativeItem;
        }

        var sampleFluid = fluidStack.copy();
        sampleFluid.setAmount(MILLIBUCKETS_PER_DOSE);
        representativeItem = PotionFluid.from(sampleFluid);
        if (representativeItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return normalizeAcceptedItem(representativeItem);
    }

    private static ItemStack createRepresentativeItemFromRecipe(Level level, FluidStack fluidStack) {
        var sampleFluid = fluidStack.copy();
        sampleFluid.setAmount(MILLIBUCKETS_PER_DOSE);

        for (var recipe : level.getRecipeManager().getAllRecipesFor(RecipeRegistry.ALCHEMIST_CAULDRON_FILL_TYPE.get())) {
            if (!recipe.result().isFluidStackIdentical(sampleFluid)) {
                continue;
            }

            var representativeItem = findRepresentativeItem(recipe.input());
            if (!representativeItem.isEmpty()) {
                return representativeItem;
            }
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack findRepresentativeItem(Ingredient ingredient) {
        for (var candidate : ingredient.getItems()) {
            var normalizedItem = normalizeAcceptedItem(candidate);
            if (!normalizedItem.isEmpty()) {
                return normalizedItem;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack normalizeAcceptedItem(ItemStack representativeItem) {
        var item = representativeItem.getItem();
        if (!(item instanceof PotionItem) && !(item instanceof DrinkableItem)) {
            return ItemStack.EMPTY;
        }

        var normalizedItem = representativeItem.copy();
        normalizedItem.setCount(1);
        return normalizedItem;
    }

    private static void applyTransfer(ItemStack flaskStack, AlchemistCauldronTile cauldronTile, TransferPreview preview) {
        var transferredFluid = cauldronTile.fluidInventory.drain(
                preview.transferableDoseCount * MILLIBUCKETS_PER_DOSE,
                IFluidHandler.FluidAction.EXECUTE
        );
        var appliedDoseCount = transferredFluid.getAmount() / MILLIBUCKETS_PER_DOSE;
        if (appliedDoseCount <= 0) {
            return;
        }

        var storageTag = flaskStack.getOrCreateTagElement(STORAGE_TAG);
        storageTag.put(STORED_ITEM_TAG, preview.representativeItem.save(new CompoundTag()));
        storageTag.putInt(STORED_DOSES_TAG, Math.min(MAX_STORED_DOSES, getStoredDoseCount(flaskStack) + appliedDoseCount));
        cauldronTile.setChanged();
    }

    private record TransferPreview(ItemStack representativeItem, int transferableDoseCount) {
    }
}
