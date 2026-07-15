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
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.tags.TagKey;
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
        ManaBypassSpellItem, CastAnimationOverrideItem, IJeiInfoItem, NonDamageableAnvilMergeItem {
    private static final String JEI_INFO_GROUP_ID = "spellgun_items";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.spellgun_items.desc_";
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_SHATTER_CAPABLE_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_shatter_capable_weapon")
    );
    private static final double ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL = 0.02D;
    private static final double REFLUX_MANA_REGEN_PER_LEVEL = 0.05D;
    private static final double RESERVOIR_MAX_MANA_PER_LEVEL = 20.0D;
    private static final double SURGE_SPELL_POWER_PER_LEVEL = 0.02D;
    private static final double ATTUNEMENT_SPELL_POWER_PER_LEVEL = 0.04D;
    private static final double TENSE_CAST_TIME_REDUCTION_PER_LEVEL = 0.05D;
    public static final float EMPTY_CASING_RETURN_CHANCE = 0.5F;
    private final SpellGunConfig spellGunConfig;
    private final Supplier<? extends AbstractSpell> configuredSpell;
    private final int configuredSpellLevel;
    private final boolean startsWithPresetSpell;
    private final String itemKey;
    private final List<AttributeBonus> handBonuses;
    private final ItemAttributeModifiers baseMainhandModifiers;

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
        this.baseMainhandModifiers = buildBaseMainhandModifiers();
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
        this.baseMainhandModifiers = buildBaseMainhandModifiers();
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
        var castResult = tryCastSpell(player, stack, usedHand);
        return switch (castResult) {
            case SUCCESS -> InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            case FAIL -> InteractionResultHolder.fail(stack);
            case NONE -> InteractionResultHolder.pass(stack);
        };
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
        appendSpellGunHelpTooltip(stack, lines);
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return buildMainhandModifiers(stack, baseMainhandModifiers);
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

    private CastResult tryCastSpell(Player player, ItemStack stack, InteractionHand usedHand) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return CastResult.NONE;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY || !canImbueSpell(spellData)) {
            return CastResult.FAIL;
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var slotId = usedHand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
        return tryCastSpellWithoutMana(player, stack, spellData, spellLevel, slotId, spell)
                ? CastResult.SUCCESS
                : CastResult.FAIL;
    }

    private boolean tryCastSpellWithoutMana(Player player, ItemStack stack, SpellData spellData, int spellLevel, String slotId, AbstractSpell spell) {
        var magicData = MagicData.getPlayerMagicData(player);
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
                        Component.translatable("ui.apprenticecodex.missing_spell_gun_ammo", ammoItem.getDescription())
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

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public String getJeiInfoGroupId() {
        return JEI_INFO_GROUP_ID;
    }

    private ItemAttributeModifiers buildBaseMainhandModifiers() {
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
                    itemKey + "_mainhand_" + attributeKey + "_" + i
            );
            builder.add(
                    attribute,
                    new AttributeModifier(modifierId, bonus.amount(), bonus.operation()),
                    EquipmentSlotGroup.MAINHAND
            );
        }
        return builder.build();
    }

    private ItemAttributeModifiers buildMainhandModifiers(ItemStack stack, ItemAttributeModifiers defaultModifiers) {
        var baseModifiers = stripManagedEnchantmentModifiers(defaultModifiers);
        if (stack == null || stack.isEmpty() || !stack.isEnchanted()) {
            return baseModifiers;
        }

        var alacrityLevel = Enchantments.getLevel(stack, Enchantments.ALACRITY);
        var refluxLevel = Enchantments.getLevel(stack, Enchantments.REFLUX);
        var reservoirLevel = Enchantments.getLevel(stack, Enchantments.RESERVOIR);
        var surgeLevel = Enchantments.getLevel(stack, Enchantments.SURGE);
        var attunementLevel = Enchantments.getLevel(stack, Enchantments.ATTUNEMENT);
        var tenseLevel = Enchantments.getLevel(stack, Enchantments.TENSE);
        if (alacrityLevel <= 0
                && refluxLevel <= 0
                && reservoirLevel <= 0
                && surgeLevel <= 0
                && attunementLevel <= 0
                && tenseLevel <= 0) {
            return baseModifiers;
        }

        var builder = ItemAttributeModifiers.builder();
        for (var entry : baseModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }

        addEnchantmentModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION,
                alacrityLevel * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "alacrity_cooldown_reduction"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MANA_REGEN,
                refluxLevel * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "reflux_mana_regen"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MAX_MANA,
                reservoirLevel * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADD_VALUE,
                "reservoir_max_mana"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.SPELL_POWER,
                surgeLevel * SURGE_SPELL_POWER_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "surge_spell_power"
        );
        if (attunementLevel > 0) {
            var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
            var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            if (attunementSpellPowerAttribute != null) {
                addEnchantmentModifier(
                        builder,
                        BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementSpellPowerAttribute),
                        attunementLevel * ATTUNEMENT_SPELL_POWER_PER_LEVEL,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                        "attunement_spell_power"
                );
            }
        }
        addEnchantmentModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION,
                tenseLevel * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "tense_cast_time_reduction"
        );

        return mergeTooltipEquivalentModifiers(builder.build(), itemKey + "_mainhand_merged");
    }

    private ItemAttributeModifiers stripManagedEnchantmentModifiers(ItemAttributeModifiers modifiers) {
        if (modifiers.modifiers().isEmpty()) {
            return modifiers;
        }

        var enchantModifierPrefix = itemKey + "_mainhand_enchant_";
        var mergedModifierPrefix = itemKey + "_mainhand_merged_";
        var builder = ItemAttributeModifiers.builder();
        boolean changed = false;
        for (var entry : modifiers.modifiers()) {
            var modifierPath = entry.modifier().id().getPath();
            if (modifierPath.startsWith(enchantModifierPrefix) || modifierPath.startsWith(mergedModifierPrefix)) {
                changed = true;
                continue;
            }

            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        return changed ? builder.build() : modifiers;
    }

    private void addEnchantmentModifier(
            ItemAttributeModifiers.Builder builder,
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        if (amount == 0.0D) {
            return;
        }

        var modifierId = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                itemKey + "_mainhand_enchant_" + key
        );
        builder.add(attribute, new AttributeModifier(modifierId, amount, operation), EquipmentSlotGroup.MAINHAND);
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
                    ImbueTooltipHelper.formatTooltipSeconds(overriddenCooldownTicks)
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
            boolean instantLongCast
    ) {
        public SpellGunConfig {
            supportedCastTypes = Set.copyOf(Objects.requireNonNull(supportedCastTypes));
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
    }

    protected record AmmoTooltipEntry(Item item, @Nullable String conditionTranslationKey) {
        public AmmoTooltipEntry {
            Objects.requireNonNull(item);
        }
    }
}
