package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ChargecastCatalystbookServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.*;
import jp.aquafactory.apprenticecodex.item.*;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.spell.IChargecastStaffbowIncompatibleSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.HandStackResolver;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class ChargecastCatalystbook extends Item implements GeoItem, IPresetSpellContainer, UniqueItem,
        RestrictedSpellImbuableItem, StoredSpellCalibrationImbueTarget, SpellCalibrationAdjustmentTarget,
        ArcaneAnvilScrollImbueBlockItem, CastAnimationOverrideItem, ImmediateSneakSelectionUiItem,
        OffhandAttributeRelocatingItem, NonDamageableAnvilMergeItem,
        TranscendencePolicy, AttributeEnchantmentPolicy, WisdomPolicy, PlunderTarget, IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.chargecast_catalystbook.desc_";
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    public static final int CALIBRATION_SCROLL_SLOT_COUNT = 4;
    public static final int BASE_CALIBRATION_SCROLL_SLOT_COUNT = 1;
    private static final String CALIBRATION_TAG = "ChargecastCalibration";
    private static final String SCROLLS_TAG = "Scrolls";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String SELECTED_SCROLL_INDEX_TAG = "SelectedScrollIndex";
    private static final String SCHOOL_POWER_SCHOOL_TAG = "SchoolPowerSchool";
    private static final double SPELL_POWER_BONUS = 0.10D;
    private static final double SCHOOL_SPELL_POWER_BONUS = 0.15D;
    private static final ResourceLocation MAINHAND_SPELL_POWER_ID = ResourceLocation.fromNamespaceAndPath(
            "apprenticecodex", "chargecast_catalystbook_mainhand_spell_power"
    );
    private static final ResourceLocation OFFHAND_SPELL_POWER_ID = ResourceLocation.fromNamespaceAndPath(
            "apprenticecodex", "chargecast_catalystbook_offhand_spell_power"
    );
    private static final Set<AttributeEnchantmentType> SUPPORTED_ATTRIBUTE_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.ALACRITY,
            AttributeEnchantmentType.REFLUX,
            AttributeEnchantmentType.RESERVOIR,
            AttributeEnchantmentType.TENSE
    );
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            CalibrationAdjustmentProfile.of(
                    CalibrationAdjustmentRule.repeatable(
                            ChargecastCatalystbook::isSpellSlotUpgrade,
                            CalibrationAdjustmentHints.slotUpgrades()
                    ),
                    CalibrationAdjustmentRule.unique(
                            ScrollcasterSchoolRuneResolver::isSchoolRune,
                            CalibrationAdjustmentHints.schoolRunes()
                    ),
                    CalibrationAdjustmentRule.unique(
                            stack -> stack.is(ItemRegistry.WISDOM_SHARD.get()),
                            CalibrationAdjustmentHints.wisdomShard()
                    ),
                    CalibrationAdjustmentRule.unique(
                            ChargecastCatalystbook::isSilverRing,
                            CalibrationAdjustmentHints.silverRing()
                    ),
                    CalibrationAdjustmentRule.unique(
                            stack -> stack.is(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                            CalibrationAdjustmentHint.specificItem(ItemRegistry.SILVER_SPELL_AMPLIFIER)
                    )
            );
    private static final HolderLookup.Provider FALLBACK_SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ChargecastCatalystbook() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        refreshSelectedSpellContainer(stack);
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 22;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return SUPPORTED_ATTRIBUTE_ENCHANTMENTS;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment)
                || AttributeEnchantmentType.from(enchantment)
                .map(SUPPORTED_ATTRIBUTE_ENCHANTMENTS::contains)
                .orElse(false)
                || enchantment.is(Enchantments.TRANSCENDENCE)
                || enchantment.is(Enchantments.WISDOM)
                || enchantment.is(Enchantments.PLUNDER);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return !enchantments.isEmpty()
                && enchantments.keySet().stream().allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        var offhand = usesOffhandAttributeModifiers(stack);
        var modifierId = offhand ? OFFHAND_SPELL_POWER_ID : MAINHAND_SPELL_POWER_ID;
        var slotGroup = offhand ? EquipmentSlotGroup.OFFHAND : EquipmentSlotGroup.MAINHAND;
        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        var schoolAttribute = MagicTools.resolveSchoolPowerAttribute(getResolvedCalibrationSchool(stack));
        if (schoolAttribute != null) {
            builder.put(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(schoolAttribute), new AttributeModifier(
                    modifierId,
                    SCHOOL_SPELL_POWER_BONUS,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        } else {
            builder.put(AttributeRegistry.SPELL_POWER, new AttributeModifier(
                    modifierId,
                    SPELL_POWER_BONUS,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }
        var merged = AttributeEnchantmentResolver.resolveMergedModifiers(
                builder.build(), stack, "apprenticecodex.chargecast_catalystbook"
        );
        var result = ItemAttributeModifiers.builder();
        for (var entry : merged.entries()) {
            result.add(entry.getKey(), entry.getValue(), slotGroup);
        }
        return result.build();
    }

    @Override
    public boolean usesOffhandAttributeModifiers(@NotNull ItemStack stack) {
        return hasAdjustment(stack, candidate -> candidate.is(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()));
    }

    @Override
    public boolean isSneakSelectionUiEnabled(ItemStack stack) {
        return getEnabledCalibrationScrollSlotCount(stack) > BASE_CALIBRATION_SCROLL_SLOT_COUNT;
    }

    @Override
    public List<SneakSelectionView> getSneakSelectionViews(ItemStack stack) {
        return getSelectionViews(stack);
    }

    @Override
    public int getSneakSelectionIndex(ItemStack stack) {
        return getSelectedScrollIndex(stack);
    }

    @Override
    public boolean isSneakSelectionIndexSelectable(ItemStack stack, int selectionIndex) {
        return isSelectableScrollIndex(stack, selectionIndex);
    }

    @Override
    public void setSneakSelectionIndex(ItemStack stack, int selectionIndex) {
        setSelectedScrollIndex(stack, selectionIndex);
    }

    @Override
    public HandStackResolver.OffhandResolution getSneakSelectionOffhandResolution() {
        return HandStackResolver.OffhandResolution.LOGICAL;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            Player player,
            @NotNull InteractionHand usedHand
    ) {
        // CastingItemのように振る舞うため、このアイテム自体はメインハンドオフハンドのケアは行わない.
        var stack = player.getItemInHand(usedHand);
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.isCasting()) {
            if (player instanceof ServerPlayer serverPlayer) {
                Utils.serverSideCancelCast(serverPlayer);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        var spellData = hasWisdomShard(stack)
                ? new SpellSelectionManager(player).getSelectedSpellData()
                : getSelectedSpellData(stack);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null || spellData.getSpell() == SpellRegistry.none()) {
            return InteractionResultHolder.pass(stack);
        }
        if (spellData.getSpell().getCastType() != CastType.INSTANT) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "ui.apprenticecodex.chargecast.cannot_cast", spellData.getSpell().getDisplayName(player)
                ), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        var spell = spellData.getSpell();
        if (spell instanceof IChargecastStaffbowIncompatibleSpell) {
            if (!level.isClientSide) {
                player.displayClientMessage(createRejectedSpellMessage(spell.getDisplayName(player)), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        var config = level.isClientSide
                ? ChargecastCatalystbookClientConfigState.values()
                : ApprenticeCodexServerConfig.chargecastCatalystbookConfig();
        if (config.isSpellDenied(spell.getSpellResource())) {
            if (!level.isClientSide) {
                player.displayClientMessage(createSpellDenylistedMessage(spell.getDisplayName(player)), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            ChargecastCatalystbookClientCastIntent.mark(player.getUUID(), stack, spell);
            // attemptInitiateCast はクライアントでは必ず false を返すため、ここで入力を消費してオフハンド使用へ流さない。
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var slotId = usedHand == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND
                : SpellSelectionManager.MAINHAND;
        var started = ChargecastCatalystbookStartSoundContext.callSuppressed(
                player.getUUID(),
                () -> spell.attemptInitiateCast(stack, spellLevel, level, player, CastSource.SWORD, true, slotId)
        );
        if (started && player instanceof ServerPlayer serverPlayer) {
            var duration = resolveCastDurationTicks(serverPlayer, stack);
            magicData = MagicData.getPlayerMagicData(serverPlayer);
            magicData.initiateCast(spell, spellLevel, duration, CastSource.SWORD, slotId);
            magicData.setPlayerCastingItem(stack);
            AudioTools.playSoundFromEntity(level, serverPlayer, SoundRegistry.VANILLA_CAST_BOOK.get(), SoundSource.PLAYERS);
            PacketDistributor.sendToPlayer(serverPlayer, new UpdateCastingStatePacket(
                    spell.getSpellId(), spellLevel, duration, CastSource.SWORD, slotId
            ));
        }
        return started
                ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : InteractionResultHolder.fail(stack);
    }

    public static Component createRejectedSpellMessage(Component spellName) {
        return Component.translatable("ui.apprenticecodex.chargecast.reject_spell", spellName)
                .withStyle(ChatFormatting.RED);
    }

    public static Component createSpellDenylistedMessage(Component spellName) {
        return Component.translatable("ui.apprenticecodex.chargecast.spell_denylisted", spellName)
                .withStyle(ChatFormatting.RED);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity,
                              int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (hasAnyCalibrationScroll(stack) && !isCurrentSelectedSpellContainer(stack)) {
            refreshSelectedSpellContainer(stack);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        var tooltipValues = resolveTooltipValues(stack);
        lines.add(Component.translatable(
                "item.apprenticecodex.chargecast_catalystbook.desc",
                ImbueTooltipHelper.getUseKeyName(),
                Utils.timeFromTicks(tooltipValues.castTimeTicks(), 1),
                Component.translatable(
                        "item.apprenticecodex.chargecast_catalystbook.spell_power",
                        Math.round((tooltipValues.spellPowerMultiplier() - 1.0D) * 100.0D)
                )
        ).withStyle(ChatFormatting.GRAY));
        var resolvedSchool = getResolvedCalibrationSchool(stack);
        if (resolvedSchool != null) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.chargecast_catalystbook.school_rune",
                    resolvedSchool.getDisplayName()
            ).withStyle(ChatFormatting.GRAY));
        }

        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                List.of(),
                "item.apprenticecodex.spellgun.tooltip.ability_title",
                "item.apprenticecodex.spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                List.of(ImbueTooltipHelper.translatableGray(
                        "item.apprenticecodex.spellgun.tooltip.restrict_restrict_instant_only"
                )),
                "item.apprenticecodex.spellgun.tooltip.restrict_title",
                null
        );
    }

    @Override
    public boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY
                && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null && spell != SpellRegistry.none() && spell.getCastType() == CastType.INSTANT;
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        refreshSelectedSpellContainer(stack);
    }

    @Override
    public List<Component> getImbueRestrictionTooltipLines() {
        return ImbueTooltipHelper.collectCastTypeRestrictionLines(EnumSet.of(SpellGunCastType.INSTANT));
    }

    @Override
    public @NotNull SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull SpellData spellData
    ) {
        if (slot < 0 || slot >= getEnabledCalibrationScrollSlotCount(targetStack)
                || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return SpellCalibrationImbueState.REJECTED;
        }
        return spellData.getSpell().getCastType() == CastType.INSTANT
                ? SpellCalibrationImbueState.ACCEPTED_USABLE
                : SpellCalibrationImbueState.REJECTED;
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    @Override
    public void onCalibrationAdjustmentsChanged(
            @NotNull ItemStack targetStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        refreshResolvedCalibrationSchool(targetStack);
        refreshSelectedSpellContainer(targetStack);
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_PROFILE;
    }

    @Override
    public boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        // caster を受け取れない Item API ではローカル/リモートを安全に識別できないため、Mixin 側で判定する。
        return false;
    }

    @Override
    public AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public boolean shouldOverrideCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        // 完了アニメーションの取得は表示だけを担当し、詠唱状態の終了は cast-finished packet に限定する。
        return false;
    }

    @Override
    public AnimationHolder getCastFinishAnimation(ItemStack stack, AbstractSpell spell, boolean cancelled) {
        // INSTANT の主モーションが Start / Finish のどちらに置かれていても、公開情報から完了動作を解決する。
        // cancelled=true は Iron's 側が戻り値に関係なくモーションを停止する。
        return ChargecastCatalystbookPresentationResolver.resolveCompletionAnimation(spell);
    }

    @Override
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static int resolveCastDurationTicks(Player player, ItemStack stack) {
        return resolveCastDurationTicks(
                player,
                stack,
                ApprenticeCodexServerConfig.chargecastCatalystbookConfig()
        );
    }

    public static int resolveCastDurationTicks(
            Player player,
            ItemStack stack,
            ChargecastCatalystbookServerConfig.Values config
    ) {
        var baseTicks = config.castTimeTicks();
        if (hasSilverRing(stack)) {
            return baseTicks;
        }
        var castTimeReduction = player.getAttributeValue(AttributeRegistry.CAST_TIME_REDUCTION);
        return Math.max(1, Math.round((float) (baseTicks * (2.0D - Utils.softCapFormula(castTimeReduction)))));
    }

    public static double resolveFinalSpellPowerMultiplier(Player player, ItemStack stack) {
        return resolveFinalSpellPowerMultiplier(
                player,
                stack,
                ApprenticeCodexServerConfig.chargecastCatalystbookConfig()
        );
    }

    public static double resolveFinalSpellPowerMultiplier(
            Player player,
            ItemStack stack,
            ChargecastCatalystbookServerConfig.Values config
    ) {
        var multiplier = config.spellPowerMultiplier();
        if (!hasSilverRing(stack)) {
            return multiplier;
        }
        var castTimeReduction = Math.max(1.0D,
                Utils.softCapFormula(player.getAttributeValue(AttributeRegistry.CAST_TIME_REDUCTION)));
        return multiplier * (1.0D + (castTimeReduction - 1.0D) * config.silverRingCastTimeBonusFactor());
    }

    private static TooltipValues resolveTooltipValues(ItemStack stack) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ChargecastCatalystbookClientTooltip.resolve(stack);
        }
        var config = ChargecastCatalystbookClientConfigState.values();
        return new TooltipValues(config.castTimeTicks(), config.spellPowerMultiplier());
    }

    public static boolean isManagedCast(Player player, @Nullable AbstractSpell spell) {
        var magicData = MagicData.getPlayerMagicData(player);
        return magicData != null && magicData.isCasting()
                && magicData.getPlayerCastingItem().getItem() instanceof ChargecastCatalystbook
                && (spell == null || spell.getSpellId().equals(magicData.getCastingSpellId()));
    }

    public static @NotNull ItemStack getCastingStack(Player player) {
        var magicData = MagicData.getPlayerMagicData(player);
        return magicData == null ? ItemStack.EMPTY : magicData.getPlayerCastingItem();
    }

    public static @NotNull ItemStack getCalibrationScroll(@NotNull ItemStack stack, int slot) {
        return getCalibrationItem(stack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT, serializationLookup());
    }

    public static void setCalibrationScroll(@NotNull ItemStack stack, int slot, @NotNull ItemStack scroll) {
        setCalibrationItem(stack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT, scroll, serializationLookup());
        refreshSelectedSpellContainer(stack);
    }

    public static int getEnabledCalibrationScrollSlotCount(@NotNull ItemStack stack) {
        if (!isValidCalibrationAccess(stack, 0, 1)) {
            return 0;
        }
        var upgrades = 0;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isSpellSlotUpgrade(CalibrationAdjustmentStorage.get(
                    stack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, serializationLookup()
            ))) {
                ++upgrades;
            }
        }
        return Math.min(CALIBRATION_SCROLL_SLOT_COUNT, BASE_CALIBRATION_SCROLL_SLOT_COUNT + upgrades);
    }

    public static boolean hasAnyCalibrationScroll(@NotNull ItemStack stack) {
        return findFirstValidScrollIndex(stack) >= 0;
    }

    public static int getSelectedScrollIndex(@NotNull ItemStack stack) {
        var calibration = getCalibrationTag(stack);
        if (calibration == null || !calibration.contains(SELECTED_SCROLL_INDEX_TAG, Tag.TAG_INT)) {
            return -1;
        }
        var selected = calibration.getInt(SELECTED_SCROLL_INDEX_TAG);
        return selected >= 0 && selected < CALIBRATION_SCROLL_SLOT_COUNT ? selected : -1;
    }

    public static void setSelectedScrollIndex(@NotNull ItemStack stack, int selected) {
        if (!isSelectableScrollIndex(stack, selected)) {
            refreshSelectedSpellContainer(stack);
            return;
        }
        updateCalibrationTag(stack, calibration -> calibration.putInt(SELECTED_SCROLL_INDEX_TAG, selected));
        refreshSelectedSpellContainer(stack);
    }

    public static boolean isSelectableScrollIndex(@NotNull ItemStack stack, int selected) {
        return selected >= 0 && selected < getEnabledCalibrationScrollSlotCount(stack)
                && getScrollSpellData(getCalibrationScroll(stack, selected)) != SpellData.EMPTY;
    }

    public static @NotNull SpellData getSelectedSpellData(@NotNull ItemStack stack) {
        var selected = normalizeSelectedScrollIndex(stack);
        return selected < 0 ? SpellData.EMPTY : getScrollSpellData(getCalibrationScroll(stack, selected));
    }

    public static @NotNull List<SneakSelectionView> getSelectionViews(@NotNull ItemStack stack) {
        normalizeSelectedScrollIndex(stack);
        var views = new ArrayList<SneakSelectionView>();
        for (var slot = 0; slot < getEnabledCalibrationScrollSlotCount(stack); ++slot) {
            var spellData = getScrollSpellData(getCalibrationScroll(stack, slot));
            views.add(SneakSelectionView.forSpell(
                    slot,
                    spellData,
                    isSelectableScrollIndex(stack, slot)
            ));
        }
        return List.copyOf(views);
    }

    public static void refreshSelectedSpellContainer(@NotNull ItemStack stack) {
        var spellData = getSelectedSpellData(stack);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            ISpellContainer.remove(stack);
            return;
        }
        if (isCurrentSelectedSpellContainer(stack, spellData)) {
            return;
        }
        // Iron's のスペルホイールには、自前領域で選択中の1魔法だけを投影する。
        var container = ISpellContainer.create(1, true, false).mutableCopy();
        container.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
        ISpellContainer.set(stack, container.toImmutable());
    }

    public static @Nullable SchoolType getResolvedCalibrationSchool(ItemStack stack) {
        var calibration = getCalibrationTag(stack);
        if (calibration == null || !calibration.contains(SCHOOL_POWER_SCHOOL_TAG, Tag.TAG_STRING)) {
            return null;
        }
        var id = net.minecraft.resources.ResourceLocation.tryParse(calibration.getString(SCHOOL_POWER_SCHOOL_TAG));
        return id == null ? null : SchoolRegistry.getSchool(id);
    }

    public static boolean hasWisdomShard(ItemStack stack) {
        return hasAdjustment(stack, candidate -> candidate.is(ItemRegistry.WISDOM_SHARD.get()));
    }

    public static boolean hasSilverRing(ItemStack stack) {
        return hasAdjustment(stack, ChargecastCatalystbook::isSilverRing);
    }

    private static void refreshResolvedCalibrationSchool(ItemStack stack) {
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(
                    CalibrationAdjustmentStorage.get(
                            stack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, serializationLookup())
            );
            if (school.isPresent()) {
                updateCalibrationTag(stack, calibration ->
                        calibration.putString(SCHOOL_POWER_SCHOOL_TAG, school.get().getId().toString()));
                return;
            }
        }
        updateCalibrationTag(stack, calibration -> calibration.remove(SCHOOL_POWER_SCHOOL_TAG));
    }

    private static int normalizeSelectedScrollIndex(ItemStack stack) {
        var selected = getSelectedScrollIndex(stack);
        if (isSelectableScrollIndex(stack, selected)) {
            return selected;
        }
        selected = findFirstValidScrollIndex(stack);
        if (selected < 0) {
            updateCalibrationTag(stack, calibration -> calibration.remove(SELECTED_SCROLL_INDEX_TAG));
            ISpellContainer.remove(stack);
            return -1;
        }
        var normalized = selected;
        updateCalibrationTag(stack, calibration -> calibration.putInt(SELECTED_SCROLL_INDEX_TAG, normalized));
        return selected;
    }

    private static int findFirstValidScrollIndex(ItemStack stack) {
        for (var slot = 0; slot < getEnabledCalibrationScrollSlotCount(stack); ++slot) {
            if (getScrollSpellData(getCalibrationScroll(stack, slot)) != SpellData.EMPTY) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isCurrentSelectedSpellContainer(ItemStack stack) {
        var spellData = getSelectedSpellData(stack);
        return spellData != SpellData.EMPTY && isCurrentSelectedSpellContainer(stack, spellData);
    }

    private static boolean isCurrentSelectedSpellContainer(ItemStack stack, SpellData spellData) {
        var container = ISpellContainer.get(stack);
        if (container == null) {
            return false;
        }
        var current = container.getSpellAtIndex(0);
        return container.getMaxSpellCount() == 1 && container.isSpellWheel() && !container.mustEquip()
                && current != SpellData.EMPTY && current.getSpell() == spellData.getSpell()
                && current.getLevel() == spellData.getLevel() && !current.isLocked();
    }

    private static @NotNull SpellData getScrollSpellData(ItemStack scroll) {
        if (scroll.isEmpty() || !(scroll.getItem() instanceof Scroll)) {
            return SpellData.EMPTY;
        }
        var container = ISpellContainer.get(scroll);
        if (container == null) {
            return SpellData.EMPTY;
        }
        var spellData = container.getSpellAtIndex(0);
        return spellData == null || spellData.getSpell() == null || spellData.getSpell().getCastType() != CastType.INSTANT
                ? SpellData.EMPTY : spellData;
    }

    private static boolean hasAdjustment(ItemStack stack, java.util.function.Predicate<ItemStack> predicate) {
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (predicate.test(CalibrationAdjustmentStorage.get(
                    stack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, serializationLookup()
            ))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSpellSlotUpgrade(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES);
    }

    private static boolean isSilverRing(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
    }

    private static @NotNull ItemStack getCalibrationItem(
            ItemStack owner, String listName, int slot, int slotCount,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(owner, slot, slotCount)) {
            return ItemStack.EMPTY;
        }
        var calibration = getCalibrationTag(owner);
        if (calibration == null || !calibration.contains(listName, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }
        var list = calibration.getList(listName, Tag.TAG_COMPOUND);
        for (var index = 0; index < list.size(); ++index) {
            var entry = list.getCompound(index);
            if (entry.getInt(SLOT_TAG) == slot && entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                return ItemStack.parseOptional(lookupProvider, entry.getCompound(ITEM_TAG));
            }
        }
        return ItemStack.EMPTY;
    }

    private static void setCalibrationItem(
            ItemStack owner, String listName, int slot, int slotCount, ItemStack item,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(owner, slot, slotCount)) {
            return;
        }
        updateCalibrationTag(owner, calibration -> {
            var list = calibration.contains(listName, Tag.TAG_LIST)
                    ? calibration.getList(listName, Tag.TAG_COMPOUND) : new ListTag();
            for (var index = list.size() - 1; index >= 0; --index) {
                if (list.getCompound(index).getInt(SLOT_TAG) == slot) {
                    list.remove(index);
                }
            }
            if (!item.isEmpty()) {
                var stored = item.copyWithCount(1);
                var entry = new CompoundTag();
                entry.putInt(SLOT_TAG, slot);
                entry.put(ITEM_TAG, stored.saveOptional(lookupProvider));
                list.add(entry);
            }
            if (list.isEmpty()) {
                calibration.remove(listName);
            } else {
                calibration.put(listName, list);
            }
        });
    }

    private static @Nullable CompoundTag getCalibrationTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        var root = customData.copyTag();
        return root.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND) ? root.getCompound(CALIBRATION_TAG) : null;
    }

    private static void updateCalibrationTag(ItemStack stack, Consumer<CompoundTag> updater) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var calibration = root.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)
                    ? root.getCompound(CALIBRATION_TAG) : new CompoundTag();
            updater.accept(calibration);
            if (calibration.isEmpty()) {
                root.remove(CALIBRATION_TAG);
            } else {
                root.put(CALIBRATION_TAG, calibration);
            }
        });
    }

    private static HolderLookup.Provider serializationLookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? FALLBACK_SERIALIZATION_LOOKUP : server.registryAccess();
    }

    private static boolean isValidCalibrationAccess(ItemStack stack, int slot, int slotCount) {
        return !stack.isEmpty() && stack.getItem() instanceof ChargecastCatalystbook
                && slot >= 0 && slot < slotCount;
    }

    public record TooltipValues(int castTimeTicks, double spellPowerMultiplier) {
    }
}
