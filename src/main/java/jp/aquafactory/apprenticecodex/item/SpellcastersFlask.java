package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.item.consumables.FireAleItem;
import io.redspace.ironsspellbooks.item.consumables.NetherwardTinctureItem;
import io.redspace.ironsspellbooks.item.consumables.SimpleElixir;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.utility.AlchemistCauldronFluidTools;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpellcastersFlask extends Item {
    private static final HolderLookup.Provider SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final int BASE_MAX_STORED_DOSES = 8;
    private static final int MILLIBUCKETS_PER_DOSE = 250;
    private static final int ENCHANTMENT_VALUE = 10;
    private static final float BASE_DRINK_DURATION_TICKS = 32.0F;
    private static final float GUZZLE_REDUCTION_PER_LEVEL = 0.1F;
    private static final int GUZZLE_LEVEL_CAP = 9;
    private static final int LARGE_MUG_BONUS_PER_LEVEL = 2;
    private static final float RED_ENERGY_DURATION_BONUS_PER_LEVEL = 0.25F;
    private static final int BAR_COLOR = 0x4F88E8;
    private static final int DEFAULT_TINT_COLOR = 0xFFFFFFFF;
    private static final int FIRE_ALE_TINT_COLOR = 0xFF8F3028;
    private static final String STORAGE_TAG = "SpellcastersFlask";
    private static final String STORED_ITEM_TAG = "StoredItem";
    private static final String STORED_DOSES_TAG = "StoredDoses";
    private static final Set<ResourceLocation> SUPPORTED_FLASK_ENCHANTMENTS = Set.of(
            Enchantments.GUZZLE.location(),
            Enchantments.LARGE_MUG.location(),
            Enchantments.RED_ENERGY.location(),
            Enchantments.GLOW_ENERGY.location()
    );
    private static final String PARTICLES_SUPPRESSED_TAG = "ParticlesSuppressed";

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
        normalizeStoredDosesToCapacity(stack);

        var level = context.getLevel();
        var blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof AlchemistCauldronTile cauldronTile) || cauldronTile.fluidInventory == null) {
            return InteractionResult.PASS;
        }

        var importPreview = previewTransfer(level, stack, cauldronTile);
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
        normalizeStoredDosesToCapacity(stack);
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
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return Math.max(1, Math.round(BASE_DRINK_DURATION_TICKS * getDrinkDurationMultiplier(stack)));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                              @NotNull LivingEntity livingEntity) {
        normalizeStoredDosesToCapacity(stack);
        var extractedEffects = extractStoredEffects(stack);
        if (extractedEffects.isEmpty()) {
            return stack;
        }

        if (livingEntity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        if (!level.isClientSide) {
            for (var effect : extractedEffects) {
                applyScaledEffect(stack, livingEntity, effect);
            }
            decrementStoredDoseCount(stack);
        }

        if (livingEntity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        livingEntity.gameEvent(GameEvent.DRINK);
        return stack;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        var storedItem = getStoredItem(stack);
        appendStoredEffectTooltips(lines, stack, storedItem);
        appendSuppressedParticlesTooltip(lines, stack);
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
        return Math.max(1, Math.round(13.0F * storedDoseCount / (float) getMaxStoredDoseCount(stack)));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId,
                              boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!level.isClientSide) {
            normalizeStoredDosesToCapacity(stack);
        }
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        return enchantmentId != null && SUPPORTED_FLASK_ENCHANTMENTS.contains(enchantmentId);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    public static boolean isFilled(ItemStack stack) {
        return getStoredDoseCount(stack) > 0;
    }

    public static boolean isEffectParticlesSuppressed(ItemStack stack) {
        var storageTag = getStorageTag(stack);
        return storageTag != null && storageTag.getBoolean(PARTICLES_SUPPRESSED_TAG);
    }

    public static @NotNull ItemStack copyWithToggledEffectParticles(@NotNull ItemStack flaskStack) {
        if (!(flaskStack.getItem() instanceof SpellcastersFlask)) {
            return ItemStack.EMPTY;
        }

        var result = flaskStack.copy();
        setEffectParticlesSuppressed(result, !isEffectParticlesSuppressed(result));
        return result;
    }

    public static @NotNull ItemStack copyFilterItem(@NotNull ItemStack stack) {
        if (stack.getItem() instanceof SpellcastersFlask) {
            return normalizeAcceptedItem(getStoredItem(stack));
        }

        return normalizeAcceptedItem(stack);
    }

    public static int getItemTintColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0 || !isFilled(stack)) {
            return DEFAULT_TINT_COLOR;
        }

        return getStoredItemTintColor(getStoredItem(stack));
    }

    public static int getStoredItemTintColorForDisplay(@NotNull ItemStack storedItem) {
        return getStoredItemTintColor(storedItem);
    }

    public static int getStoredDoseCount(ItemStack stack) {
        return Math.min(getRawStoredDoseCount(stack), getMaxStoredDoseCount(stack));
    }

    public static int getMaxDoseCapacity(ItemStack stack) {
        return getMaxStoredDoseCount(stack);
    }

    public static ItemStack getStoredItem(ItemStack stack) {
        var storageTag = getStorageTag(stack);
        if (storageTag == null || !storageTag.contains(STORED_ITEM_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }

        var storedItem = ItemStack.parseOptional(SERIALIZATION_LOOKUP, storageTag.getCompound(STORED_ITEM_TAG));
        return storedItem.isEmpty() ? ItemStack.EMPTY : storedItem;
    }

    public static boolean canAddDoseFromItem(ItemStack flaskStack, ItemStack candidateStack) {
        var candidateItem = normalizeAcceptedItem(candidateStack);
        if (candidateItem.isEmpty()) {
            return false;
        }

        var storedDoseCount = getStoredDoseCount(flaskStack);
        if (storedDoseCount >= getMaxStoredDoseCount(flaskStack)) {
            return false;
        }

        if (storedDoseCount <= 0) {
            return true;
        }

        var storedItem = getStoredItem(flaskStack);
        return !storedItem.isEmpty() && ItemStack.isSameItemSameComponents(storedItem, candidateItem);
    }

    public static boolean matchesStoredItem(ItemStack flaskStack, ItemStack candidateStack) {
        var candidateItem = normalizeAcceptedItem(candidateStack);
        if (candidateItem.isEmpty()) {
            return false;
        }

        var storedItem = normalizeAcceptedItem(getStoredItem(flaskStack));
        return !storedItem.isEmpty() && ItemStack.isSameItemSameComponents(storedItem, candidateItem);
    }

    public static ItemStack copyWithAddedDose(ItemStack flaskStack, ItemStack candidateStack) {
        if (!canAddDoseFromItem(flaskStack, candidateStack)) {
            return ItemStack.EMPTY;
        }

        var result = flaskStack.copy();
        var storedDoseCount = getStoredDoseCount(result);
        setStoredState(result, normalizeAcceptedItem(candidateStack), storedDoseCount + 1);
        return result;
    }

    public static ItemStack copyWithAddedDoses(ItemStack flaskStack, ItemStack candidateStack, int addedDoseCount) {
        var candidateItem = normalizeAcceptedItem(candidateStack);
        if (candidateItem.isEmpty() || addedDoseCount <= 0) {
            return ItemStack.EMPTY;
        }

        var storedDoseCount = getStoredDoseCount(flaskStack);
        var targetDoseCount = Math.min(getMaxStoredDoseCount(flaskStack), storedDoseCount + addedDoseCount);
        if (targetDoseCount <= storedDoseCount) {
            return ItemStack.EMPTY;
        }

        if (storedDoseCount > 0 && !matchesStoredItem(flaskStack, candidateItem)) {
            return ItemStack.EMPTY;
        }

        var result = flaskStack.copy();
        setStoredState(result, candidateItem, targetDoseCount);
        return result;
    }

    public static ItemStack getTransferCraftingRemainder(ItemStack candidateStack) {
        if (candidateStack.hasCraftingRemainingItem()) {
            var remainder = candidateStack.getCraftingRemainingItem().copy();
            remainder.setCount(1);
            return remainder;
        }

        var item = candidateStack.getItem();
        if (item instanceof PotionItem || item instanceof SimpleElixir || item instanceof FireAleItem) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }

        return ItemStack.EMPTY;
    }

    public static boolean canExtractOneDose(ItemStack flaskStack) {
        return getStoredDoseCount(flaskStack) > 0 && !normalizeAcceptedItem(getStoredItem(flaskStack)).isEmpty();
    }

    public static ItemStack copyStoredItemForCrafting(ItemStack flaskStack) {
        if (!canExtractOneDose(flaskStack)) {
            return ItemStack.EMPTY;
        }

        var storedItem = getStoredItem(flaskStack).copy();
        storedItem.setCount(1);
        return storedItem;
    }

    public static ItemStack copyAfterExtractingOneDose(ItemStack flaskStack) {
        if (!canExtractOneDose(flaskStack)) {
            return ItemStack.EMPTY;
        }

        var result = flaskStack.copy();
        decrementStoredDoseCount(result, 1);
        return result;
    }

    public static ItemStack copyAfterExtractingDoses(ItemStack flaskStack, int extractedDoseCount) {
        if (!canExtractOneDose(flaskStack) || extractedDoseCount <= 0) {
            return ItemStack.EMPTY;
        }

        var result = flaskStack.copy();
        decrementStoredDoseCount(result, extractedDoseCount);
        return result;
    }

    public static @NotNull ItemStack resolveRepresentativeItem(@NotNull Level level, @NotNull FluidStack fluidStack) {
        return createRepresentativeItem(level, fluidStack);
    }

    public static @Nullable FluidStack createFluidForStoredItem(@NotNull Level level, @NotNull ItemStack storedItem,
                                                                int amountMb) {
        if (amountMb <= 0) {
            return null;
        }

        var preview = createExportPreview(level, storedItem);
        if (preview == null) {
            return null;
        }

        var fluidStack = preview.singleDoseFluid();
        fluidStack.setAmount(amountMb);
        return fluidStack;
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
        return withFullAlpha(effects.get(0).getEffect().value().getColor());
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
            var potionContents = storedItem.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (!potionContents.hasEffects()) {
                return List.of();
            }

            var effects = new ArrayList<MobEffectInstance>();
            potionContents.forEachEffect(effects::add);
            return effects;
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

    private static MobEffectInstance scaleEffect(ItemStack flaskStack, MobEffectInstance originalEffect) {
        var scaledDuration = originalEffect.getEffect().value().isInstantenous()
                ? originalEffect.getDuration()
                : Math.max(1, Math.round(originalEffect.getDuration() * getEffectDurationMultiplier(flaskStack)));
        var scaledAmplifier = Math.max(0, originalEffect.getAmplifier() + getGlowEnergyLevel(flaskStack));
        var visible = !isEffectParticlesSuppressed(flaskStack) && originalEffect.isVisible();
        return new MobEffectInstance(
                originalEffect.getEffect(),
                scaledDuration,
                scaledAmplifier,
                originalEffect.isAmbient(),
                visible,
                originalEffect.showIcon()
        );
    }

    private static void applyScaledEffect(ItemStack flaskStack, LivingEntity livingEntity,
                                          MobEffectInstance originalEffect) {
        var scaledEffect = scaleEffect(flaskStack, originalEffect);
        if (scaledEffect.getEffect().value().isInstantenous()) {
            // 即時効果は addEffect では発火しないため、PotionItem 相当の専用経路で適用する。
            scaledEffect.getEffect().value().applyInstantenousEffect(
                    livingEntity,
                    livingEntity,
                    livingEntity,
                    scaledEffect.getAmplifier(),
                    1.0D
            );
            return;
        }

        livingEntity.addEffect(scaledEffect);
    }

    private static void appendStoredEffectTooltips(List<Component> lines, ItemStack flaskStack, ItemStack storedItem) {
        var effectLines = createStoredEffectTooltipLines(flaskStack, storedItem);
        if (effectLines.isEmpty()) {
            return;
        }

        lines.add(Component.translatable("item.apprenticecodex.flask_system.effects")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.addAll(effectLines);
    }

    private static void appendSuppressedParticlesTooltip(List<Component> lines, ItemStack flaskStack) {
        if (!isEffectParticlesSuppressed(flaskStack)) {
            return;
        }

        lines.add(Component.translatable("item.apprenticecodex.flask_system.particles_suppressed")
                .withStyle(ChatFormatting.GRAY));
    }

    private static List<Component> createStoredEffectTooltipLines(ItemStack flaskStack, ItemStack storedItem) {
        if (storedItem.isEmpty()) {
            return List.of();
        }

        if (storedItem.getItem() instanceof FireAleItem) {
            return List.of(formatHiddenStoredEffectTooltipLine(storedItem));
        }

        var effects = extractEffectsFromItem(storedItem);
        if (effects.isEmpty()) {
            return List.of();
        }

        var lines = new ArrayList<Component>(effects.size());
        for (var effect : effects) {
            lines.add(formatEffectTooltipLine(scaleEffect(flaskStack, effect)));
        }
        return lines;
    }

    private static Component formatHiddenStoredEffectTooltipLine(ItemStack storedItem) {
        var effectColor = ChatFormatting.GREEN;
        return Component.literal("- ").withStyle(effectColor)
                .append(storedItem.getHoverName().copy().withStyle(effectColor))
                .append(Component.literal(" ??:??").withStyle(effectColor));
    }

    private static Component formatEffectTooltipLine(MobEffectInstance effect) {
        var effectColor = getEffectTooltipColor(effect);
        MutableComponent line = Component.literal("- ").withStyle(effectColor)
                .append(effect.getEffect().value().getDisplayName().copy().withStyle(effectColor));
        if (effect.getAmplifier() > 0) {
            line.append(Component.literal(" ").withStyle(effectColor))
                    .append(Component.literal(formatEffectLevel(effect)).withStyle(effectColor));
        }

        if (!effect.getEffect().value().isInstantenous()) {
            line.append(Component.literal(" ").withStyle(effectColor))
                    .append(Component.literal(formatEffectDuration(effect)).withStyle(effectColor));
        }

        return line;
    }

    private static ChatFormatting getEffectTooltipColor(MobEffectInstance effect) {
        return switch (effect.getEffect().value().getCategory()) {
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
                .append(Component.literal(Integer.toString(getMaxStoredDoseCount(stack)))
                        .withStyle(ChatFormatting.YELLOW));
    }

    private static String formatEffectDuration(MobEffectInstance effect) {
        return StringUtil.formatTickDuration(effect.getDuration(), 20.0F);
    }

    private static String formatEffectLevel(MobEffectInstance effect) {
        return toRomanNumeral(effect.getAmplifier() + 1);
    }

    private static String toRomanNumeral(int value) {
        if (value <= 0) {
            return Integer.toString(value);
        }

        var remaining = value;
        var builder = new StringBuilder();
        var numerals = new int[]{1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        var symbols = new String[]{"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        for (int i = 0; i < numerals.length; ++i) {
            while (remaining >= numerals[i]) {
                builder.append(symbols[i]);
                remaining -= numerals[i];
            }
        }
        return builder.toString();
    }

    @Nullable
    private static TransferPreview previewTransfer(Level level, ItemStack flaskStack, AlchemistCauldronTile cauldronTile) {
        var storedDoseCount = getStoredDoseCount(flaskStack);
        var storedItem = getStoredItem(flaskStack);
        var fluidStack = AlchemistCauldronFluidTools.findFirstFluidFromTop(cauldronTile, candidate -> {
            var representativeItem = createRepresentativeItem(level, candidate);
            if (representativeItem.isEmpty()) {
                return false;
            }

            if (storedDoseCount > 0 && !storedItem.isEmpty() && !ItemStack.isSameItemSameComponents(storedItem, representativeItem)) {
                return false;
            }

            return Math.min(
                    getMaxStoredDoseCount(flaskStack) - storedDoseCount,
                    candidate.getAmount() / MILLIBUCKETS_PER_DOSE
            ) > 0;
        });
        if (fluidStack == null || fluidStack.isEmpty()) {
            return null;
        }

        var representativeItem = createRepresentativeItem(level, fluidStack);
        if (representativeItem.isEmpty()) {
            return null;
        }

        var transferableDoseCount = Math.min(
                getMaxStoredDoseCount(flaskStack) - storedDoseCount,
                fluidStack.getAmount() / MILLIBUCKETS_PER_DOSE
        );
        if (transferableDoseCount <= 0) {
            return null;
        }

        var drainFluid = fluidStack.copy();
        drainFluid.setAmount(transferableDoseCount * MILLIBUCKETS_PER_DOSE);
        return new TransferPreview(representativeItem, drainFluid, transferableDoseCount);
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
            if (!FluidStack.matches(recipe.value().result(), sampleFluid)) {
                continue;
            }

            var representativeItem = findRepresentativeItem(recipe.value().input());
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
                new SingleRecipeInput(storedItem),
                level
        );
        return recipe.map(
                fillAlchemistCauldronRecipe -> new ExportPreview(fillAlchemistCauldronRecipe.value().result(),
                        1,
                        fillAlchemistCauldronRecipe.value().fillSound().value())).orElse(null);
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
        var transferredFluid = AlchemistCauldronFluidTools.drainMatchingFluid(
                cauldronTile,
                preview.drainFluid,
                preview.drainFluid.getAmount(),
                IFluidHandler.FluidAction.EXECUTE
        );
        var appliedDoseCount = transferredFluid.getAmount() / MILLIBUCKETS_PER_DOSE;
        if (appliedDoseCount <= 0) {
            return;
        }

        setStoredState(flaskStack, preview.representativeItem, getStoredDoseCount(flaskStack) + appliedDoseCount);
        cauldronTile.setChanged();
    }

    private static void applyExport(ItemStack flaskStack, AlchemistCauldronTile cauldronTile, ExportPreview preview) {
        var filledAmount = cauldronTile.fluidInventory.fill(preview.fluidStack, IFluidHandler.FluidAction.EXECUTE);
        if (filledAmount != preview.fluidStack.getAmount()) {
            return;
        }

        decrementStoredDoseCount(flaskStack, preview.doseCount);
        cauldronTile.setChanged();
        if (cauldronTile.getLevel() != null) {
            cauldronTile.getLevel().playSound(null, cauldronTile.getBlockPos(), preview.fillSound, SoundSource.BLOCKS);
        }
    }

    private static void decrementStoredDoseCount(ItemStack flaskStack) {
        decrementStoredDoseCount(flaskStack, 1);
    }

    private static void decrementStoredDoseCount(ItemStack flaskStack, int consumedDoseCount) {
        var storedItem = getStoredItem(flaskStack);
        if (storedItem.isEmpty()) {
            clearStoredState(flaskStack);
            return;
        }

        var remainingDoseCount = Math.max(0, getStoredDoseCount(flaskStack) - consumedDoseCount);
        setStoredState(flaskStack, storedItem, remainingDoseCount);
    }

    private static int getRawStoredDoseCount(ItemStack stack) {
        var storageTag = getStorageTag(stack);
        if (storageTag == null) {
            return 0;
        }

        return Math.max(0, storageTag.getInt(STORED_DOSES_TAG));
    }

    private static int getMaxStoredDoseCount(ItemStack stack) {
        return BASE_MAX_STORED_DOSES + getLargeMugLevel(stack) * LARGE_MUG_BONUS_PER_LEVEL;
    }

    private static void setStoredState(ItemStack flaskStack, ItemStack storedItem, int storedDoseCount) {
        var normalizedItem = normalizeAcceptedItem(storedItem);
        if (normalizedItem.isEmpty()) {
            clearStoredState(flaskStack);
            return;
        }

        var clampedStoredDoseCount = Math.max(0, Math.min(getMaxStoredDoseCount(flaskStack), storedDoseCount));
        updateStorageTag(flaskStack, storageTag -> {
            storageTag.put(STORED_ITEM_TAG, normalizedItem.saveOptional(SERIALIZATION_LOOKUP));
            storageTag.putInt(STORED_DOSES_TAG, clampedStoredDoseCount);
        });
    }

    private static float getDrinkDurationMultiplier(ItemStack stack) {
        var guzzleReduction = Math.min(getGuzzleLevel(stack), GUZZLE_LEVEL_CAP) * GUZZLE_REDUCTION_PER_LEVEL;
        return Math.max(0.1F, 1.0F - guzzleReduction);
    }

    private static float getEffectDurationMultiplier(ItemStack stack) {
        return 1.0F + getRedEnergyLevel(stack) * RED_ENERGY_DURATION_BONUS_PER_LEVEL;
    }

    private static int getGuzzleLevel(ItemStack stack) {
        return Enchantments.getLevel(stack, Enchantments.GUZZLE);
    }

    private static int getLargeMugLevel(ItemStack stack) {
        return Enchantments.getLevel(stack, Enchantments.LARGE_MUG);
    }

    private static int getRedEnergyLevel(ItemStack stack) {
        return Enchantments.getLevel(stack, Enchantments.RED_ENERGY);
    }

    private static int getGlowEnergyLevel(ItemStack stack) {
        return Enchantments.getLevel(stack, Enchantments.GLOW_ENERGY);
    }

    private static void setEffectParticlesSuppressed(ItemStack flaskStack, boolean suppressed) {
        updateStorageTag(flaskStack, storageTag -> {
            if (suppressed) {
                storageTag.putBoolean(PARTICLES_SUPPRESSED_TAG, true);
            } else {
                storageTag.remove(PARTICLES_SUPPRESSED_TAG);
            }
        });
    }

    private static void clearStoredState(ItemStack flaskStack) {
        updateStorageTag(flaskStack, storageTag -> {
            storageTag.remove(STORED_ITEM_TAG);
            storageTag.remove(STORED_DOSES_TAG);
        });
    }

    private static void cleanupStorageTag(CompoundTag rootTag, CompoundTag storageTag) {
        if (storageTag.getAllKeys().isEmpty()) {
            rootTag.remove(STORAGE_TAG);
        } else {
            rootTag.put(STORAGE_TAG, storageTag);
        }
    }

    private static void updateStorageTag(ItemStack flaskStack, java.util.function.Consumer<CompoundTag> updater) {
        CustomData.update(DataComponents.CUSTOM_DATA, flaskStack, tag -> {
            var storageTag = tag.contains(STORAGE_TAG, Tag.TAG_COMPOUND)
                    ? tag.getCompound(STORAGE_TAG).copy()
                    : new CompoundTag();
            updater.accept(storageTag);
            cleanupStorageTag(tag, storageTag);
        });
    }

    private static void normalizeStoredDosesToCapacity(ItemStack stack) {
        var storageTag = getStorageTag(stack);
        if (storageTag == null) {
            return;
        }

        var storedItem = getStoredItem(stack);
        if (storedItem.isEmpty() || !isSupportedStoredItem(storedItem)) {
            clearStoredState(stack);
            return;
        }

        var normalizedDoseCount = getStoredDoseCount(stack);
        if (getRawStoredDoseCount(stack) != normalizedDoseCount) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                if (!tag.contains(STORAGE_TAG, Tag.TAG_COMPOUND)) {
                    return;
                }

                var updatedStorageTag = tag.getCompound(STORAGE_TAG).copy();
                updatedStorageTag.putInt(STORED_DOSES_TAG, normalizedDoseCount);
                tag.put(STORAGE_TAG, updatedStorageTag);
            });
        }
    }

    private static @Nullable CompoundTag getStorageTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        var tag = customData.copyTag();
        return tag.contains(STORAGE_TAG, Tag.TAG_COMPOUND) ? tag.getCompound(STORAGE_TAG) : null;
    }

    private static void removeStorageTag(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(STORAGE_TAG));
    }

    private record TransferPreview(ItemStack representativeItem, FluidStack drainFluid, int transferableDoseCount) {
    }

    private record ExportPreview(FluidStack fluidStack, int doseCount, SoundEvent fillSound) {
        private FluidStack singleDoseFluid() {
            var fluid = fluidStack.copy();
            fluid.setAmount(MILLIBUCKETS_PER_DOSE);
            return fluid;
        }
    }
}
