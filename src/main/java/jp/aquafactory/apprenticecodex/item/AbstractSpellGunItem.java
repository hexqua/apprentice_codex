package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractSpellGunItem extends Item implements IPresetSpellContainer, RestrictedSpellImbuableItem,
        ManaBypassSpellItem, CastAnimationOverrideItem {
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
    private final Multimap<Attribute, AttributeModifier> mainhandModifiers;

    protected AbstractSpellGunItem(
            Properties properties,
            SpellGunConfig spellGunConfig,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel
    ) {
        this(
                properties,
                spellGunConfig,
                configuredSpell,
                configuredSpellLevel,
                null,
                List.of()
        );
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
        this.mainhandModifiers = buildBaseMainhandModifiers();
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
            SpellGunConfig spellGunConfig
    ) {
        this(properties, spellGunConfig, null, List.of());
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
        this.mainhandModifiers = buildBaseMainhandModifiers();
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
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        var spellContainer = ISpellContainer.create(1, false, false).mutableCopy();
        if (startsWithPresetSpell) {
            // createImbuedContainer は spellWheel を有効化するため、spell gun では明示的に無効のまま組み立てる.
            spellContainer.addSpellAtIndex(configuredSpell.get(), configuredSpellLevel, 0, true);
        }
        ISpellContainer.set(itemStack, spellContainer.toImmutable());
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
    public int getEnchantmentValue(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return false;
        }

        if (!ApprenticeCodex.MODID.equals(enchantmentId.getNamespace())) {
            return false;
        }

        return isSupportedSpellGunEnchantment(enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        appendSpellGunHelpTooltip(stack, lines);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            return buildMainhandModifiers(stack);
        }

        return super.getAttributeModifiers(slot, stack);
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
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, true);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
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

    public AnimationHolder getSpellGunCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
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
        return getSpellGunCastStartAnimation(stack, spell, spellLevel);
    }

    @Override
    public final boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return shouldSuppressSpellGunCastFinishAnimation(stack, spell);
    }

    private static boolean isSupportedSpellGunEnchantment(Enchantment enchantment) {
        return (EnchantmentRegistry.ALACRITY.isPresent() && enchantment == EnchantmentRegistry.ALACRITY.get())
                || (EnchantmentRegistry.REFLUX.isPresent() && enchantment == EnchantmentRegistry.REFLUX.get())
                || (EnchantmentRegistry.RESERVOIR.isPresent() && enchantment == EnchantmentRegistry.RESERVOIR.get())
                || (EnchantmentRegistry.SURGE.isPresent() && enchantment == EnchantmentRegistry.SURGE.get())
                || (EnchantmentRegistry.ATTUNEMENT.isPresent() && enchantment == EnchantmentRegistry.ATTUNEMENT.get())
                || (EnchantmentRegistry.TENSE.isPresent() && enchantment == EnchantmentRegistry.TENSE.get())
                || (EnchantmentRegistry.TRANSCENDENCE.isPresent() && enchantment == EnchantmentRegistry.TRANSCENDENCE.get())
                || (EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get())
                || (EnchantmentRegistry.PLUNDER.isPresent() && enchantment == EnchantmentRegistry.PLUNDER.get());
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
    private Integer getOverriddenLongCastTicks() {
        return spellGunConfig.overriddenLongCastDurationTicks();
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
                && spellGunConfig.overriddenLongCastDurationTicks() != null
                && spellGunConfig.overriddenLongCastDurationTicks() <= 0;
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
        var slotId = usedHand == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND
                : SpellSelectionManager.MAINHAND;

        return tryCastSpellWithoutMana(player, stack, spellData, spellLevel, slotId, spell)
                ? CastResult.SUCCESS
                : CastResult.FAIL;
    }

    private boolean tryCastSpellWithoutMana(Player player, ItemStack stack, SpellData spellData, int spellLevel, String slotId, AbstractSpell spell) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || player.isCreative()) {
            var casted = spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    CastSource.SWORD,
                    true,
                    slotId
            );
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
        if (ammoItem != null && !SpellGunCastEvent.hasAmmo(player, player.getInventory(), ammoItem)) {
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

        var casted = spell.attemptInitiateCast(
                stack,
                spellLevel,
                player.level(),
                player,
                CastSource.SWORD,
                true,
                slotId
        );
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

    private Multimap<Attribute, AttributeModifier> buildBaseMainhandModifiers() {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        var prefix = "apprenticecodex." + itemKey + ".mainhand";
        for (int i = 0; i < handBonuses.size(); ++i) {
            var bonus = handBonuses.get(i);
            var attribute = bonus.attributeSupplier().get();
            if (attribute == null) {
                continue;
            }

            var attributeKey = resolveAttributeKey(bonus, attribute, i);
            var modifierIdSeed = prefix + "." + attributeKey + "." + i;
            var modifierId = UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8));
            builder.put(
                    attribute,
                    new AttributeModifier(modifierId, modifierIdSeed, bonus.amount(), bonus.operation())
            );
        }
        return builder.build();
    }

    private Multimap<Attribute, AttributeModifier> buildMainhandModifiers(ItemStack stack) {
        var baseModifiers = mainhandModifiers;
        if (stack == null || stack.isEmpty()) {
            return baseModifiers;
        }
        if (!stack.isEnchanted()) {
            return baseModifiers;
        }
        if (!EnchantmentRegistry.ALACRITY.isPresent()
                || !EnchantmentRegistry.REFLUX.isPresent()
                || !EnchantmentRegistry.RESERVOIR.isPresent()
                || !EnchantmentRegistry.SURGE.isPresent()
                || !EnchantmentRegistry.ATTUNEMENT.isPresent()
                || !EnchantmentRegistry.TENSE.isPresent()) {
            return baseModifiers;
        }

        var alacrityLevel = stack.getEnchantmentLevel(EnchantmentRegistry.ALACRITY.get());
        var refluxLevel = stack.getEnchantmentLevel(EnchantmentRegistry.REFLUX.get());
        var reservoirLevel = stack.getEnchantmentLevel(EnchantmentRegistry.RESERVOIR.get());
        var surgeLevel = stack.getEnchantmentLevel(EnchantmentRegistry.SURGE.get());
        var attunementLevel = stack.getEnchantmentLevel(EnchantmentRegistry.ATTUNEMENT.get());
        var tenseLevel = stack.getEnchantmentLevel(EnchantmentRegistry.TENSE.get());

        if (alacrityLevel <= 0
                && refluxLevel <= 0
                && reservoirLevel <= 0
                && surgeLevel <= 0
                && attunementLevel <= 0
                && tenseLevel <= 0) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        var prefix = "apprenticecodex." + itemKey + ".mainhand.enchant";

        addEnchantmentModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION.get(),
                alacrityLevel * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".alacrity.cooldown_reduction"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MANA_REGEN.get(),
                refluxLevel * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".reflux.mana_regen"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MAX_MANA.get(),
                reservoirLevel * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADDITION,
                prefix + ".reservoir.max_mana"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.SPELL_POWER.get(),
                surgeLevel * SURGE_SPELL_POWER_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".surge.spell_power"
        );
        if (attunementLevel > 0) {
            var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
            var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            addEnchantmentModifier(
                    builder,
                    attunementSpellPowerAttribute,
                    attunementLevel * ATTUNEMENT_SPELL_POWER_PER_LEVEL,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    prefix + ".attunement.spell_power"
            );
        }
        addEnchantmentModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION.get(),
                tenseLevel * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".tense.cast_time_reduction"
        );

        return mergeTooltipEquivalentModifiers(builder.build(), prefix + ".merged");
    }

    private static void addEnchantmentModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String modifierIdSeed
    ) {
        if (attribute == null || amount == 0.0D) {
            return;
        }

        var modifierId = UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8));
        builder.put(attribute, new AttributeModifier(modifierId, modifierIdSeed, amount, operation));
    }

    private static Multimap<Attribute, AttributeModifier> mergeTooltipEquivalentModifiers(
            Multimap<Attribute, AttributeModifier> modifiers,
            String modifierSeedPrefix
    ) {
        if (modifiers.isEmpty()) {
            return modifiers;
        }

        var merged = new LinkedHashMap<MergeTarget, Double>();
        var passthrough = new java.util.ArrayList<Map.Entry<Attribute, AttributeModifier>>();
        for (var entry : modifiers.entries()) {
            var modifier = entry.getValue();
            var operation = modifier.getOperation();
            // MULTIPLY_TOTAL は線形合算できないため、挙動維持のためそのまま残す.
            if (operation != AttributeModifier.Operation.ADDITION
                    && operation != AttributeModifier.Operation.MULTIPLY_BASE) {
                passthrough.add(entry);
                continue;
            }

            var key = new MergeTarget(entry.getKey(), operation);
            merged.merge(key, modifier.getAmount(), Double::sum);
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        for (var entry : merged.entrySet()) {
            var target = entry.getKey();
            var amount = entry.getValue();
            if (amount == 0.0D) {
                continue;
            }

            var operationToken = target.operation().name().toLowerCase(Locale.ROOT);
            var attributeToken = resolveAttributeToken(target.attribute());
            var modifierIdSeed = modifierSeedPrefix + "." + attributeToken + "." + operationToken;
            var modifierId = UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8));
            builder.put(
                    target.attribute(),
                    new AttributeModifier(modifierId, modifierIdSeed, amount, target.operation())
            );
        }

        for (var entry : passthrough) {
            builder.put(entry);
        }
        return builder.build();
    }

    private void appendSpellGunHelpTooltip(ItemStack stack, List<Component> lines) {
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }

        if (!Screen.hasShiftDown()) {
            lines.add(Component.translatable("item." + ApprenticeCodex.MODID + ".spellgun.tooltip.hint")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        appendSpellGunTooltipSection(
                lines,
                collectSpellGunAbilityTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        appendSpellGunTooltipSection(
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

    private void appendSpellGunTooltipSection(
            List<Component> lines,
            List<Component> sectionLines,
            String titleTranslationKey,
            String emptyTranslationKey
    ) {
        lines.add(Component.translatable(titleTranslationKey).withStyle(ChatFormatting.GOLD));

        if (sectionLines.isEmpty()) {
            lines.add(Component.translatable(emptyTranslationKey).withStyle(ChatFormatting.GRAY));
            return;
        }

        lines.addAll(sectionLines);
    }

    private List<Component> collectSpellGunAbilityTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        var overriddenCooldownTicks = getOverriddenCooldownTicks();
        if (overriddenCooldownTicks != null) {
            translatedLines.add(Component.translatable(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_reduce_recast",
                    formatTooltipSeconds(overriddenCooldownTicks)
            ).withStyle(ChatFormatting.GRAY));
        }

        var overriddenLongCastTicks = getOverriddenLongCastTicks();
        if (overriddenLongCastTicks != null) {
            var translationKey = overriddenLongCastTicks <= 0
                    ? "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
                    : "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_reduce_cast";
            var tooltip = overriddenLongCastTicks <= 0
                    ? Component.translatable(translationKey)
                    : Component.translatable(translationKey, formatTooltipSeconds(overriddenLongCastTicks));
            translatedLines.add(tooltip.withStyle(ChatFormatting.GRAY));
        }
        return translatedLines;
    }

    private List<Component> collectSpellGunRestrictTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        if (spellGunConfig.supportedCastTypes().size() == 1 && spellGunConfig.supports(SpellGunCastType.INSTANT)) {
            translatedLines.add(Component.translatable(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_instant_only"
            ).withStyle(ChatFormatting.GRAY));
        }
        if (spellGunConfig.supportedCastTypes().size() == 1 && spellGunConfig.supports(SpellGunCastType.LONG)) {
            translatedLines.add(Component.translatable(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_long_only"
            ).withStyle(ChatFormatting.GRAY));
        }

        var maxCooldownTicks = spellGunConfig.maxInstantImbueCooldownTicks();
        if (maxCooldownTicks != null) {
            translatedLines.add(Component.translatable(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_cooldown",
                    formatTooltipSeconds(maxCooldownTicks)
            ).withStyle(ChatFormatting.GRAY));
        }

        if (spellGunConfig.requireZeroInstantRecast()) {
            translatedLines.add(Component.translatable(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_no_recast"
            ).withStyle(ChatFormatting.GRAY));
        }

        return translatedLines;
    }

    private void appendSpellGunAmmoTooltipSection(
            ItemStack stack,
            List<Component> lines,
            String titleTranslationKey,
            String emptyTranslationKey
    ) {
        lines.add(Component.translatable(titleTranslationKey).withStyle(ChatFormatting.GOLD));

        var sectionLines = collectSpellGunAmmoTooltipSection(stack);
        if (sectionLines.isEmpty()) {
            lines.add(Component.translatable(emptyTranslationKey).withStyle(ChatFormatting.GRAY));
            return;
        }

        lines.addAll(sectionLines);
    }

    private List<Component> collectSpellGunAmmoTooltipSection(ItemStack stack) {
        var translatedLines = new ArrayList<Component>();
        for (var entry : getAmmoTooltipEntries(stack)) {
            var line = Component.literal("- ").append(entry.item().getDescription());
            if (entry.conditionTranslationKey() != null) {
                line = line.append(Component.literal(" ("))
                        .append(Component.translatable(entry.conditionTranslationKey()))
                        .append(Component.literal(")"));
            }
            translatedLines.add(line.withStyle(ChatFormatting.GRAY));
        }
        return translatedLines;
    }

    private static String resolveAttributeKey(AttributeBonus bonus, Attribute attribute, int index) {
        if (bonus.key() != null && !bonus.key().isBlank()) {
            return normalizeKeyToken(bonus.key());
        }

        var registryKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (registryKey != null) {
            return normalizeKeyToken(registryKey.toString());
        }

        // 登録キーが得られない属性でもUUIDが毎回安定するようフォールバックを固定化する.
        return "unknown_" + index;
    }

    private static String resolveAttributeToken(Attribute attribute) {
        var registryKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (registryKey == null) {
            return "unknown";
        }
        return normalizeKeyToken(registryKey.toString());
    }

    private static String normalizeKeyToken(String token) {
        return Objects.requireNonNull(token)
                .toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");
    }

    private static String formatTooltipSeconds(int ticks) {
        return BigDecimal.valueOf(ticks)
                .divide(BigDecimal.valueOf(20L))
                .stripTrailingZeros()
                .toPlainString();
    }

    // `bonus` ヘルパーは属性参照の受け取り方ごとにオーバーロードしている.
    // 将来の spell gun 実装で属性の持ち方が異なっても同じ書き味で定義できるようにしている.

    // Forge の RegistryObject や Deferred 登録由来の Supplier をそのまま渡す用途.
    protected static AttributeBonus bonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attributeSupplier, amount, operation, null);
    }

    // 既に Attribute 実体を持っているケース向け.
    protected static AttributeBonus bonus(
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(() -> attribute, amount, operation, null);
    }

    // バニラの Attributes.* のような Holder 経由の属性指定向け.
    protected static AttributeBonus bonus(
            Holder<Attribute> attributeHolder,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attributeHolder::value, amount, operation, null);
    }

    // 属性名の解決が不安定なケースで、UUID シード用キーを明示したい場合の Supplier 版.
    protected static AttributeBonus bonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attributeSupplier, amount, operation, key);
    }

    // 属性名の解決が不安定なケースで、UUID シード用キーを明示したい場合の Holder 版.
    protected static AttributeBonus bonus(
            Holder<Attribute> attributeHolder,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attributeHolder::value, amount, operation, key);
    }

    // 属性名の解決が不安定なケースで、UUID シード用キーを明示したい場合の Attribute 実体版.
    protected static AttributeBonus bonus(
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(() -> attribute, amount, operation, key);
    }

    private enum CastResult {
        NONE,
        SUCCESS,
        FAIL
    }

    // `key` は UUID 生成のシードに使う任意識別子.
    // null の場合は属性の登録キーを優先して使用する.
    private record MergeTarget(
            Attribute attribute,
            AttributeModifier.Operation operation
    ) {
    }

    protected record AttributeBonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        public AttributeBonus {
            Objects.requireNonNull(attributeSupplier);
            Objects.requireNonNull(operation);
        }
    }

    public record SpellGunConfig(
            Set<SpellGunCastType> supportedCastTypes,
            @Nullable Integer maxInstantImbueCooldownTicks,
            boolean requireZeroInstantRecast,
            @Nullable Integer overriddenSpellCooldownTicks,
            @Nullable Integer overriddenLongCastDurationTicks
    ) {
        public SpellGunConfig {
            supportedCastTypes = Set.copyOf(Objects.requireNonNull(supportedCastTypes));
        }

        public boolean supports(SpellGunCastType castType) {
            return supportedCastTypes.contains(castType);
        }
    }

    protected record AmmoTooltipEntry(Item item, @Nullable String conditionTranslationKey) {
        public AmmoTooltipEntry {
            Objects.requireNonNull(item);
        }
    }
}
