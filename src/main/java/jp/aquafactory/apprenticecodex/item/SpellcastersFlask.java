package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.item.consumables.FireAleItem;
import io.redspace.ironsspellbooks.item.consumables.NetherwardTinctureItem;
import io.redspace.ironsspellbooks.item.consumables.SimpleElixir;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SpellcastersFlask extends Item {
    private static final int MAX_STORED_DOSES = 8;
    private static final int MILLIBUCKETS_PER_DOSE = 250;
    private static final float DRINK_DURATION_MULTIPLIER = 1.0F;
    private static final float EFFECT_DURATION_MULTIPLIER = 1.0F;
    private static final float EFFECT_LEVEL_MULTIPLIER = 1.0F;
    private static final int BAR_COLOR = 0x4F88E8;
    private static final int DEFAULT_TINT_COLOR = 0xFFFFFFFF;
    private static final int FIRE_ALE_TINT_COLOR = 0xFF8F3028;
    private static final String STORAGE_TAG = "SpellcastersFlask";
    private static final String STORED_ITEM_TAG = "StoredItem";
    private static final String STORED_DOSES_TAG = "StoredDoses";

    public SpellcastersFlask() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var storedItem = getStoredItem(stack);
        if (storedItem.isEmpty()) {
            return super.getName(stack);
        }

        return Component.translatable(
                "item.apprenticecodex.flask_system.filled_name",
                super.getName(stack),
                storedItem.getHoverName()
        );
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context) {
        var level = context.getLevel();
        var blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof AlchemistCauldronTile cauldronTile) || cauldronTile.fluidInventory == null) {
            return InteractionResult.PASS;
        }

        var importPreview = previewTransfer(level, stack, cauldronTile.fluidInventory.getFluidInTank(0));
        if (importPreview != null) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            applyTransfer(stack, cauldronTile, importPreview);
            return InteractionResult.CONSUME;
        }

        var exportPreview = previewExport(level, stack, cauldronTile);
        if (exportPreview != null) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            applyExport(stack, cauldronTile, exportPreview);
            return InteractionResult.CONSUME;
        }

        var player = context.getPlayer();
        if (player != null && canConsumeStoredItem(stack)) {
            player.startUsingItem(context.getHand());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (!canConsumeStoredItem(stack)) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return Math.max(1, Math.round(32.0F * DRINK_DURATION_MULTIPLIER));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                              @NotNull LivingEntity livingEntity) {
        var extractedEffects = extractStoredEffects(stack);
        if (extractedEffects.isEmpty()) {
            return stack;
        }

        if (livingEntity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        if (!level.isClientSide) {
            for (var effect : extractedEffects) {
                livingEntity.addEffect(scaleEffect(effect));
            }
            decrementDoseAndClearIfEmpty(stack);
        }

        if (livingEntity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        livingEntity.gameEvent(GameEvent.DRINK);
        return stack;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);

        var storedItem = getStoredItem(stack);
        appendStoredEffectTooltips(lines, storedItem);
        lines.add(Component.empty());
        lines.add(createStoredAmountTooltipLine(stack));
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

    public static int getItemTintColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0 || !isFilled(stack)) {
            return DEFAULT_TINT_COLOR;
        }

        return getStoredItemTintColor(getStoredItem(stack));
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

    private static int getStoredItemTintColor(ItemStack storedItem) {
        if (storedItem.isEmpty()) {
            return DEFAULT_TINT_COLOR;
        }

        if (storedItem.getItem() instanceof FireAleItem) {
            return FIRE_ALE_TINT_COLOR;
        }

        var effects = extractEffectsFromItem(storedItem);
        if (effects.isEmpty()) {
            return DEFAULT_TINT_COLOR;
        }

        // PotionUtils の混色ロジックは使わず、仕様として抽出順の先頭効果色だけを表示に使う。
        return withFullAlpha(effects.get(0).getEffect().getColor());
    }

    private static int withFullAlpha(int rgbColor) {
        return 0xFF000000 | rgbColor;
    }

    private static boolean canConsumeStoredItem(ItemStack stack) {
        return getStoredDoseCount(stack) > 0 && !extractStoredEffects(stack).isEmpty();
    }

    private static List<MobEffectInstance> extractStoredEffects(ItemStack flaskStack) {
        return extractEffectsFromItem(getStoredItem(flaskStack));
    }

    private static List<MobEffectInstance> extractEffectsFromItem(ItemStack storedItem) {
        if (storedItem.isEmpty()) {
            return List.of();
        }

        if (storedItem.getItem() instanceof PotionItem) {
            return PotionUtils.getMobEffects(storedItem);
        }

        if (storedItem.getItem() instanceof SimpleElixir simpleElixir) {
            return List.of(simpleElixir.getMobEffect());
        }

        if (storedItem.getItem() instanceof FireAleItem) {
            return List.of(
                    new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 3, false, true, true),
                    new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 45, 0, false, true, true),
                    new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 45, 2, false, true, true)
            );
        }

        if (storedItem.getItem() instanceof NetherwardTinctureItem) {
            return List.of(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20));
        }

        return List.of();
    }

    private static MobEffectInstance scaleEffect(MobEffectInstance originalEffect) {
        var scaledDuration = originalEffect.getEffect().isInstantenous()
                ? originalEffect.getDuration()
                : Math.max(1, Math.round(originalEffect.getDuration() * EFFECT_DURATION_MULTIPLIER));
        var originalLevel = originalEffect.getAmplifier() + 1;
        var scaledAmplifier = Math.max(0, Math.round(originalLevel * EFFECT_LEVEL_MULTIPLIER) - 1);
        return new MobEffectInstance(
                originalEffect.getEffect(),
                scaledDuration,
                scaledAmplifier,
                originalEffect.isAmbient(),
                originalEffect.isVisible(),
                originalEffect.showIcon()
        );
    }

    private static void appendStoredEffectTooltips(List<Component> lines, ItemStack storedItem) {
        var effectLines = createStoredEffectTooltipLines(storedItem);
        if (effectLines.isEmpty()) {
            return;
        }

        lines.add(Component.translatable("item.apprenticecodex.flask_system.effects")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.addAll(effectLines);
    }

    private static List<Component> createStoredEffectTooltipLines(ItemStack storedItem) {
        if (storedItem.isEmpty()) {
            return List.of();
        }

        var effects = extractEffectsFromItem(storedItem);
        if (effects.isEmpty()) {
            return List.of();
        }

        var lines = new ArrayList<Component>(effects.size());
        for (var effect : effects) {
            lines.add(formatEffectTooltipLine(scaleEffect(effect)));
        }
        return lines;
    }

    private static Component formatEffectTooltipLine(MobEffectInstance effect) {
        var effectColor = getEffectTooltipColor(effect);
        MutableComponent line = Component.literal("- ").withStyle(effectColor)
                .append(effect.getEffect().getDisplayName().copy().withStyle(effectColor));
        if (effect.getAmplifier() > 0) {
            line.append(Component.literal(" ").withStyle(effectColor))
                    .append(Component.translatable("potion.potency." + effect.getAmplifier()).withStyle(effectColor));
        }

        if (!effect.getEffect().isInstantenous()) {
            line.append(Component.literal(" ").withStyle(effectColor))
                    .append(Component.literal(formatEffectDuration(effect)).withStyle(effectColor));
        }

        return line;
    }

    private static ChatFormatting getEffectTooltipColor(MobEffectInstance effect) {
        return switch (effect.getEffect().getCategory()) {
            case HARMFUL -> ChatFormatting.RED;
            case NEUTRAL -> ChatFormatting.GREEN;
            case BENEFICIAL -> ChatFormatting.BLUE;
        };
    }

    private static Component createStoredAmountTooltipLine(ItemStack stack) {
        return Component.empty()
                .append(Component.translatable("item.apprenticecodex.flask_system.amount_label")
                        .withStyle(ChatFormatting.GOLD))
                .append(Component.literal(Integer.toString(getStoredDoseCount(stack)))
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("/").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(Integer.toString(MAX_STORED_DOSES))
                        .withStyle(ChatFormatting.YELLOW));
    }

    private static String formatEffectDuration(MobEffectInstance effect) {
        return StringUtil.formatTickDuration(effect.getDuration());
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

    @Nullable
    private static ExportPreview previewExport(Level level, ItemStack flaskStack, AlchemistCauldronTile cauldronTile) {
        if (cauldronTile.getFluidAmount() > 0 || getStoredDoseCount(flaskStack) <= 0) {
            return null;
        }

        var storedItem = getStoredItem(flaskStack);
        if (storedItem.isEmpty()) {
            return null;
        }

        var preview = createExportPreview(level, storedItem);
        if (preview == null) {
            return null;
        }

        var exportableDoseCount = Math.min(
                getStoredDoseCount(flaskStack),
                cauldronTile.fluidInventory.getTankCapacity(0) / MILLIBUCKETS_PER_DOSE
        );
        if (exportableDoseCount <= 0) {
            return null;
        }

        var exportFluid = preview.singleDoseFluid().copy();
        exportFluid.setAmount(exportableDoseCount * MILLIBUCKETS_PER_DOSE);

        var fillAmount = cauldronTile.fluidInventory.fill(exportFluid, IFluidHandler.FluidAction.SIMULATE);
        if (fillAmount <= 0) {
            return null;
        }

        var appliedDoseCount = fillAmount / MILLIBUCKETS_PER_DOSE;
        if (appliedDoseCount <= 0) {
            return null;
        }

        exportFluid.setAmount(appliedDoseCount * MILLIBUCKETS_PER_DOSE);
        return new ExportPreview(exportFluid, appliedDoseCount, preview.fillSound);
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

    @Nullable
    private static ExportPreview createExportPreview(Level level, ItemStack storedItem) {
        var recipePreview = createExportPreviewFromRecipe(level, storedItem);
        if (recipePreview != null) {
            return recipePreview;
        }

        var potionFluid = PotionFluid.from(storedItem);
        if (potionFluid.isEmpty()) {
            return null;
        }

        return new ExportPreview(potionFluid, 1, SoundEvents.BOTTLE_EMPTY);
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

    @Nullable
    private static ExportPreview createExportPreviewFromRecipe(Level level, ItemStack storedItem) {
        var recipe = level.getRecipeManager().getRecipeFor(
                RecipeRegistry.ALCHEMIST_CAULDRON_FILL_TYPE.get(),
                new SimpleContainer(storedItem),
                level
        );
        return recipe.map(
                fillAlchemistCauldronRecipe -> new ExportPreview(fillAlchemistCauldronRecipe.result(),
                        1,
                        fillAlchemistCauldronRecipe.fillSound().value())).orElse(null);
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
        if (!isSupportedStoredItem(representativeItem)) {
            return ItemStack.EMPTY;
        }

        var normalizedItem = representativeItem.copy();
        normalizedItem.setCount(1);
        return normalizedItem;
    }

    private static boolean isSupportedStoredItem(ItemStack stack) {
        var item = stack.getItem();
        return item instanceof PotionItem
                || item instanceof SimpleElixir
                || item instanceof FireAleItem
                || item instanceof NetherwardTinctureItem;
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

    private static void applyExport(ItemStack flaskStack, AlchemistCauldronTile cauldronTile, ExportPreview preview) {
        var filledAmount = cauldronTile.fluidInventory.fill(preview.fluidStack, IFluidHandler.FluidAction.EXECUTE);
        if (filledAmount != preview.fluidStack.getAmount()) {
            return;
        }

        decrementDoseAndClearIfEmpty(flaskStack, preview.doseCount);
        cauldronTile.setChanged();
        if (cauldronTile.getLevel() != null) {
            cauldronTile.getLevel().playSound(null, cauldronTile.getBlockPos(), preview.fillSound, SoundSource.BLOCKS);
        }
    }

    private static void decrementDoseAndClearIfEmpty(ItemStack flaskStack) {
        decrementDoseAndClearIfEmpty(flaskStack, 1);
    }

    private static void decrementDoseAndClearIfEmpty(ItemStack flaskStack, int consumedDoseCount) {
        var storageTag = flaskStack.getTagElement(STORAGE_TAG);
        if (storageTag == null) {
            return;
        }

        var remainingDoseCount = Math.max(0, getStoredDoseCount(flaskStack) - consumedDoseCount);
        if (remainingDoseCount <= 0) {
            flaskStack.removeTagKey(STORAGE_TAG);
            return;
        }

        storageTag.putInt(STORED_DOSES_TAG, remainingDoseCount);
    }

    private record TransferPreview(ItemStack representativeItem, int transferableDoseCount) {
    }

    private record ExportPreview(FluidStack fluidStack, int doseCount, SoundEvent fillSound) {
        private FluidStack singleDoseFluid() {
            var fluid = fluidStack.copy();
            fluid.setAmount(MILLIBUCKETS_PER_DOSE);
            return fluid;
        }
    }
}
