package jp.aquafactory.apprenticecodex.item.flask;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.item.consumables.FireAleItem;
import io.redspace.ironsspellbooks.item.consumables.NetherwardTinctureItem;
import io.redspace.ironsspellbooks.item.consumables.SimpleElixir;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.utility.AlchemistCauldronFluidTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

public abstract class AbstractPotionFlaskItem extends Item {
    private static final HolderLookup.Provider SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    protected static final int MILLIBUCKETS_PER_DOSE = 250;
    protected static final int ENCHANTMENT_VALUE = 10;
    protected static final float RED_ENERGY_DURATION_BONUS_PER_LEVEL = 0.25F;
    protected static final int BAR_COLOR = 0x4F88E8;
    protected static final int DEFAULT_TINT_COLOR = 0xFFFFFFFF;
    protected static final int FIRE_ALE_TINT_COLOR = 0xFF8F3028;
    protected static final String STORAGE_TAG = "SpellcastersFlask";
    protected static final String STORED_ITEM_TAG = "StoredItem";
    protected static final String STORED_DOSES_TAG = "StoredDoses";
    protected static final String PARTICLES_SUPPRESSED_TAG = "ParticlesSuppressed";

    private static final int BASE_MAX_STORED_DOSES = 8;
    private static final int LARGE_MUG_BONUS_PER_LEVEL = 2;
    private static final int MISMATCH_TRANSFER_CONFIRM_TICKS = 30;
    private static final int MISMATCH_POTION_CONSUMPTION = 2;
    private static final Map<UUID, PendingMismatchTransfer> PENDING_MISMATCH_TRANSFERS = new HashMap<>();

    protected AbstractPotionFlaskItem(Properties properties) {
        super(properties);
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

            var player = context.getPlayer();
            if (isVanillaPotionTypeMismatched(importPreview.representativeItem)
                    && player != null
                    && !consumePendingMismatchTransfer(player, context.getClickedPos(), context.getHand(), importPreview, level.getGameTime())) {
                rememberPendingMismatchTransfer(player, context.getClickedPos(), context.getHand(), importPreview, level.getGameTime());
                sendMismatchTransferWarning(player, stack, importPreview.representativeItem);
                return InteractionResult.CONSUME;
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

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        var storedItem = getStoredItem(stack);
        appendStoredEffectTooltips(lines, stack, storedItem);
        appendSuppressedParticlesTooltip(lines, stack);
        appendMismatchFlaskTypeTooltip(lines, stack);
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

        return isSupportedFlaskEnchantment(enchantment);
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

    protected int getBaseStoredDoseCapacity() {
        return BASE_MAX_STORED_DOSES;
    }

    protected int getLargeMugBonusPerLevel() {
        return LARGE_MUG_BONUS_PER_LEVEL;
    }

    protected boolean isSupportedStoredItem(ItemStack stack) {
        var item = stack.getItem();
        return isSupportedPotionItem(stack)
                || item instanceof SimpleElixir
                || item instanceof FireAleItem
                || item instanceof NetherwardTinctureItem;
    }

    protected boolean isSupportedPotionItem(ItemStack stack) {
        return isSupportedPotionItemByAnyFlask(stack);
    }

    protected boolean isPreferredPotionItem(ItemStack stack) {
        return stack.is(Items.POTION);
    }

    protected boolean isSupportedFlaskEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.LARGE_MUG)
                || enchantment.is(Enchantments.RED_ENERGY)
                || enchantment.is(Enchantments.GLOW_ENERGY);
    }

    protected int getLargeMugLevel(ItemStack stack) {
        return Enchantments.getLevel(stack, Enchantments.LARGE_MUG);
    }

    protected int getRedEnergyLevel(ItemStack stack) {
        return Enchantments.getLevel(stack, Enchantments.RED_ENERGY);
    }

    protected int getGlowEnergyLevel(ItemStack stack) {
        return Enchantments.getLevel(stack, Enchantments.GLOW_ENERGY);
    }

