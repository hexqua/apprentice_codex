package jp.aquafactory.apprenticecodex.item.scrollcastergauntlet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.HandStackResolver;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.ArcaneAnvilScrollImbueBlockItem;
import jp.aquafactory.apprenticecodex.item.BetterCombatOffhandDualWieldingPolicyItem;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.ImmediateSneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.item.ItemTransformPreservingCastAnimationItem;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.OffhandUsePriorityHelper;
import jp.aquafactory.apprenticecodex.item.PriorityOffhandUseDeferringItem;
import jp.aquafactory.apprenticecodex.item.RightClickSpellItemHelper;
import jp.aquafactory.apprenticecodex.item.SchoolRuneSpellPowerTuning;
import jp.aquafactory.apprenticecodex.item.SneakSelectionView;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.item.StoredSpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.offhand.OffhandMagicModifierHelper;

public final class ScrollcasterGauntlet extends Item implements GeoItem, IPresetSpellContainer, UniqueItem,
        ItemTransformPreservingCastAnimationItem, ArcaneAnvilScrollImbueBlockItem,
        BetterCombatOffhandDualWieldingPolicyItem, SwingTriggeredMagicItem, PriorityOffhandUseDeferringItem, IJeiInfoItem,
        ImmediateSneakSelectionUiItem, StoredSpellCalibrationImbueTarget, SpellCalibrationAdjustmentTarget,
        NonDamageableAnvilMergeItem, TranscendencePolicy, AttributeEnchantmentPolicy, WisdomPolicy {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.scrollcaster_gauntlet.desc_";

    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    public static final int CALIBRATION_SCROLL_SLOT_COUNT = 10;
    public static final int BASE_CALIBRATION_SCROLL_SLOT_COUNT = 4;
    public static final int CALIBRATION_SCROLL_SLOTS_PER_UPGRADE = 2;
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            CalibrationAdjustmentProfile.of(
                    CalibrationAdjustmentRule.repeatable(
                            "slot_upgrade",
                            ScrollcasterGauntlet::isCalibrationSlotUpgrade,
                            CalibrationAdjustmentHints.slotUpgrades()
                    ).withEffectLines(CalibrationAdjustmentEffects.addScrollSlot(
                            CALIBRATION_SCROLL_SLOTS_PER_UPGRADE
                    )),
                    CalibrationAdjustmentRule.unique(
                            "mithril_freecast_staff",
                            ScrollcasterGauntlet::isFreecastStaffAdjustment,
                            CalibrationAdjustmentHints.mithrilFreecastStaff()
                    ).withEffectLines(CalibrationAdjustmentEffects.addSwingcastFunction()),
                    CalibrationAdjustmentRule.unique(
                            "school_rune",
                            ScrollcasterSchoolRuneResolver::isSchoolRune,
                            CalibrationAdjustmentHints.schoolRunes(),
                            CalibrationAdjustmentHints.schoolRuneConstraint()
                    ).withEffectLines(CalibrationAdjustmentEffects.changeSpellPower(
                            SchoolRuneSpellPowerTuning.TUNED_SCHOOL_SPELL_POWER_BONUS,
                            SchoolRuneSpellPowerTuning.GENERAL_SPELL_POWER_REDUCTION
                    ))
            );

    private static final HolderLookup.Provider FALLBACK_SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @Override
    public @NotNull SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull SpellData spellData
    ) {
        return evaluateCalibrationImbue(targetStack, slot, spellData, serializationLookup());
    }

    @Override
    public @NotNull SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull SpellData spellData,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (slot < 0 || slot >= getEnabledCalibrationScrollSlotCount(targetStack, lookupProvider)
                || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return SpellCalibrationImbueState.REJECTED;
        }
        return SpellCalibrationImbueState.ACCEPTED_USABLE;
    }

    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_hunter_weapon")
    );
    private static final String MAIN_CONTROLLER = "main";
    private static final String CALIBRATION_TAG = "SpellCalibration";
    private static final String SCROLLS_TAG = "Scrolls";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String SCHOOL_POWER_SCHOOL_TAG = "SchoolPowerSchool";
    private static final String SELECTED_SCROLL_INDEX_TAG = "SelectedScrollIndex";
    private static final String CAST_ANIMATION = "cast";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_CAST = RawAnimation.begin().thenPlay("cast");
    private static final double ATTACK_DAMAGE_BONUS = 5.0D;
    private static final double ATTACK_SPEED_BONUS = -2.2D;
    private static final double EPIC_FIGHT_ATTACK_DAMAGE_BONUS = 2.0D;
    private static final double EPIC_FIGHT_ATTACK_SPEED_BONUS = 0.0D;
    private static final ResourceLocation SPELL_POWER_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "scrollcaster_gauntlet.mainhand_spell_power");
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_SWORD);
    private static final ItemStack PICKAXE_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_PICKAXE);
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ScrollcasterGauntlet() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        // Iron's の upgrade 処理は同 Attribute/Operation の既存補正 1 本を置換するため、表示前に自前補正を合算しておく。
        var modifiers = OffhandMagicModifierHelper.buildEquippedModifiers(
                buildMainhandModifiers(stack),
                stack,
                "scrollcaster_gauntlet"
        );
        return buildMergedMainhandAttributeModifiers(modifiers);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 22;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment) || isSupportedEnchantment(stack, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return enchantments.isEmpty()
                || enchantments.keySet().stream().allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return AttributeEnchantmentPolicy.ALL_ATTRIBUTE_ENCHANTMENTS;
    }

    @Override
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand == InteractionHand.MAIN_HAND && shouldPrioritizeOffhandUse(player)) {
            return InteractionResultHolder.pass(stack);
        }
        if (usedHand == InteractionHand.OFF_HAND && shouldDeferToMainhandSpellUse(player)) {
            return InteractionResultHolder.pass(stack);
        }

        var spellData = getSelectedSpellData(stack, level.registryAccess());
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null || spellData.getSpell() == SpellRegistry.none()) {
            return InteractionResultHolder.pass(stack);
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var casted = spell.attemptInitiateCast(
                stack,
                spellLevel,
                level,
                player,
                CastSource.SWORD,
                true,
                usedHand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND
        );

        if (casted && player instanceof ServerPlayer serverPlayer) {
            triggerCastAnimation(serverPlayer, stack);
        }

        return casted
                ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        refreshSelectedSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        var lookupProvider = level.registryAccess();
        if (hasAnyCalibrationScroll(stack, lookupProvider)
                && !isCurrentSelectedSpellContainer(stack, lookupProvider)) {
            refreshSelectedSpellContainer(stack, lookupProvider);
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return createCalibrationAdjustmentTooltip(stack);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @NotNull Item.TooltipContext context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        lines.add(Component.translatable(
                "item.apprenticecodex.right_click_magic_weapon.desc",
                ImbueTooltipHelper.getUseKeyName()
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.apprenticecodex.right_click_magic_weapon.item_type")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                "item.apprenticecodex.scrollcaster_gauntlet.desc",
                ImbueTooltipHelper.getUseKeyName()
        ).withStyle(ChatFormatting.GRAY));
        var resolvedSchool = getResolvedCalibrationSchool(stack);
        if (resolvedSchool != null) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.scrollcaster_gauntlet.school_rune",
                    resolvedSchool.getDisplayName()
            ).withStyle(ChatFormatting.GRAY));
        }
        var lookupProvider = context.registries();
        if (lookupProvider != null && hasFreecastStaffAdjustment(stack, lookupProvider)) {
            lines.add(Component.translatable("item.apprenticecodex.freecast.common.desc")
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, lines, flag);
    }

    @Override
    public boolean canTriggerSpellOnSwing(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        return stack.getItem() == this
                && hasFreecastStaffAdjustment(stack, player.level().registryAccess());
    }

    @Override
    public boolean tryTriggerSpellOnSwing(Player player, InteractionHand hand, boolean bypassChargeCheck) {
        if (player.level().isClientSide) {
            return false;
        }

        var stack = player.getItemInHand(hand);
        if (!canTriggerSpellOnSwing(player, hand)
                || (!bypassChargeCheck && !AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player))) {
            return false;
        }

        var lookupProvider = player.level().registryAccess();
        refreshSelectedSpellContainer(stack, lookupProvider);
        var spellData = getSelectedSpellData(stack, lookupProvider);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return false;
        }

        var spell = spellData.getSpell();
        if (!MithrilFreecastStaff.canSwingCastSpell(spell, true)) {
            player.displayClientMessage(
                    Component.translatable(
                            "ui.apprenticecodex.swingcast.cannot_swing_cast",
                            spell.getDisplayName(player)
                    ).withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            return false;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var slotId = resolveSpellSelectionSlot(hand);
        try (var swingContext = SwingcastStaffCastContext.open(player.getUUID(), stack, spell);
             var freecastContext = ScrollcasterGauntletFreecastContext.open(player.getUUID(), stack, spell)) {
            var casted = spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    CastSource.SWORD,
                    true,
                    slotId
            );
            if (!casted) {
                return false;
            }

            TriggeredSpellCastHelper.applyLongCastDurationOverride(
                    player,
                    spellLevel,
                    spell,
                    magicData,
                    slotId,
                    spell.getCastType() == CastType.LONG ? 0 : null
            );
            if (player instanceof ServerPlayer serverPlayer) {
                triggerCastAnimation(serverPlayer, stack);
            }
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Scrollcaster Gauntlet freecast swing context failed to close.", exception);
        }
    }

    public int resolveFreecastSwingCooldownTicks(Player player, ItemStack stack, AbstractSpell spell, int currentEffectiveCooldown) {
        var spellLevel = resolveEffectiveSpellLevel(player, stack, spell);
        return currentEffectiveCooldown
                + (spell.getCastType() == CastType.LONG ? spell.getEffectiveCastTime(spellLevel, player) : 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
            state.setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }).triggerableAnim(CAST_ANIMATION, ANIM_CAST));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static boolean shouldPrioritizeOffhandUse(Player player) {
        return OffhandUsePriorityHelper.isPriorityOffhandUseItem(player.getOffhandItem());
    }

    private static boolean shouldDeferToMainhandSpellUse(Player player) {
        var mainHandStack = player.getMainHandItem();
        var mainHandItem = mainHandStack.getItem();
        return RightClickSpellItemHelper.isRightClickSpellItem(mainHandStack)
                || mainHandItem instanceof AbstractRightClickMagicWeaponItem
                || mainHandItem instanceof ScrollcasterGauntlet;
    }

    private void triggerCastAnimation(ServerPlayer player, ItemStack stack) {
        var instanceId = GeoItem.getOrAssignId(stack, player.serverLevel());
        triggerAnim(player, instanceId, MAIN_CONTROLLER, CAST_ANIMATION);
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> buildMainhandModifiers(ItemStack stack) {
        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        var attackDamageBonus = getAttackDamageBonus();
        var attackSpeedBonus = getAttackSpeedBonus();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID,
                        attackDamageBonus,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID,
                        attackSpeedBonus,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );
        var schoolPowerAttribute = getResolvedSchoolPowerAttribute(stack);
        if (schoolPowerAttribute != null) {
            builder.put(
                    AttributeRegistry.SPELL_POWER,
                    new AttributeModifier(
                            SPELL_POWER_MODIFIER_ID,
                            SchoolRuneSpellPowerTuning.TUNED_GENERAL_SPELL_POWER_BONUS,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );
            builder.put(
                    schoolPowerAttribute,
                    new AttributeModifier(
                            SPELL_POWER_MODIFIER_ID,
                            SchoolRuneSpellPowerTuning.TUNED_SCHOOL_SPELL_POWER_BONUS,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );
        } else if (shouldApplyBaseSpellPowerBonus(stack)) {
            builder.put(
                    AttributeRegistry.SPELL_POWER,
                    new AttributeModifier(
                            SPELL_POWER_MODIFIER_ID,
                            SchoolRuneSpellPowerTuning.BASE_GENERAL_SPELL_POWER_BONUS,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );
        }
        return builder.build();
    }

    private static double getAttackDamageBonus() {
        return isEpicFightLoaded() ? EPIC_FIGHT_ATTACK_DAMAGE_BONUS : ATTACK_DAMAGE_BONUS;
    }

    private static double getAttackSpeedBonus() {
        return isEpicFightLoaded() ? EPIC_FIGHT_ATTACK_SPEED_BONUS : ATTACK_SPEED_BONUS;
    }

    private static boolean isEpicFightLoaded() {
        // Epic Fight の fist モーションは攻撃速度 4 前提のため、導入時だけ表示値ごとグローブ相当に寄せる。
        return ModList.get().isLoaded(EpicFightCompat.MOD_ID);
    }

    private static ItemAttributeModifiers buildMergedMainhandAttributeModifiers(
            Multimap<Holder<Attribute>, AttributeModifier> modifiers
    ) {
        var grouped = new LinkedHashMap<MergeTarget, List<AttributeModifier>>();
        for (var entry : modifiers.entries()) {
            grouped.computeIfAbsent(
                    new MergeTarget(entry.getKey(), entry.getValue().operation()),
                    ignored -> new ArrayList<>()
            ).add(entry.getValue());
        }

        var builder = ItemAttributeModifiers.builder();
        var mergedIndex = 0;
        for (var entry : grouped.entrySet()) {
            var target = entry.getKey();
            var targetModifiers = entry.getValue();
            if (targetModifiers.size() == 1) {
                builder.add(target.attribute(), targetModifiers.getFirst(), EquipmentSlotGroup.MAINHAND);
                continue;
            }

            var amount = targetModifiers.stream().mapToDouble(AttributeModifier::amount).sum();
            if (amount == 0.0D) {
                continue;
            }
            builder.add(
                    target.attribute(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(
                                    "apprenticecodex",
                                    "scrollcaster_gauntlet/mainhand_merged_" + mergedIndex++
                            ),
                            amount,
                            target.operation()
                    ),
                    EquipmentSlotGroup.MAINHAND
            );
        }
        return builder.build();
    }

    private static boolean shouldApplyBaseSpellPowerBonus(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !hasResolvedCalibrationSchool(stack);
    }

    private static boolean isSupportedEnchantment(ItemStack gauntletStack, Holder<Enchantment> enchantment) {
        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        return isExplicitlySupportedMagicEnchantment(enchantment)
                || MalumCompatibility.isReplenishingEnchantment(enchantmentId)
                || MalumCompatibility.isSpiritPlunderSupported(gauntletStack, enchantmentId)
                || MalumCompatibility.isMagicCapableWeaponEnchantment(gauntletStack, enchantmentId)
                || ((SWORD_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment)
                        || PICKAXE_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment))
                && !DURABILITY_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment));
    }

    private static boolean isExplicitlySupportedMagicEnchantment(Holder<Enchantment> enchantment) {
        return matches(enchantment, Enchantments.ALACRITY)
                || matches(enchantment, Enchantments.REFLUX)
                || matches(enchantment, Enchantments.RESERVOIR)
                || matches(enchantment, Enchantments.TENSE)
                || matches(enchantment, Enchantments.SURGE)
                || matches(enchantment, Enchantments.ATTUNEMENT)
                || matches(enchantment, Enchantments.TRANSCENDENCE)
                || matches(enchantment, Enchantments.WISDOM);
    }

    private static boolean matches(Holder<Enchantment> enchantment, ResourceKey<Enchantment> key) {
        return enchantment.is(key);
    }

    private static @Nullable Holder<Attribute> getResolvedSchoolPowerAttribute(ItemStack stack) {
        var attribute = MagicTools.resolveSchoolPowerAttribute(getResolvedCalibrationSchool(stack));
        return attribute == null ? null : BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }

    public static @Nullable SchoolType getResolvedCalibrationSchool(ItemStack stack) {
        var schoolId = getResolvedCalibrationSchoolId(stack);
        if (schoolId == null) {
            return null;
        }

        return SchoolRegistry.getSchool(schoolId);
    }

    public static @NotNull ItemStack getInventoryOverlayIconStack(ItemStack stack) {
        var school = getResolvedCalibrationSchool(stack);
        return school == null ? ItemStack.EMPTY : SchoolAffinityRegistry.createIconStack(school);
    }

    private static @NotNull ItemStack readCalibrationAdjustment(@NotNull ItemStack gauntletStack, int slot) {
        return CalibrationAdjustmentStorage.get(gauntletStack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    private static @NotNull ItemStack readCalibrationAdjustment(
            @NotNull ItemStack gauntletStack,
            int slot,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        return CalibrationAdjustmentStorage.get(
                gauntletStack,
                slot,
                CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                lookupProvider
        );
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
        refreshResolvedCalibrationSchool(targetStack, lookupProvider);
        refreshSelectedSpellContainer(targetStack, lookupProvider);
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_PROFILE;
    }

    public static @NotNull ItemStack getCalibrationScroll(@NotNull ItemStack gauntletStack, int slot) {
        return getCalibrationScroll(gauntletStack, slot, serializationLookup());
    }

    public static @NotNull ItemStack getCalibrationScroll(
            @NotNull ItemStack gauntletStack,
            int slot,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        return getCalibrationItem(gauntletStack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT, lookupProvider);
    }

    public static void setCalibrationScroll(@NotNull ItemStack gauntletStack, int slot, @NotNull ItemStack stack) {
        setCalibrationScroll(gauntletStack, slot, stack, serializationLookup());
    }

    public static void setCalibrationScroll(
            @NotNull ItemStack gauntletStack,
            int slot,
            @NotNull ItemStack stack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        setCalibrationItem(gauntletStack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT, stack, lookupProvider);
        refreshSelectedSpellContainer(gauntletStack, lookupProvider);
    }

    public static boolean hasAnyCalibrationScroll(@NotNull ItemStack gauntletStack) {
        return hasAnyCalibrationScroll(gauntletStack, serializationLookup());
    }

    public static boolean hasAnyCalibrationScroll(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        return findFirstValidScrollIndex(gauntletStack, lookupProvider) >= 0;
    }

    @Override
    public boolean hasAnyStoredCalibrationScroll(@NotNull ItemStack targetStack) {
        for (var slot = 0; slot < CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!getCalibrationScroll(targetStack, slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFreecastStaffAdjustment(@NotNull ItemStack gauntletStack) {
        return hasFreecastStaffAdjustment(gauntletStack, serializationLookup());
    }

    public static boolean hasFreecastStaffAdjustment(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return false;
        }

        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isFreecastStaffAdjustment(readCalibrationAdjustment(gauntletStack, slot, lookupProvider))) {
                return true;
            }
        }
        return false;
    }

    public static int getEnabledCalibrationScrollSlotCount(@NotNull ItemStack gauntletStack) {
        return getEnabledCalibrationScrollSlotCount(gauntletStack, serializationLookup());
    }

    public static int getEnabledCalibrationScrollSlotCount(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return 0;
        }

        var upgradeCount = 0;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isCalibrationSlotUpgrade(readCalibrationAdjustment(gauntletStack, slot, lookupProvider))) {
                ++upgradeCount;
            }
        }

        return Math.min(
                CALIBRATION_SCROLL_SLOT_COUNT,
                BASE_CALIBRATION_SCROLL_SLOT_COUNT + upgradeCount * CALIBRATION_SCROLL_SLOTS_PER_UPGRADE
        );
    }

    public static int getSelectedScrollIndex(@NotNull ItemStack gauntletStack) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return -1;
        }

        var calibrationTag = getCalibrationTag(gauntletStack);
        if (calibrationTag == null || !calibrationTag.contains(SELECTED_SCROLL_INDEX_TAG, Tag.TAG_INT)) {
            return -1;
        }

        var index = calibrationTag.getInt(SELECTED_SCROLL_INDEX_TAG);
        return index >= 0 && index < CALIBRATION_SCROLL_SLOT_COUNT ? index : -1;
    }

    public static void setSelectedScrollIndex(@NotNull ItemStack gauntletStack, int selectedScrollIndex) {
        setSelectedScrollIndex(gauntletStack, selectedScrollIndex, serializationLookup());
    }

    public static void setSelectedScrollIndex(
            @NotNull ItemStack gauntletStack,
            int selectedScrollIndex,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return;
        }

        if (!isSelectableScrollIndex(gauntletStack, selectedScrollIndex, lookupProvider)) {
            refreshSelectedSpellContainer(gauntletStack, lookupProvider);
            return;
        }

        setStoredSelectedScrollIndex(gauntletStack, selectedScrollIndex);
        refreshSelectedSpellContainer(gauntletStack, lookupProvider);
    }

    public static boolean isSelectableScrollIndex(@NotNull ItemStack gauntletStack, int selectedScrollIndex) {
        return isSelectableScrollIndex(gauntletStack, selectedScrollIndex, serializationLookup());
    }

    public static boolean isSelectableScrollIndex(
            @NotNull ItemStack gauntletStack,
            int selectedScrollIndex,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        return isValidCalibrationAccess(gauntletStack, 0, 1)
                && selectedScrollIndex >= 0
                && selectedScrollIndex < getEnabledCalibrationScrollSlotCount(gauntletStack, lookupProvider)
                && isValidScrollSpell(getCalibrationScroll(gauntletStack, selectedScrollIndex, lookupProvider));
    }

    public static int normalizeSelectedScrollIndex(@NotNull ItemStack gauntletStack) {
        return normalizeSelectedScrollIndex(gauntletStack, serializationLookup());
    }

    public static int normalizeSelectedScrollIndex(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return -1;
        }

        var selectedIndex = getSelectedScrollIndex(gauntletStack);
        if (isSelectableScrollIndex(gauntletStack, selectedIndex, lookupProvider)) {
            return selectedIndex;
        }

        var firstValidIndex = findFirstValidScrollIndex(gauntletStack, lookupProvider);
        if (firstValidIndex < 0) {
            clearSelectedScrollIndex(gauntletStack);
            ISpellContainer.remove(gauntletStack);
            return -1;
        }

        setStoredSelectedScrollIndex(gauntletStack, firstValidIndex);
        return firstValidIndex;
    }

    public static @NotNull SpellData getSelectedSpellData(@NotNull ItemStack gauntletStack) {
        return getSelectedSpellData(gauntletStack, serializationLookup());
    }

    public static @NotNull SpellData getSelectedSpellData(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        var selectedIndex = normalizeSelectedScrollIndex(gauntletStack, lookupProvider);
        if (selectedIndex < 0) {
            return SpellData.EMPTY;
        }

        return getScrollSpellData(getCalibrationScroll(gauntletStack, selectedIndex, lookupProvider));
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
        // Better Combatが論理オフハンドを空にする場合でも、Gauntletの選択UIだけは実スロットを参照する。
        return HandStackResolver.OffhandResolution.PHYSICAL;
    }

    public static @NotNull List<SneakSelectionView> getSelectionViews(@NotNull ItemStack gauntletStack) {
        return getSelectionViews(gauntletStack, serializationLookup());
    }

    public static @NotNull List<SneakSelectionView> getSelectionViews(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return List.of();
        }

        normalizeSelectedScrollIndex(gauntletStack, lookupProvider);
        var enabledSlotCount = getEnabledCalibrationScrollSlotCount(gauntletStack, lookupProvider);
        var views = new ArrayList<SneakSelectionView>();
        for (var slot = 0; slot < enabledSlotCount; ++slot) {
            var spellData = getScrollSpellData(getCalibrationScroll(gauntletStack, slot, lookupProvider));
            views.add(SneakSelectionView.forSpell(
                    slot,
                    spellData,
                    isSelectableScrollIndex(gauntletStack, slot, lookupProvider)
            ));
        }
        return List.copyOf(views);
    }

    public static void refreshSelectedSpellContainer(@NotNull ItemStack gauntletStack) {
        refreshSelectedSpellContainer(gauntletStack, serializationLookup());
    }

    public static void refreshSelectedSpellContainer(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        var spellData = getSelectedSpellData(gauntletStack, lookupProvider);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            ISpellContainer.remove(gauntletStack);
            return;
        }

        if (isCurrentSelectedSpellContainer(gauntletStack, spellData)) {
            return;
        }

        // Iron's のショートカット選択とクールダウン表示に乗せるため、内部スクロールをGauntlet本体のSpell Wheelへ投影する。
        var spellContainer = ISpellContainer.create(1, true, false).mutableCopy();
        spellContainer.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
        ISpellContainer.set(gauntletStack, spellContainer.toImmutable());
    }

    private static boolean isCurrentSelectedSpellContainer(@NotNull ItemStack gauntletStack) {
        return isCurrentSelectedSpellContainer(gauntletStack, serializationLookup());
    }

    private static boolean isCurrentSelectedSpellContainer(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        var spellData = getSelectedSpellData(gauntletStack, lookupProvider);
        return spellData != SpellData.EMPTY
                && spellData.getSpell() != null
                && isCurrentSelectedSpellContainer(gauntletStack, spellData);
    }

    private static boolean isCurrentSelectedSpellContainer(@NotNull ItemStack gauntletStack, @NotNull SpellData spellData) {
        var current = ISpellContainer.get(gauntletStack);
        if (current != null) {
            var currentSpell = current.getSpellAtIndex(0);
            return current.getMaxSpellCount() == 1
                    && current.isSpellWheel()
                    && !current.mustEquip()
                    && currentSpell != SpellData.EMPTY
                    && currentSpell.getSpell() == spellData.getSpell()
                    && currentSpell.getLevel() == spellData.getLevel()
                    && !currentSpell.isLocked();
        }
        return false;
    }

    public static void refreshResolvedCalibrationSchool(@NotNull ItemStack gauntletStack) {
        refreshResolvedCalibrationSchool(gauntletStack, serializationLookup());
    }

    public static void refreshResolvedCalibrationSchool(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return;
        }

        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(
                    readCalibrationAdjustment(gauntletStack, slot, lookupProvider)
            );
            if (school.isPresent()) {
                setResolvedCalibrationSchoolId(gauntletStack, school.get().getId());
                return;
            }
        }

        clearResolvedCalibrationSchool(gauntletStack);
    }

    private static @NotNull ItemStack getCalibrationItem(@NotNull ItemStack gauntletStack, String listName,
                                                         int slot, int slotCount,
                                                         @NotNull HolderLookup.Provider lookupProvider) {
        if (!isValidCalibrationAccess(gauntletStack, slot, slotCount)) {
            return ItemStack.EMPTY;
        }

        var calibrationTag = getCalibrationTag(gauntletStack);
        if (calibrationTag == null || !calibrationTag.contains(listName, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }

        var list = calibrationTag.getList(listName, Tag.TAG_COMPOUND);
        for (var index = 0; index < list.size(); ++index) {
            var entry = list.getCompound(index);
            if (entry.getInt(SLOT_TAG) != slot || !entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            return ItemStack.parseOptional(lookupProvider, entry.getCompound(ITEM_TAG));
        }
        return ItemStack.EMPTY;
    }

    private static void setCalibrationItem(@NotNull ItemStack gauntletStack, String listName, int slot, int slotCount,
                                           @NotNull ItemStack stack,
                                           @NotNull HolderLookup.Provider lookupProvider) {
        if (!isValidCalibrationAccess(gauntletStack, slot, slotCount)) {
            return;
        }

        updateCalibrationTag(gauntletStack, calibrationTag -> {
            var list = calibrationTag.contains(listName, Tag.TAG_LIST)
                    ? calibrationTag.getList(listName, Tag.TAG_COMPOUND)
                    : new ListTag();
            removeCalibrationItem(list, slot);

            if (!stack.isEmpty()) {
                var storedStack = stack.copy();
                storedStack.setCount(1);
                var entry = new CompoundTag();
                entry.putInt(SLOT_TAG, slot);
                entry.put(ITEM_TAG, storedStack.saveOptional(lookupProvider));
                list.add(entry);
            }

            if (list.isEmpty()) {
                calibrationTag.remove(listName);
            } else {
                calibrationTag.put(listName, list);
            }
        });
    }

    private static int findFirstValidScrollIndex(@NotNull ItemStack gauntletStack) {
        return findFirstValidScrollIndex(gauntletStack, serializationLookup());
    }

    private static int findFirstValidScrollIndex(
            @NotNull ItemStack gauntletStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        var enabledSlotCount = getEnabledCalibrationScrollSlotCount(gauntletStack, lookupProvider);
        for (var slot = 0; slot < enabledSlotCount; ++slot) {
            if (isValidScrollSpell(getCalibrationScroll(gauntletStack, slot, lookupProvider))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isValidScrollSpell(@NotNull ItemStack scrollStack) {
        return getScrollSpellData(scrollStack) != SpellData.EMPTY;
    }

    public static boolean isCalibrationSlotUpgrade(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES);
    }

    public static boolean isFreecastStaffAdjustment(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MithrilFreecastStaff;
    }

    private static int resolveEffectiveSpellLevel(Player player, ItemStack stack, AbstractSpell spell) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null
                && Objects.equals(spell.getSpellId(), magicData.getCastingSpellId())
                && magicData.getCastingSpellLevel() > 0) {
            return magicData.getCastingSpellLevel();
        }

        var spellData = getSelectedSpellData(stack, player.level().registryAccess());
        if (spellData != SpellData.EMPTY && spell.equals(spellData.getSpell())) {
            return spell.getLevelFor(spellData.getLevel(), player);
        }

        return spell.getLevelFor(1, player);
    }

    private static String resolveSpellSelectionSlot(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
    }

    private static @NotNull SpellData getScrollSpellData(@NotNull ItemStack scrollStack) {
        if (scrollStack.isEmpty() || !(scrollStack.getItem() instanceof Scroll)) {
            return SpellData.EMPTY;
        }

        var scrollContainer = ISpellContainer.get(scrollStack);
        if (scrollContainer == null) {
            return SpellData.EMPTY;
        }

        var spellData = scrollContainer.getSpellAtIndex(0);
        // 直近調査した不具合で@NotNullからnullが返ってきてクラッシュしている不具合を見た記憶があるので冗長だがnullチェックを行っておく.
        //noinspection ConstantValue
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private static void setStoredSelectedScrollIndex(@NotNull ItemStack gauntletStack, int selectedScrollIndex) {
        if (selectedScrollIndex < 0 || selectedScrollIndex >= CALIBRATION_SCROLL_SLOT_COUNT) {
            clearSelectedScrollIndex(gauntletStack);
            return;
        }

        updateCalibrationTag(gauntletStack, tag -> tag.putInt(SELECTED_SCROLL_INDEX_TAG, selectedScrollIndex));
    }

    private static void clearSelectedScrollIndex(@NotNull ItemStack gauntletStack) {
        updateCalibrationTag(gauntletStack, tag -> tag.remove(SELECTED_SCROLL_INDEX_TAG));
    }

    private static boolean hasResolvedCalibrationSchool(ItemStack stack) {
        return getResolvedCalibrationSchoolId(stack) != null;
    }

    private static ResourceLocation getResolvedCalibrationSchoolId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        var calibrationTag = getCalibrationTag(stack);
        if (calibrationTag == null || !calibrationTag.contains(SCHOOL_POWER_SCHOOL_TAG, Tag.TAG_STRING)) {
            return null;
        }

        return ResourceLocation.tryParse(calibrationTag.getString(SCHOOL_POWER_SCHOOL_TAG));
    }

    private static void setResolvedCalibrationSchoolId(ItemStack stack, ResourceLocation schoolId) {
        updateCalibrationTag(stack, tag -> tag.putString(SCHOOL_POWER_SCHOOL_TAG, schoolId.toString()));
    }

    private static void clearResolvedCalibrationSchool(ItemStack stack) {
        updateCalibrationTag(stack, tag -> tag.remove(SCHOOL_POWER_SCHOOL_TAG));
    }

    private static @Nullable CompoundTag getCalibrationTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        var rootTag = customData.copyTag();
        if (!rootTag.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        return rootTag.getCompound(CALIBRATION_TAG);
    }

    private static void updateCalibrationTag(ItemStack stack, Consumer<CompoundTag> updater) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, rootTag -> {
            var calibrationTag = rootTag.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)
                    ? rootTag.getCompound(CALIBRATION_TAG)
                    : new CompoundTag();
            updater.accept(calibrationTag);
            if (calibrationTag.isEmpty()) {
                rootTag.remove(CALIBRATION_TAG);
            } else {
                rootTag.put(CALIBRATION_TAG, calibrationTag);
            }
        });
    }

    private static HolderLookup.Provider serializationLookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? FALLBACK_SERIALIZATION_LOOKUP : server.registryAccess();
    }

    private static void removeCalibrationItem(ListTag list, int slot) {
        for (var index = list.size() - 1; index >= 0; --index) {
            if (list.getCompound(index).getInt(SLOT_TAG) == slot) {
                list.remove(index);
            }
        }
    }

    private static boolean isValidCalibrationAccess(@NotNull ItemStack gauntletStack, int slot, int slotCount) {
        return !gauntletStack.isEmpty()
                && gauntletStack.getItem() instanceof ScrollcasterGauntlet
                && slot >= 0
                && slot < slotCount;
    }

    private record MergeTarget(
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation
    ) {
    }
}
