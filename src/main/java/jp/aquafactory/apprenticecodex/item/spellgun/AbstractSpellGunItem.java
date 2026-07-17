package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHint;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import jp.aquafactory.apprenticecodex.item.CastAnimationOverrideItem;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;

public abstract class AbstractSpellGunItem extends Item implements IPresetSpellContainer, RestrictedSpellImbuableItem,
        ManaBypassSpellItem, CastAnimationOverrideItem, IJeiInfoItem, NonDamageableAnvilMergeItem,
        SpellCalibrationAdjustmentTarget, TranscendencePolicy, AttributeEnchantmentPolicy, WisdomPolicy,
        PlunderTarget {
    private static final String JEI_INFO_GROUP_ID = "spellgun_items";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.spellgun_items.desc_";
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_SHATTER_CAPABLE_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_shatter_capable_weapon")
    );
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 1;
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            CalibrationAdjustmentProfile.of(
                    CalibrationAdjustmentRule.unique(
                            stack -> stack.is(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                            new CalibrationAdjustmentHint.SpecificItem(ItemRegistry.SILVER_SPELL_AMPLIFIER)
                    )
            );
    private static final String CALIBRATION_TAG = "SpellgunCalibration";
    private static final String ADJUSTMENT_TAG = "Adjustment";
    private static final String ID_ONLY_ADJUSTMENT_ITEM_TAG = "AdjustmentItem";
    private static final String ITEM_ID_TAG = "id";
    private static final HolderLookup.Provider FALLBACK_SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    public static final float EMPTY_CASING_RETURN_CHANCE = 0.5F;
    private final SpellGunConfig spellGunConfig;
    private final Supplier<? extends AbstractSpell> configuredSpell;
    private final int configuredSpellLevel;
    private final boolean startsWithPresetSpell;
    private final String itemKey;
    private final List<AttributeBonus> handBonuses;

    protected AbstractSpellGunItem(
            Properties properties,
            SpellGunConfig spellGunConfig,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel
    ) {
        this(properties, spellGunConfig, configuredSpell, configuredSpellLevel, null, List.of());
    }

    protected AbstractSpellGunItem(
            Properties properties,
            SpellGunConfig spellGunConfig,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            String itemKey,
            List<AttributeBonus> handBonuses
    ) {
        super(properties);
        this.spellGunConfig = Objects.requireNonNull(spellGunConfig);
        this.configuredSpell = Objects.requireNonNull(configuredSpell);
        this.configuredSpellLevel = configuredSpellLevel;
        this.startsWithPresetSpell = true;
        this.itemKey = normalizeKeyToken(itemKey != null ? itemKey : getClass().getSimpleName());
        this.handBonuses = List.copyOf(handBonuses);
    }

    protected AbstractSpellGunItem(
            Properties properties,
            SpellGunConfig spellGunConfig,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            String itemKey,
            AttributeBonus... handBonuses
    ) {
        this(properties, spellGunConfig, configuredSpell, configuredSpellLevel, itemKey, List.of(handBonuses));
    }

    protected AbstractSpellGunItem(
            Properties properties,
            SpellGunConfig spellGunConfig,
            String itemKey,
            List<AttributeBonus> handBonuses
    ) {
        super(properties);
        this.spellGunConfig = Objects.requireNonNull(spellGunConfig);
        this.configuredSpell = null;
        this.configuredSpellLevel = 0;
        this.startsWithPresetSpell = false;
        this.itemKey = normalizeKeyToken(itemKey != null ? itemKey : getClass().getSimpleName());
        this.handBonuses = List.copyOf(handBonuses);
    }

    protected AbstractSpellGunItem(
            Properties properties,
            SpellGunConfig spellGunConfig,
            String itemKey,
            AttributeBonus... handBonuses
    ) {
        this(properties, spellGunConfig, itemKey, List.of(handBonuses));
    }

    @Override
    public final void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        if (repairPresetSpellContainerStateIfNeeded(itemStack)) {
            return;
        }

        if (ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        // createImbuedContainer は spellWheel を有効化するため、spell gun では明示的に無効のまま組み立てる.
        var spellContainer = ISpellContainer.create(1, false, false).mutableCopy();
        if (startsWithPresetSpell) {
            InitialSpellContainerHelper.addInitialSpellIfEnabled(
                    spellContainer,
                    configuredSpell,
                    configuredSpellLevel,
                    0,
                    true
            );
        }
        ISpellContainer.set(itemStack, spellContainer.toImmutable());
    }

    public final boolean repairPresetSpellContainerStateIfNeeded(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        if (PresetSpellContainerStateHelper.restoreIfNeeded(itemStack, 1, false, false, this::canImbueSpell)) {
            return true;
        }

        return normalizeLegacyOverriddenSpellContainerIfNeeded(itemStack);
    }

    @Override
    public final @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand == InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            tryTriggerImbuedSpell(serverPlayer, usedHand, null);
        }
        // オフハンド Spellgun が選ばれた後の失敗をメインハンドへフォールバックさせない。
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 1;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return ALL_ATTRIBUTE_ENCHANTMENTS;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        return enchantmentId != null && isMalumSpiritPlunder(stack, enchantmentId);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(
                "item." + ApprenticeCodex.MODID + ".common.spellgun.desc_1",
                ImbueTooltipHelper.getAttackKeyName()
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                "item." + ApprenticeCodex.MODID + ".common.spellgun.desc_2",
                ImbueTooltipHelper.getUseKeyName()
        ).withStyle(ChatFormatting.GRAY));
        appendSpellGunHelpTooltip(stack, lines);
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        var slotGroup = usesOffhandAttributeModifiers(stack)
                ? EquipmentSlotGroup.OFFHAND
                : EquipmentSlotGroup.MAINHAND;
        var handToken = slotGroup == EquipmentSlotGroup.OFFHAND ? "offhand" : "mainhand";
        return buildHandModifiers(stack, slotGroup, handToken);
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    @Override
    public @NotNull ItemStack getCalibrationAdjustment(@NotNull ItemStack targetStack, int slot) {
        return getCalibrationAdjustment(targetStack, slot, serializationLookup());
    }

    @Override
    public @NotNull ItemStack getCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(targetStack, slot)) {
            return ItemStack.EMPTY;
        }

        var customData = targetStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return ItemStack.EMPTY;
        }
        var rootTag = customData.copyTag();
        if (!rootTag.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        var calibrationTag = rootTag.getCompound(CALIBRATION_TAG);
        if (calibrationTag.contains(ADJUSTMENT_TAG, Tag.TAG_COMPOUND)) {
            var adjustmentTag = calibrationTag.getCompound(ADJUSTMENT_TAG);
            var adjustment = parseAdjustment(lookupProvider, adjustmentTag);
            if (!adjustment.isEmpty()) {
                return adjustment;
            }
            var fallbackAdjustment = createItemStack(adjustmentTag.getString(ITEM_ID_TAG));
            if (!fallbackAdjustment.isEmpty()) {
                return fallbackAdjustment;
            }
        }

        return createItemStack(calibrationTag.getString(ID_ONLY_ADJUSTMENT_ITEM_TAG));
    }

    private static @NotNull ItemStack parseAdjustment(
            @NotNull HolderLookup.Provider lookupProvider,
            @NotNull CompoundTag adjustmentTag
    ) {
        if (adjustmentTag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.CODEC
                .parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), adjustmentTag)
                .result()
                .orElse(ItemStack.EMPTY);
    }

    private static @NotNull ItemStack createItemStack(@NotNull String itemIdString) {
        var itemId = ResourceLocation.tryParse(itemIdString);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public boolean trySetCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment
    ) {
        return trySetCalibrationAdjustment(targetStack, slot, adjustment, serializationLookup());
    }

    @Override
    public boolean trySetCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!canPlaceCalibrationAdjustment(targetStack, slot, adjustment, lookupProvider)) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, rootTag -> {
            if (adjustment.isEmpty()) {
                rootTag.remove(CALIBRATION_TAG);
                return;
            }

            var storedAdjustment = adjustment.copy();
            storedAdjustment.setCount(1);
            var calibrationTag = rootTag.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)
                    ? rootTag.getCompound(CALIBRATION_TAG)
                    : new CompoundTag();
            calibrationTag.put(ADJUSTMENT_TAG, storedAdjustment.saveOptional(lookupProvider));
            calibrationTag.remove(ID_ONLY_ADJUSTMENT_ITEM_TAG);
            rootTag.put(CALIBRATION_TAG, calibrationTag);
        });
        return true;
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_PROFILE;
    }

    public static boolean usesOffhandAttributeModifiers(@NotNull ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof AbstractSpellGunItem spellGun
                && ItemRegistry.SILVER_SPELL_AMPLIFIER.getId().equals(
                        spellGun.getCalibrationAdjustmentItemId(stack)
                );
    }

    private static boolean isValidCalibrationAccess(ItemStack stack, int slot) {
        return slot == 0 && !stack.isEmpty() && stack.getItem() instanceof AbstractSpellGunItem;
    }

    private @Nullable ResourceLocation getCalibrationAdjustmentItemId(@NotNull ItemStack targetStack) {
        var customData = targetStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        var rootTag = customData.copyTag();
        if (!rootTag.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }

        var calibrationTag = rootTag.getCompound(CALIBRATION_TAG);
        if (calibrationTag.contains(ADJUSTMENT_TAG, Tag.TAG_COMPOUND)) {
            var itemId = ResourceLocation.tryParse(
                    calibrationTag.getCompound(ADJUSTMENT_TAG).getString(ITEM_ID_TAG)
            );
            if (itemId != null) {
                return itemId;
            }
        }
        return ResourceLocation.tryParse(calibrationTag.getString(ID_ONLY_ADJUSTMENT_ITEM_TAG));
    }

    private static HolderLookup.Provider serializationLookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? FALLBACK_SERIALIZATION_LOOKUP : server.registryAccess();
    }

    public final boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    public final boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        if (spell == null || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()) {
            return false;
        }
        if (SpellGunSpellListManager.isDenylisted(spell)) {
            return false;
        }

        var spellGunCastType = SpellGunCastType.from(spell.getCastType());
        if (spellGunCastType == null || !spellGunConfig.supports(spellGunCastType)) {
            return false;
        }

        return passesImbueConditions(spell, spellLevel);
    }

    @Override
    public final void normalizeImbuedSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        var spellData = getPrimarySpellData(stack);
        var normalized = ISpellContainer.create(1, false, false).mutableCopy();
        if (spellData != null && canImbueSpell(spellData)) {
            // Arcane Anvil で差し替えた呪文まで固定すると Workbench 抽出不能になるため、preset 以外は removable に戻す。
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
            PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        } else {
            PresetSpellContainerStateHelper.clearRememberedState(stack);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    private boolean normalizeLegacyOverriddenSpellContainerIfNeeded(ItemStack stack) {
        var spellData = getPrimarySpellData(stack);
        if (spellData == null
                || spellData.canRemove()
                || !canImbueSpell(spellData)
                || matchesConfiguredPresetSpell(spellData)) {
            return false;
        }

        var normalized = ISpellContainer.create(1, false, false).mutableCopy();
        if (!normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false)) {
            return false;
        }

        ISpellContainer.set(stack, normalized.toImmutable());
        PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        return true;
    }

    @Override
    public final boolean supportsManaBypass(@Nullable AbstractSpell spell) {
        if (spell == null) {
            return false;
        }

        var spellGunCastType = SpellGunCastType.from(spell.getCastType());
        return spellGunCastType != null && spellGunConfig.supports(spellGunCastType);
    }

    @Nullable
    protected final SpellData getPrimarySpellData(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        return spellData == SpellData.EMPTY ? null : spellData;
    }

    @Nullable
    public final SpellData getImbuedSpellData(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }
        return getPrimarySpellData(stack);
    }

    private boolean matchesConfiguredPresetSpell(@Nullable SpellData spellData) {
        return spellData != null
                && startsWithPresetSpell
                && configuredSpell != null
                && configuredSpell.get().equals(spellData.getSpell())
                && configuredSpellLevel == spellData.getLevel();
    }

    @Nullable
    public final Item getDisplayedAmmoItem(ItemStack stack) {
        // Spellgun は手元参照だけでは初期化されないため、HUD 判定前に一度だけ補完する。
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var spellData = getPrimarySpellData(stack);
        if (spellData == null || !canImbueSpell(spellData)) {
            return null;
        }

        return getAmmoItem(stack, spellData);
    }

    @Nullable
    public Item getAmmoItem(ItemStack stack, @Nullable SpellData spellData) {
        return null;
    }

    final boolean shouldReturnEmptyCasing(Player player) {
        var emptyCasingReturnChance = SpellcasterAmmoPouch.applyEmptyCasingReturnChanceBonus(
                EMPTY_CASING_RETURN_CHANCE,
                player
        );
        return emptyCasingReturnChance > 0.0F
                && player.getRandom().nextFloat() < emptyCasingReturnChance;
    }

    public boolean shouldOverrideSpellGunCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        if (!matchesSpellGunAnimationOverrideSpell(stack, spell)) {
            return false;
        }

        return spell.getCastType() == CastType.INSTANT || isZeroTickLongCastAnimationOverride(spell);
    }

    public AnimationHolder getSpellGunCastStartAnimation() {
        // モーションは全て片手INSTANTに上書きする.
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    public boolean shouldSuppressSpellGunCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesSpellGunAnimationOverrideSpell(stack, spell) && isZeroTickLongCastAnimationOverride(spell);
    }

    @Override
    public final boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return shouldOverrideSpellGunCastStartAnimation(stack, spell);
    }

    @Override
    public final AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return getSpellGunCastStartAnimation();
    }

    @Override
    public final boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return shouldSuppressSpellGunCastFinishAnimation(stack, spell);
    }
    protected List<AmmoTooltipEntry> getAmmoTooltipEntries(ItemStack stack) {
        var ammoItem = getAmmoItem(stack, getPrimarySpellData(stack));
        if (ammoItem == null) {
            return List.of();
        }

        // 単純な spell gun は実際の消費弾をそのまま表示し、条件分岐がある物だけ個別 override する.
        return List.of(new AmmoTooltipEntry(ammoItem, null));
    }

    @Nullable
    final Integer getOverriddenCooldownTicks() {
        return spellGunConfig.overriddenSpellCooldownTicks();
    }

    @Nullable
    final Integer getAdjustedCooldownTicks(int originalCooldownTicks) {
        return spellGunConfig.adjustedSpellCooldownTicks(originalCooldownTicks);
    }

    final boolean isRecastCast(@Nullable MagicData magicData, @Nullable AbstractSpell spell) {
        return magicData != null
                && spell != null
                && magicData.getPlayerRecasts().hasRecastForSpell(spell);
    }

    @Nullable
    private Integer getOverriddenLongCastTicks() {
        return spellGunConfig.instantLongCast() ? 0 : null;
    }

    private boolean matchesSpellGunAnimationOverrideSpell(ItemStack stack, @Nullable AbstractSpell spell) {
        if (spell == null) {
            return false;
        }

        var spellData = getPrimarySpellData(stack);
        if (spellData != null) {
            return spellData.getSpell().equals(spell);
        }

        return startsWithPresetSpell && configuredSpell != null && configuredSpell.get().equals(spell);
    }

    private boolean isZeroTickLongCastAnimationOverride(AbstractSpell spell) {
        return spell.getCastType() == CastType.LONG
                && spellGunConfig.supports(SpellGunCastType.LONG)
                && spellGunConfig.instantLongCast();
    }

    private boolean passesImbueConditions(AbstractSpell spell, int spellLevel) {
        var maxCooldownTicks = spellGunConfig.maxInstantImbueCooldownTicks();
        if (maxCooldownTicks != null && spell.getSpellCooldown() > maxCooldownTicks) {
            return false;
        }
        return !spellGunConfig.requireZeroInstantRecast() || spell.getRecastCount(spellLevel, null) <= 0;
    }

    public final boolean tryTriggerImbuedSpell(ServerPlayer player, InteractionHand usedHand,
                                                @Nullable BlockTargetData targetData) {
        var stack = player.getItemInHand(usedHand);
        if (stack.isEmpty() || stack.getItem() != this) {
            return false;
        }

        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            sendNotImbuedError(player, stack);
            BlockTargetingHelper.clearPendingServerTarget(player);
            return false;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY) {
            sendNotImbuedError(player, stack);
            BlockTargetingHelper.clearPendingServerTarget(player);
            return false;
        }

        if (!canImbueSpell(spellData)) {
            sendInvalidSpellError(player, stack, spellData);
            BlockTargetingHelper.clearPendingServerTarget(player);
            return false;
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var slotId = usedHand == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND
                : SpellSelectionManager.MAINHAND;

        if (targetData != null) {
            BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), targetData);
        }

        try {
            return tryCastSpellWithoutMana(player, stack, spellData, spellLevel, slotId, spell);
        } finally {
            BlockTargetingHelper.clearPendingServerTarget(player);
        }
    }

    private boolean tryCastSpellWithoutMana(Player player, ItemStack stack, SpellData spellData, int spellLevel, String slotId, AbstractSpell spell) {
        var magicData = MagicData.getPlayerMagicData(player);
        // attemptInitiateCast は既存詠唱を壊す場合があるため、同時入力は先に開始済みの詠唱を優先する。
        if (magicData != null && magicData.isCasting()) {
            return false;
        }

        if (magicData == null || player.isCreative()) {
            var casted = spell.attemptInitiateCast(stack, spellLevel, player.level(), player, CastSource.SWORD, true, slotId);
            if (casted) {
                TriggeredSpellCastHelper.applyLongCastDurationOverride(
                        player,
                        spellLevel,
                        spell,
                        magicData,
                        slotId,
                        getOverriddenLongCastTicks()
                );
            }
            return casted;
        }

        var ammoItem = getAmmoItem(stack, spellData);
        if (ammoItem != null && !isRecastCast(magicData, spell) && !SpellGunCastEvent.hasAmmo(player, player.getInventory(), ammoItem)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.apprenticecodex.spellgun.missing_ammo", ammoItem.getDescription())
                                .withStyle(ChatFormatting.RED)
                ));
            }
            return false;
        }

        var borrowedMana = Math.max(0f, spell.getManaCost(spellLevel) - magicData.getMana());
        if (borrowedMana > 0f) {
            // 魔法詠唱はマナがいるため、事前に満たす量だけ補充する(後で剥奪する)
            magicData.addMana(borrowedMana);
        }

        var casted = spell.attemptInitiateCast(stack, spellLevel, player.level(), player, CastSource.SWORD, true, slotId);
        if (!casted && borrowedMana > 0f) {
            magicData.setMana(Math.max(0f, magicData.getMana() - borrowedMana));
            return false;
        }
        if (!casted) {
            return false;
        }

        if (borrowedMana > 0f) {
            ItemManaBypassCastEvent.reserveBorrowedMana(player, borrowedMana);
        }

        TriggeredSpellCastHelper.applyLongCastDurationOverride(
                player,
                spellLevel,
                spell,
                magicData,
                slotId,
                getOverriddenLongCastTicks()
        );
        return true;
    }

    private static void sendNotImbuedError(ServerPlayer player, ItemStack stack) {
        player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("ui.apprenticecodex.spellgun.not_imbued", stack.getHoverName())
                        .withStyle(ChatFormatting.RED)
        ));
    }

    private static void sendInvalidSpellError(ServerPlayer player, ItemStack stack, SpellData spellData) {
        player.connection.send(new ClientboundSetActionBarTextPacket(
                createInvalidSpellError(player, stack, spellData)
        ));
    }

    private static Component createInvalidSpellError(Player player, ItemStack stack, SpellData spellData) {
        return Component.translatable(
                "ui.apprenticecodex.spellgun.invalid_spell",
                spellData.getSpell().getDisplayName(player),
                stack.getHoverName()
        ).withStyle(ChatFormatting.RED);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public String getJeiInfoGroupId() {
        return JEI_INFO_GROUP_ID;
    }

    private ItemAttributeModifiers buildBaseHandModifiers(
            EquipmentSlotGroup slotGroup,
            String handToken
    ) {
        var builder = ItemAttributeModifiers.builder();
        for (int i = 0; i < handBonuses.size(); ++i) {
            var bonus = handBonuses.get(i);
            var attribute = bonus.attribute();
            if (attribute == null || bonus.amount() == 0.0D) {
                continue;
            }

            var attributeKey = resolveAttributeKey(bonus, i);
            var modifierId = ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID,
                    itemKey + "_" + handToken + "_" + attributeKey + "_" + i
            );
            builder.add(
                    attribute,
                    new AttributeModifier(modifierId, bonus.amount(), bonus.operation()),
                    slotGroup
            );
        }
        return builder.build();
    }

    private ItemAttributeModifiers buildHandModifiers(
            ItemStack stack,
            EquipmentSlotGroup slotGroup,
            String handToken
    ) {
        var baseModifiers = buildBaseHandModifiers(slotGroup, handToken);
        if (stack == null || stack.isEmpty() || !stack.isEnchanted()) {
            return baseModifiers;
        }

        var builder = ItemAttributeModifiers.builder();
        for (var entry : baseModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        var prefix = itemKey + "_" + handToken + "_enchant";
        if (!AttributeEnchantmentResolver.addModifiers(builder, stack, slotGroup, prefix)) {
            return baseModifiers;
        }

        return mergeTooltipEquivalentModifiers(
                builder.build(), itemKey + "_" + handToken + "_merged"
        );
    }

    private static ItemAttributeModifiers mergeTooltipEquivalentModifiers(
            ItemAttributeModifiers modifiers,
            String modifierPathPrefix
    ) {
        if (modifiers.modifiers().isEmpty()) {
            return modifiers;
        }

        var merged = new LinkedHashMap<MergeTarget, MergedModifier>();
        var passthrough = new ArrayList<ItemAttributeModifiers.Entry>();
        int unknownIndex = 0;
        for (var entry : modifiers.modifiers()) {
            var operation = entry.modifier().operation();
            // ADD_MULTIPLIED_TOTAL は線形合算できないため、挙動維持のためそのまま残す。
            if (operation != AttributeModifier.Operation.ADD_VALUE
                    && operation != AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                passthrough.add(entry);
                continue;
            }

            var attributeToken = resolveAttributeToken(entry.attribute(), unknownIndex++);
            var target = new MergeTarget(attributeToken, operation, entry.slot());
            var existing = merged.get(target);
            if (existing == null) {
                merged.put(target, new MergedModifier(entry.attribute(), entry.modifier().amount()));
            } else {
                merged.put(target, new MergedModifier(existing.attribute(), existing.amount() + entry.modifier().amount()));
            }
        }

        var builder = ItemAttributeModifiers.builder();
        int mergedIndex = 0;
        for (Map.Entry<MergeTarget, MergedModifier> entry : merged.entrySet()) {
            var target = entry.getKey();
            var mergedModifier = entry.getValue();
            if (mergedModifier.amount() == 0.0D) {
                continue;
            }

            var operationToken = target.operation().name().toLowerCase(Locale.ROOT);
            var modifierId = ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID,
                    modifierPathPrefix + "_" + target.attributeToken() + "_" + operationToken + "_" + mergedIndex++
            );
            builder.add(
                    mergedModifier.attribute(),
                    new AttributeModifier(modifierId, mergedModifier.amount(), target.operation()),
                    target.slot()
            );
        }

        for (var entry : passthrough) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        return builder.build();
    }

    private void appendSpellGunHelpTooltip(ItemStack stack, List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectSpellGunAbilityTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectSpellGunRestrictTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
        appendSpellGunAmmoTooltipSection(
                stack,
                lines,
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ammo_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ammo_none"
        );
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
    }

    private List<Component> collectSpellGunAbilityTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_no_mana"
        ));

        var overriddenCooldownTicks = getOverriddenCooldownTicks();
        if (overriddenCooldownTicks != null) {
            translatedLines.add(ImbueTooltipHelper.translatableGray(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_reduce_recast",
                    ImbueTooltipHelper.formatTooltipSeconds(
                            ImbueTooltipHelper.resolveClientCooldownReductionAdjustedTicks(overriddenCooldownTicks)
                    )
            ));
        }

        var cooldownReductionTicks = spellGunConfig.cooldownReductionTicks();
        var reducedCooldownMinimumTicks = spellGunConfig.reducedCooldownMinimumTicks();
        if (cooldownReductionTicks != null && reducedCooldownMinimumTicks != null) {
            translatedLines.add(ImbueTooltipHelper.translatableGray(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_subtract_cooldown",
                    ImbueTooltipHelper.formatTooltipSeconds(cooldownReductionTicks),
                    ImbueTooltipHelper.formatTooltipSeconds(reducedCooldownMinimumTicks)
            ));
        }

        if (spellGunConfig.instantLongCast()) {
            translatedLines.add(ImbueTooltipHelper.translatableGray(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
            ));
        }
        return translatedLines;
    }

    private List<Component> collectSpellGunRestrictTooltipSection() {
        var translatedLines = new ArrayList<>(ImbueTooltipHelper.collectCastTypeRestrictionLines(spellGunConfig.supportedCastTypes()));
        ImbueTooltipHelper.appendMaxCooldownRestrictionLine(translatedLines, spellGunConfig.maxInstantImbueCooldownTicks());
        ImbueTooltipHelper.appendNoRecastRestrictionLine(translatedLines, spellGunConfig.requireZeroInstantRecast());
        return translatedLines;
    }

    @Override
    public final List<Component> getImbueRestrictionTooltipLines() {
        return collectSpellGunRestrictTooltipSection();
    }

    private void appendSpellGunAmmoTooltipSection(
            ItemStack stack,
            List<Component> lines,
            String titleTranslationKey,
            String emptyTranslationKey
    ) {
        var sectionLines = collectSpellGunAmmoTooltipSection(stack);
        ImbueTooltipHelper.appendTooltipSection(lines, sectionLines, titleTranslationKey, emptyTranslationKey);
    }

    private List<Component> collectSpellGunAmmoTooltipSection(ItemStack stack) {
        var translatedLines = new ArrayList<Component>();
        for (var entry : getAmmoTooltipEntries(stack)) {
            translatedLines.add(ImbueTooltipHelper.createAmmoTooltipLine(entry.item(), entry.conditionTranslationKey()));
        }
        return translatedLines;
    }

    private static String resolveAttributeToken(Holder<Attribute> attribute, int index) {
        return attribute.unwrapKey()
                .map(resourceKey -> normalizeKeyToken(resourceKey.location().toString()))
                .orElse("unknown_" + index);
    }

    private static String resolveAttributeKey(AttributeBonus bonus, int index) {
        if (bonus.key() != null && !bonus.key().isBlank()) {
            return normalizeKeyToken(bonus.key());
        }
        return bonus.attribute().unwrapKey()
                .map(resourceKey -> normalizeKeyToken(resourceKey.location().toString()))
                .orElse("unknown_" + index);
    }

    private static boolean isMalumSpiritPlunder(ItemStack stack, ResourceLocation enchantmentId) {
        return MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(MALUM_SOUL_SHATTER_CAPABLE_WEAPON);
    }

    private static String normalizeKeyToken(String token) {
        return Objects.requireNonNull(token)
                .toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_')
                .replace('.', '_')
                .replaceAll("[^a-z0-9_-]", "_");
    }

    // `bonus` ヘルパーは属性参照の受け取り方ごとにオーバーロードしている.
    // 将来の spell gun 実装で属性の持ち方が異なっても同じ書き味で定義できるようにしている.

    // Forge の RegistryObject や Deferred 登録由来の Supplier をそのまま渡す用途.
    protected static AttributeBonus bonus(
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attribute, amount, operation, null);
    }

    protected static AttributeBonus bonus(
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attribute, amount, operation, key);
    }

    private enum CastResult {
        NONE,
        SUCCESS,
        FAIL
    }

    // `key` は属性補正 ID 生成のシードに使う任意識別子。
    // null の場合は属性の登録キーを優先して使用する。
    protected record AttributeBonus(
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            @Nullable String key
    ) {
        public AttributeBonus {
            Objects.requireNonNull(attribute);
            Objects.requireNonNull(operation);
        }
    }
    private record MergeTarget(
            String attributeToken,
            AttributeModifier.Operation operation,
            EquipmentSlotGroup slot
    ) {
    }

    private record MergedModifier(
            Holder<Attribute> attribute,
            double amount
    ) {
    }

    public record SpellGunConfig(
            Set<SpellGunCastType> supportedCastTypes,
            @Nullable IntSupplier maxInstantImbueCooldownTicksSupplier,
            boolean requireZeroInstantRecast,
            @Nullable IntSupplier overriddenSpellCooldownTicksSupplier,
            @Nullable IntSupplier cooldownReductionTicksSupplier,
            @Nullable IntSupplier reducedCooldownMinimumTicksSupplier,
            boolean instantLongCast
    ) {
        public SpellGunConfig {
            supportedCastTypes = Set.copyOf(Objects.requireNonNull(supportedCastTypes));
            if ((cooldownReductionTicksSupplier == null) != (reducedCooldownMinimumTicksSupplier == null)) {
                throw new IllegalArgumentException("Cooldown reduction and minimum suppliers must be configured together");
            }
            if (overriddenSpellCooldownTicksSupplier != null && cooldownReductionTicksSupplier != null) {
                throw new IllegalArgumentException("Cooldown override and reduction cannot be configured together");
            }
        }

        public boolean supports(SpellGunCastType castType) {
            return supportedCastTypes.contains(castType);
        }

        @Nullable
        public Integer maxInstantImbueCooldownTicks() {
            if (maxInstantImbueCooldownTicksSupplier == null) {
                return null;
            }
            var ticks = maxInstantImbueCooldownTicksSupplier.getAsInt();
            return ticks <= 0 ? null : ticks;
        }

        @Nullable
        public Integer overriddenSpellCooldownTicks() {
            return overriddenSpellCooldownTicksSupplier == null
                    ? null
                    : Math.max(0, overriddenSpellCooldownTicksSupplier.getAsInt());
        }

        @Nullable
        public Integer cooldownReductionTicks() {
            return cooldownReductionTicksSupplier == null
                    ? null
                    : Math.max(0, cooldownReductionTicksSupplier.getAsInt());
        }

        @Nullable
        public Integer reducedCooldownMinimumTicks() {
            return reducedCooldownMinimumTicksSupplier == null
                    ? null
                    : Math.max(0, reducedCooldownMinimumTicksSupplier.getAsInt());
        }

        @Nullable
        public Integer adjustedSpellCooldownTicks(int originalCooldownTicks) {
            var overriddenTicks = overriddenSpellCooldownTicks();
            if (overriddenTicks != null) {
                return overriddenTicks;
            }

            var reductionTicks = cooldownReductionTicks();
            var minimumTicks = reducedCooldownMinimumTicks();
            if (reductionTicks == null || minimumTicks == null) {
                return null;
            }

            var originalTicks = Math.max(0, originalCooldownTicks);
            var reducedTicks = Math.max(minimumTicks, originalTicks - reductionTicks);
            // 短縮能力で元のクールダウンを延長しないよう、設定下限より短い魔法は元値を維持する。
            return Math.min(originalTicks, reducedTicks);
        }
    }

    protected record AmmoTooltipEntry(Item item, @Nullable String conditionTranslationKey) {
        public AmmoTooltipEntry {
            Objects.requireNonNull(item);
        }
    }
}