    protected int getMaxStoredDoseCount(ItemStack stack) {
        return getBaseStoredDoseCapacity() + getLargeMugLevel(stack) * getLargeMugBonusPerLevel();
    }

    protected float getEffectDurationMultiplier(ItemStack stack) {
        return (1.0F + getRedEnergyLevel(stack) * RED_ENERGY_DURATION_BONUS_PER_LEVEL)
                * getGlowEnergyDurationMultiplier(stack);
    }

    protected float getGlowEnergyDurationMultiplier(ItemStack stack) {
        var glowEnergyLevel = getGlowEnergyLevel(stack);
        if (glowEnergyLevel <= 0) {
            return 1.0F;
        }

        return 1.0F / (1.0F + glowEnergyLevel);
    }

    protected @NotNull ItemStack normalizeAcceptedItem(ItemStack representativeItem) {
        if (!isSupportedStoredItem(representativeItem)) {
            return ItemStack.EMPTY;
        }

        var normalizedItem = representativeItem.copy();
        normalizedItem.setCount(1);
        return normalizedItem;
    }

    public static boolean isFilled(ItemStack stack) {
        return getStoredDoseCount(stack) > 0;
    }

    public static boolean isEffectParticlesSuppressed(ItemStack stack) {
        var storageTag = getStorageTag(stack);
        return storageTag != null && storageTag.getBoolean(PARTICLES_SUPPRESSED_TAG);
    }

    public static @NotNull ItemStack copyWithToggledEffectParticles(@NotNull ItemStack flaskStack) {
        if (getFlaskItem(flaskStack) == null) {
            return ItemStack.EMPTY;
        }

        var result = flaskStack.copy();
        setEffectParticlesSuppressed(result, !isEffectParticlesSuppressed(result));
        return result;
    }

    public static @NotNull ItemStack copyFilterItem(@NotNull ItemStack stack) {
        if (stack.getItem() instanceof AbstractPotionFlaskItem) {
            return normalizeSharedAcceptedItem(getStoredItem(stack));
        }

        return normalizeSharedAcceptedItem(stack);
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
        var flaskItem = getFlaskItem(stack);
        if (flaskItem == null) {
            return 0;
        }

        return Math.min(getRawStoredDoseCount(stack), flaskItem.getMaxStoredDoseCount(stack));
    }

    public static int getMaxDoseCapacity(ItemStack stack) {
        var flaskItem = getFlaskItem(stack);
        return flaskItem == null ? 0 : flaskItem.getMaxStoredDoseCount(stack);
    }

    public static boolean isStoredVanillaPotionTypeMismatched(ItemStack flaskStack) {
        var flaskItem = getFlaskItem(flaskStack);
        return flaskItem != null && flaskItem.isVanillaPotionTypeMismatched(getStoredItem(flaskStack));
    }

    public static int getStoredDoseConsumptionCount(ItemStack flaskStack) {
        return isStoredVanillaPotionTypeMismatched(flaskStack) ? MISMATCH_POTION_CONSUMPTION : 1;
    }

    public static boolean canAcceptRepresentativeForAutomaticFill(ItemStack flaskStack, ItemStack representativeItem) {
        var flaskItem = getFlaskItem(flaskStack);
        return flaskItem != null
                && !flaskItem.normalizeAcceptedItem(representativeItem).isEmpty();
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
        var flaskItem = getFlaskItem(flaskStack);
        return flaskItem != null && flaskItem.canAddDoseFromItemInternal(flaskStack, candidateStack);
    }

    public static boolean matchesStoredItem(ItemStack flaskStack, ItemStack candidateStack) {
        var flaskItem = getFlaskItem(flaskStack);
        return flaskItem != null && flaskItem.matchesStoredItemInternal(flaskStack, candidateStack);
    }

    public static ItemStack copyWithAddedDose(ItemStack flaskStack, ItemStack candidateStack) {
        return copyWithAddedDoses(flaskStack, candidateStack, 1);
    }

    public static ItemStack copyWithAddedDoses(ItemStack flaskStack, ItemStack candidateStack, int addedDoseCount) {
        var flaskItem = getFlaskItem(flaskStack);
        return flaskItem == null ? ItemStack.EMPTY : flaskItem.copyWithAddedDosesInternal(flaskStack, candidateStack, addedDoseCount);
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
        var flaskItem = getFlaskItem(flaskStack);
        return flaskItem != null && flaskItem.canExtractOneDoseInternal(flaskStack);
    }

    public static ItemStack copyStoredItemForCrafting(ItemStack flaskStack) {
        var flaskItem = getFlaskItem(flaskStack);
        return flaskItem == null ? ItemStack.EMPTY : flaskItem.copyStoredItemForCraftingInternal(flaskStack);
    }

    public static ItemStack copyAfterExtractingOneDose(ItemStack flaskStack) {
        return copyAfterExtractingDoses(flaskStack, 1);
    }

    public static ItemStack copyAfterExtractingDoses(ItemStack flaskStack, int extractedDoseCount) {
        var flaskItem = getFlaskItem(flaskStack);
        return flaskItem == null ? ItemStack.EMPTY : flaskItem.copyAfterExtractingDosesInternal(flaskStack, extractedDoseCount);
    }

    public static @NotNull ItemStack createExtractedPotionForThrow(@NotNull ItemStack flaskStack, int additionalAmplifier) {
        return createExtractedPotionForThrow(flaskStack, getStoredItem(flaskStack), additionalAmplifier);
    }

    public static @NotNull ItemStack createExtractedPotionForThrow(@NotNull ItemStack flaskStack, @NotNull ItemStack storedItem,
                                                                   int additionalAmplifier) {
        var flaskItem = getFlaskItem(flaskStack);
        if (flaskItem == null) {
            return ItemStack.EMPTY;
        }

        var normalizedStoredItem = flaskItem.normalizeAcceptedItem(storedItem);
        if (normalizedStoredItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var scaledEffects = flaskItem.extractEffectsFromItem(normalizedStoredItem).stream()
                .map(effect -> flaskItem.scaleEffect(flaskStack, effect, additionalAmplifier))
                .toList();
        if (scaledEffects.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var thrownPotion = new ItemStack(normalizedStoredItem.is(Items.LINGERING_POTION)
                ? Items.LINGERING_POTION
                : Items.SPLASH_POTION);
        thrownPotion.set(
                DataComponents.POTION_CONTENTS,
                new PotionContents(
                        java.util.Optional.of(Potions.WATER),
                        java.util.Optional.of(getStoredItemTintColor(normalizedStoredItem) & 0x00FFFFFF),
                        scaledEffects
                )
        );
        return thrownPotion;
    }

    public static @NotNull ItemStack resolveRepresentativeItem(@NotNull Level level, @NotNull FluidStack fluidStack) {
        return createRepresentativeItemForAnyFlask(level, fluidStack);
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

        var effects = extractEffectsFromAnySupportedItem(storedItem);
        if (effects.isEmpty()) {
            return DEFAULT_TINT_COLOR;
        }

        // PotionUtils の混色ロジックは使わず、仕様として抽出順の先頭効果色だけを表示に使う。
        return withFullAlpha(effects.get(0).getEffect().value().getColor());
    }

    private static int withFullAlpha(int rgbColor) {
        return 0xFF000000 | rgbColor;
    }

    protected List<MobEffectInstance> extractEffectsFromItem(ItemStack storedItem) {
        return extractEffectsFromAnySupportedItem(storedItem);
    }

    private static List<MobEffectInstance> extractEffectsFromAnySupportedItem(ItemStack storedItem) {
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

    protected final MobEffectInstance scaleEffect(ItemStack flaskStack, MobEffectInstance originalEffect) {
        return scaleEffect(flaskStack, originalEffect, 0);
    }

    protected final MobEffectInstance scaleEffect(ItemStack flaskStack, MobEffectInstance originalEffect, int additionalAmplifier) {
        var scaledDuration = originalEffect.getEffect().value().isInstantenous()
                ? originalEffect.getDuration()
                // Glow Energy の増幅は維持しつつ、フラスコ全般では duration を逆補正してバランスを取る。
                : Math.max(1, Math.round(originalEffect.getDuration() * getEffectDurationMultiplier(flaskStack)));
        var scaledAmplifier = Math.max(0, originalEffect.getAmplifier() + getGlowEnergyLevel(flaskStack) + additionalAmplifier);
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

    private void appendStoredEffectTooltips(List<Component> lines, ItemStack flaskStack, ItemStack storedItem) {
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

    private static void appendMismatchFlaskTypeTooltip(List<Component> lines, ItemStack flaskStack) {
        if (!isStoredVanillaPotionTypeMismatched(flaskStack)) {
            return;
        }

        lines.add(Component.translatable("item.apprenticecodex.flask_system.mismatch_flask_type")
                .withStyle(ChatFormatting.YELLOW));
    }

    private List<Component> createStoredEffectTooltipLines(ItemStack flaskStack, ItemStack storedItem) {
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
                .append(Component.literal(Integer.toString(getMaxDoseCapacity(stack)))
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
    private TransferPreview previewTransfer(Level level, ItemStack flaskStack, AlchemistCauldronTile cauldronTile) {
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
        return new TransferPreview(representativeItem, drainFluid);
    }

    @Nullable
    private ExportPreview previewExport(Level level, ItemStack flaskStack, AlchemistCauldronTile cauldronTile) {
        if (cauldronTile.getFluidAmount() > 0 || getStoredDoseCount(flaskStack) <= 0) {
            return null;
        }

        var storedItem = getStoredItem(flaskStack);
        if (storedItem.isEmpty()) {
            return null;
        }

        var preview = createExportPreviewForStoredItem(level, storedItem);
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
        if (appliedDoseCount == 0) {
            return null;
        }

        exportFluid.setAmount(appliedDoseCount * MILLIBUCKETS_PER_DOSE);
        return new ExportPreview(exportFluid, appliedDoseCount, preview.fillSound);
    }

    protected @NotNull ItemStack createRepresentativeItem(Level level, FluidStack fluidStack) {
        var representativeItem = createRepresentativeItemFromRecipe(level, fluidStack, this::normalizeAcceptedItem);
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
    protected ExportPreview createExportPreviewForStoredItem(Level level, ItemStack storedItem) {
        var preview = createExportPreview(level, storedItem);
        if (preview == null) {
            return null;
        }

        return normalizeAcceptedItem(storedItem).isEmpty() ? null : preview;
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

    private static @NotNull ItemStack createRepresentativeItemForAnyFlask(Level level, FluidStack fluidStack) {
        var representativeItem = createRepresentativeItemFromRecipe(level, fluidStack, AbstractPotionFlaskItem::normalizeSharedAcceptedItem);
        if (!representativeItem.isEmpty()) {
            return representativeItem;
        }

        var sampleFluid = fluidStack.copy();
        sampleFluid.setAmount(MILLIBUCKETS_PER_DOSE);
        representativeItem = PotionFluid.from(sampleFluid);
        if (representativeItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return normalizeSharedAcceptedItem(representativeItem);
    }

    private static ItemStack createRepresentativeItemFromRecipe(Level level, FluidStack fluidStack,
                                                                UnaryOperator<ItemStack> normalizer) {
        var sampleFluid = fluidStack.copy();
        sampleFluid.setAmount(MILLIBUCKETS_PER_DOSE);

        for (var recipe : level.getRecipeManager().getAllRecipesFor(RecipeRegistry.ALCHEMIST_CAULDRON_FILL_TYPE.get())) {
            if (!FluidStack.matches(recipe.value().result(), sampleFluid)) {
                continue;
            }

            var representativeItem = findRepresentativeItem(recipe.value().input(), normalizer);
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

    private static ItemStack findRepresentativeItem(Ingredient ingredient, UnaryOperator<ItemStack> normalizer) {
        for (var candidate : ingredient.getItems()) {
            var normalizedItem = normalizer.apply(candidate);
            if (!normalizedItem.isEmpty()) {
                return normalizedItem;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack normalizeSharedAcceptedItem(ItemStack representativeItem) {
        if (!isSupportedStoredItemByAnyFlask(representativeItem)) {
            return ItemStack.EMPTY;
        }

        var normalizedItem = representativeItem.copy();
        normalizedItem.setCount(1);
        return normalizedItem;
    }

    private static boolean isSupportedStoredItemByAnyFlask(ItemStack stack) {
        var item = stack.getItem();
        return isSupportedPotionItemByAnyFlask(stack)
                || item instanceof SimpleElixir
                || item instanceof FireAleItem
                || item instanceof NetherwardTinctureItem;
    }

    private static boolean isSupportedPotionItemByAnyFlask(ItemStack stack) {
        return stack.getItem() instanceof PotionItem
                && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION));
    }

    private boolean isVanillaPotionTypeMismatched(ItemStack stack) {
        return isSupportedPotionItemByAnyFlask(stack) && !isPreferredPotionItem(stack);
    }

    private static boolean consumePendingMismatchTransfer(Player player, BlockPos cauldronPos, InteractionHand hand,
                                                          TransferPreview preview, long gameTime) {
        var pending = PENDING_MISMATCH_TRANSFERS.remove(player.getUUID());
        return pending != null
                && pending.expiresAtGameTime >= gameTime
                && pending.hand == hand
                && pending.cauldronPos.equals(cauldronPos)
                && ItemStack.isSameItemSameComponents(pending.representativeItem, preview.representativeItem);
    }

    private static void rememberPendingMismatchTransfer(Player player, BlockPos cauldronPos, InteractionHand hand,
                                                        TransferPreview preview, long gameTime) {
        PENDING_MISMATCH_TRANSFERS.put(player.getUUID(), new PendingMismatchTransfer(
                cauldronPos.immutable(),
                hand,
                preview.representativeItem.copy(),
                gameTime + MISMATCH_TRANSFER_CONFIRM_TICKS
        ));
    }

    private static void sendMismatchTransferWarning(Player player, ItemStack flaskStack, ItemStack representativeItem) {
        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.flask_system.mismatch_flask_type.warning",
                representativeItem.getHoverName(),
                flaskStack.getHoverName()
        ).withStyle(ChatFormatting.YELLOW), true);
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

    protected static void decrementStoredDoseCount(ItemStack flaskStack) {
        decrementStoredDoseCount(flaskStack, 1);
    }

    protected static void decrementStoredDoseCount(ItemStack flaskStack, int consumedDoseCount) {
        var storedItem = getStoredItem(flaskStack);
        if (storedItem.isEmpty()) {
            clearStoredState(flaskStack);
            return;
        }

        var remainingDoseCount = Math.max(0, getStoredDoseCount(flaskStack) - consumedDoseCount);
        if (remainingDoseCount == 0) {
            clearStoredState(flaskStack);
            return;
        }

        setStoredState(flaskStack, storedItem, remainingDoseCount);
    }

    private static int getRawStoredDoseCount(ItemStack stack) {
        var storageTag = getStorageTag(stack);
        if (storageTag == null) {
            return 0;
        }

        return Math.max(0, storageTag.getInt(STORED_DOSES_TAG));
    }

    private static void setStoredState(ItemStack flaskStack, ItemStack storedItem, int storedDoseCount) {
        var flaskItem = getFlaskItem(flaskStack);
        if (flaskItem == null) {
            return;
        }

        var normalizedItem = flaskItem.normalizeAcceptedItem(storedItem);
        if (normalizedItem.isEmpty() || storedDoseCount <= 0) {
            clearStoredState(flaskStack);
            return;
        }

        updateStorageTag(flaskStack, storageTag -> {
            storageTag.put(STORED_ITEM_TAG, normalizedItem.saveOptional(SERIALIZATION_LOOKUP));
            storageTag.putInt(
                    STORED_DOSES_TAG,
                    Math.max(0, Math.min(flaskItem.getMaxStoredDoseCount(flaskStack), storedDoseCount))
            );
        });
    }

    private boolean canAddDoseFromItemInternal(ItemStack flaskStack, ItemStack candidateStack) {
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

    private boolean matchesStoredItemInternal(ItemStack flaskStack, ItemStack candidateStack) {
        var candidateItem = normalizeAcceptedItem(candidateStack);
        if (candidateItem.isEmpty()) {
            return false;
        }

        var storedItem = normalizeAcceptedItem(getStoredItem(flaskStack));
        return !storedItem.isEmpty() && ItemStack.isSameItemSameComponents(storedItem, candidateItem);
    }

    private ItemStack copyWithAddedDosesInternal(ItemStack flaskStack, ItemStack candidateStack, int addedDoseCount) {
        var candidateItem = normalizeAcceptedItem(candidateStack);
        if (candidateItem.isEmpty() || addedDoseCount <= 0) {
            return ItemStack.EMPTY;
        }

        var storedDoseCount = getStoredDoseCount(flaskStack);
        var targetDoseCount = Math.min(getMaxStoredDoseCount(flaskStack), storedDoseCount + addedDoseCount);
        if (targetDoseCount <= storedDoseCount) {
            return ItemStack.EMPTY;
        }

        if (storedDoseCount > 0 && !matchesStoredItemInternal(flaskStack, candidateItem)) {
            return ItemStack.EMPTY;
        }

        var result = flaskStack.copy();
        setStoredState(result, candidateItem, targetDoseCount);
        return result;
    }

    private boolean canExtractOneDoseInternal(ItemStack flaskStack) {
        return getStoredDoseCount(flaskStack) > 0 && !normalizeAcceptedItem(getStoredItem(flaskStack)).isEmpty();
    }

    private ItemStack copyStoredItemForCraftingInternal(ItemStack flaskStack) {
        if (!canExtractOneDoseInternal(flaskStack)) {
            return ItemStack.EMPTY;
        }

        var storedItem = getStoredItem(flaskStack).copy();
        storedItem.setCount(1);
        return storedItem;
    }

    private ItemStack copyAfterExtractingDosesInternal(ItemStack flaskStack, int extractedDoseCount) {
        if (!canExtractOneDoseInternal(flaskStack) || extractedDoseCount <= 0) {
            return ItemStack.EMPTY;
        }

        var result = flaskStack.copy();
        decrementStoredDoseCount(result, extractedDoseCount);
        return result;
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

    protected final void normalizeStoredDosesToCapacity(ItemStack stack) {
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

    @Nullable
    private static CompoundTag getStorageTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        var tag = customData.copyTag();
        return tag.contains(STORAGE_TAG, Tag.TAG_COMPOUND) ? tag.getCompound(STORAGE_TAG) : null;
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

    @Nullable
    private static AbstractPotionFlaskItem getFlaskItem(ItemStack stack) {
        return stack.getItem() instanceof AbstractPotionFlaskItem flask ? flask : null;
    }

    private record TransferPreview(ItemStack representativeItem, FluidStack drainFluid) {
    }

    private record PendingMismatchTransfer(BlockPos cauldronPos, InteractionHand hand, ItemStack representativeItem,
                                           long expiresAtGameTime) {
    }

    protected record ExportPreview(FluidStack fluidStack, int doseCount, SoundEvent fillSound) {
        private FluidStack singleDoseFluid() {
            var fluid = fluidStack.copy();
            fluid.setAmount(MILLIBUCKETS_PER_DOSE);
            return fluid;
        }
    }
}
