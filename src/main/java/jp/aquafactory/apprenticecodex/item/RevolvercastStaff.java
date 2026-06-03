package jp.aquafactory.apprenticecodex.item;

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
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.RevolvercastStaffRenderer;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class RevolvercastStaff extends AbstractRightClickMagicWeaponItem
        implements GeoItem, CastAnimationOverrideItem, IJeiInfoItem, SwingTriggeredMagicItem,
        RestrictedSpellImbuableItem, ArcaneAnvilImbueBlockItem {
    private static final String ITEM_KEY = "revolvercast_staff";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.revolvercast_staff.desc_";
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    public static final int CALIBRATION_SCROLL_SLOT_COUNT = 10;
    public static final int BASE_CALIBRATION_SCROLL_SLOT_COUNT = 4;
    public static final int CALIBRATION_SCROLL_SLOTS_PER_UPGRADE = 2;

    private static final String ORB_CONTROLLER = "orb";
    private static final String REVOLVE_CONTROLLER = "revolve";
    private static final String REVOLVE_ANIMATION = "revolve";
    private static final String REVOLVE_ANIMATION_ALT = "revolve2";
    private static final String CALIBRATION_TAG = "SpellCalibration";
    private static final String ADJUSTMENTS_TAG = "Adjustments";
    private static final String SCROLLS_TAG = "Scrolls";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String SCHOOL_POWER_SCHOOL_TAG = "SchoolPowerSchool";
    private static final String SELECTED_SCROLL_INDEX_TAG = "SelectedScrollIndex";
    private static final String REVOLVE_ANIMATION_VARIANT_TAG = "RevolveAnimationVariant";
    private static final RawAnimation ANIM_ORB_LOOP = RawAnimation.begin().thenLoop("orb_loop");
    private static final RawAnimation ANIM_REVOLVE = RawAnimation.begin().thenPlay(REVOLVE_ANIMATION);
    private static final RawAnimation ANIM_REVOLVE_ALT = RawAnimation.begin().thenPlay(REVOLVE_ANIMATION_ALT);
    private static final int ENCHANTMENT_VALUE = 15;
    private static final double DISPLAYED_ATTACK_DAMAGE = 8.0D;
    private static final double DISPLAYED_ATTACK_SPEED = 1.8D;
    private static final double GENERAL_SPELL_POWER_BONUS = 0.10D;
    private static final double SCHOOL_SPELL_POWER_BONUS = 0.15D;
    private static final UUID SPELL_POWER_MODIFIER_ID = UUID.fromString("c746a21b-7055-4fdf-8d47-c31d41041a46");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/geo/" + ITEM_KEY + ".png"
    );

    public RevolvercastStaff() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                true,
                ENCHANTMENT_VALUE,
                ITEM_KEY,
                DISPLAYED_ATTACK_DAMAGE - 1.0D,
                DISPLAYED_ATTACK_SPEED - 4.0D
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity,
                              int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (hasAnyCalibrationScroll(stack) || ISpellContainer.isSpellContainer(stack)) {
            refreshSelectedSpellContainer(stack);
        }
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        refreshSelectedSpellContainer(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        DISPLAYED_ATTACK_DAMAGE - 1.0D,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_UUID,
                        "Weapon modifier",
                        DISPLAYED_ATTACK_SPEED - 4.0D,
                        AttributeModifier.Operation.ADDITION
                )
        );

        var spellPowerAmount = SCHOOL_SPELL_POWER_BONUS;
        var spellPowerAttribute = getResolvedSchoolPowerAttribute(stack);
        if (spellPowerAttribute == null) {
            spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            spellPowerAmount = GENERAL_SPELL_POWER_BONUS;
        }
        builder.put(
                spellPowerAttribute,
                new AttributeModifier(
                        SPELL_POWER_MODIFIER_ID,
                        "apprenticecodex.revolvercast_staff.mainhand.spell_power",
                        spellPowerAmount,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
        return builder.build();
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (EnchantmentRegistry.TRANSCENDENCE.isPresent() && enchantment == EnchantmentRegistry.TRANSCENDENCE.get()) {
            return false;
        }

        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return canSwingCastSpell(spell);
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        refreshSelectedSpellContainer(stack);
    }

    @Override
    public boolean tryTriggerSpellOnSwing(Player player, InteractionHand hand, boolean bypassChargeCheck) {
        if (player.level().isClientSide) {
            return false;
        }

        var stack = player.getItemInHand(hand);
        if (!isSameItem(stack) || (!bypassChargeCheck && !isFullyChargedAttack(player))) {
            return false;
        }

        refreshSelectedSpellContainer(stack);
        var spellData = getSelectedSpellData(stack);
        if (spellData == SpellData.EMPTY || !canImbueSpell(spellData)) {
            return false;
        }

        var spell = spellData.getSpell();
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            if (advanceAfterFailedCastIfNeeded(stack)) {
                triggerRevolveAnimationIfPossible(player, stack);
            }
            return false;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        try (var ignored = SwingcastStaffCastContext.open(player.getUUID(), stack, spell)) {
            var casted = spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    CastSource.SWORD,
                    true,
                    resolveSpellSelectionSlot(hand)
            );
            if (!casted) {
                if (advanceAfterFailedCastIfNeeded(stack)) {
                    triggerRevolveAnimationIfPossible(player, stack);
                }
                return false;
            }

            TriggeredSpellCastHelper.applyLongCastDurationOverride(
                    player,
                    spellLevel,
                    spell,
                    magicData,
                    resolveSpellSelectionSlot(hand),
                    spell.getCastType() == CastType.LONG ? 0 : null
            );
            if (advanceToNextValidScrollIndex(stack)) {
                triggerRevolveAnimationIfPossible(player, stack);
            }
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Revolvercast Staff swing cast context failed to close.", exception);
        }
    }

    public int resolveSwingcastCooldownTicks(Player player, ItemStack stack, AbstractSpell spell, int currentEffectiveCooldown) {
        var spellLevel = resolveEffectiveSpellLevel(player, stack, spell);
        return currentEffectiveCooldown
                + (spell.getCastType() == CastType.LONG ? spell.getEffectiveCastTime(spellLevel, player) : 0);
    }

    @Override
    public boolean shouldSuppressCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return AnimationHolder.pass();
    }

    @Override
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public List<Component> getImbueRestrictionTooltipLines() {
        return collectRestrictTooltipSection();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("item.apprenticecodex.swingcast.common.desc").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.apprenticecodex.revolvercast_staff.desc").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(hasRecoveryRune(stack)
                ? "item.apprenticecodex.revolvercast_staff.desc.skip_mode"
                : "item.apprenticecodex.revolvercast_staff.desc.normal_mode"
        ).withStyle(ChatFormatting.GRAY));
        var resolvedSchool = getResolvedCalibrationSchool(stack);
        if (resolvedSchool != null) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.revolvercast_staff.school_rune",
                    resolvedSchool.getDisplayName()
            ).withStyle(ChatFormatting.GRAY));
        }
        appendRevolvercastTooltip(lines);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private RevolvercastStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new RevolvercastStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, ORB_CONTROLLER, 0, state -> {
                    state.setAnimation(ANIM_ORB_LOOP);
                    return PlayState.CONTINUE;
                })
        );
        controllerRegistrar.add(
                new AnimationController<>(this, REVOLVE_CONTROLLER, 0, state -> PlayState.STOP)
                        .triggerableAnim(REVOLVE_ANIMATION, ANIM_REVOLVE)
                        .triggerableAnim(REVOLVE_ANIMATION_ALT, ANIM_REVOLVE_ALT)
        );
    }

    @Override
    public @NotNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    public static boolean canSwingCastSpell(@Nullable AbstractSpell spell) {
        if (spell == null || spell == SpellRegistry.none()) {
            return false;
        }

        return EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG)
                .contains(SpellGunCastType.from(spell.getCastType()));
    }

    public static @NotNull ItemStack getCalibrationAdjustment(@NotNull ItemStack staffStack, int slot) {
        return getCalibrationItem(staffStack, ADJUSTMENTS_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    public static void setCalibrationAdjustment(@NotNull ItemStack staffStack, int slot, @NotNull ItemStack stack) {
        setCalibrationItem(staffStack, ADJUSTMENTS_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, stack);
        refreshResolvedCalibrationSchool(staffStack);
        refreshSelectedSpellContainer(staffStack);
    }

    public static @NotNull ItemStack getCalibrationScroll(@NotNull ItemStack staffStack, int slot) {
        return getCalibrationItem(staffStack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT);
    }

    public static void setCalibrationScroll(@NotNull ItemStack staffStack, int slot, @NotNull ItemStack stack) {
        setCalibrationItem(staffStack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT, stack);
        refreshSelectedSpellContainer(staffStack);
    }

    public static boolean hasAnyCalibrationScroll(@NotNull ItemStack staffStack) {
        return findFirstValidScrollIndex(staffStack) >= 0;
    }

    public static int getEnabledCalibrationScrollSlotCount(@NotNull ItemStack staffStack) {
        if (!isValidCalibrationAccess(staffStack, 0, 1)) {
            return 0;
        }

        var upgradeCount = 0;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isCalibrationSlotUpgrade(getCalibrationAdjustment(staffStack, slot))) {
                ++upgradeCount;
            }
        }

        return Math.min(
                CALIBRATION_SCROLL_SLOT_COUNT,
                BASE_CALIBRATION_SCROLL_SLOT_COUNT + upgradeCount * CALIBRATION_SCROLL_SLOTS_PER_UPGRADE
        );
    }

    public static int getSelectedScrollIndex(@NotNull ItemStack staffStack) {
        if (!isValidCalibrationAccess(staffStack, 0, 1)) {
            return -1;
        }

        var calibrationTag = staffStack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null || !calibrationTag.contains(SELECTED_SCROLL_INDEX_TAG, Tag.TAG_INT)) {
            return -1;
        }

        var index = calibrationTag.getInt(SELECTED_SCROLL_INDEX_TAG);
        return index >= 0 && index < CALIBRATION_SCROLL_SLOT_COUNT ? index : -1;
    }

    public static void setSelectedScrollIndex(@NotNull ItemStack staffStack, int selectedScrollIndex) {
        if (!isValidCalibrationAccess(staffStack, 0, 1)) {
            return;
        }

        if (isSelectableScrollIndex(staffStack, selectedScrollIndex)) {
            setStoredSelectedScrollIndex(staffStack, selectedScrollIndex);
        }
        refreshSelectedSpellContainer(staffStack);
    }

    public static boolean isSelectableScrollIndex(@NotNull ItemStack staffStack, int selectedScrollIndex) {
        return isValidCalibrationAccess(staffStack, 0, 1)
                && selectedScrollIndex >= 0
                && selectedScrollIndex < getEnabledCalibrationScrollSlotCount(staffStack)
                && isValidScrollSpell(getCalibrationScroll(staffStack, selectedScrollIndex));
    }

    public static int normalizeSelectedScrollIndex(@NotNull ItemStack staffStack) {
        if (!isValidCalibrationAccess(staffStack, 0, 1)) {
            return -1;
        }

        var selectedIndex = getSelectedScrollIndex(staffStack);
        if (isSelectableScrollIndex(staffStack, selectedIndex)) {
            return selectedIndex;
        }

        var nextValidIndex = findNextValidScrollIndex(staffStack, selectedIndex);
        if (nextValidIndex < 0) {
            clearSelectedScrollIndex(staffStack);
            ISpellContainer.remove(staffStack);
            return -1;
        }

        setStoredSelectedScrollIndex(staffStack, nextValidIndex);
        return nextValidIndex;
    }

    public static boolean advanceToNextValidScrollIndex(@NotNull ItemStack staffStack) {
        if (!isValidCalibrationAccess(staffStack, 0, 1)) {
            return false;
        }

        var selectedIndex = normalizeSelectedScrollIndex(staffStack);
        if (selectedIndex < 0) {
            refreshSelectedSpellContainer(staffStack);
            return false;
        }

        var nextValidIndex = findNextValidScrollIndex(staffStack, selectedIndex);
        if (nextValidIndex < 0) {
            clearSelectedScrollIndex(staffStack);
            ISpellContainer.remove(staffStack);
            return false;
        }

        setStoredSelectedScrollIndex(staffStack, nextValidIndex);
        refreshSelectedSpellContainer(staffStack);
        return true;
    }

    public static @NotNull SpellData getSelectedSpellData(@NotNull ItemStack staffStack) {
        var selectedIndex = normalizeSelectedScrollIndex(staffStack);
        if (selectedIndex < 0) {
            return SpellData.EMPTY;
        }

        return getScrollSpellData(getCalibrationScroll(staffStack, selectedIndex));
    }

    public static void refreshSelectedSpellContainer(@NotNull ItemStack staffStack) {
        var spellData = getSelectedSpellData(staffStack);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            ISpellContainer.remove(staffStack);
            return;
        }

        if (isCurrentSelectedSpellContainer(staffStack, spellData)) {
            return;
        }

        var spellContainer = ISpellContainer.create(1, true, false).mutableCopy();
        spellContainer.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
        ISpellContainer.set(staffStack, spellContainer.toImmutable());
    }

    public static void refreshResolvedCalibrationSchool(@NotNull ItemStack staffStack) {
        if (!isValidCalibrationAccess(staffStack, 0, 1)) {
            return;
        }

        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(getCalibrationAdjustment(staffStack, slot));
            if (school.isPresent()) {
                setResolvedCalibrationSchoolId(staffStack, school.get().getId());
                return;
            }
        }

        clearResolvedCalibrationSchool(staffStack);
    }

    public static @Nullable SchoolType getResolvedCalibrationSchool(ItemStack stack) {
        var schoolId = getResolvedCalibrationSchoolId(stack);
        return schoolId == null ? null : SchoolRegistry.getSchool(schoolId);
    }

    public static boolean isCalibrationAdjustmentItem(@NotNull ItemStack stack) {
        return isCalibrationSlotUpgrade(stack) || ScrollcasterSchoolRuneResolver.isSchoolRune(stack) || isRecoveryRune(stack);
    }

    public static boolean isCalibrationSlotUpgrade(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES);
    }

    public static boolean isRecoveryRune(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get();
    }

    public static boolean hasRecoveryRune(@NotNull ItemStack staffStack) {
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isRecoveryRune(getCalibrationAdjustment(staffStack, slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean advanceAfterFailedCastIfNeeded(ItemStack stack) {
        if (hasRecoveryRune(stack)) {
            return advanceToNextValidScrollIndex(stack);
        }
        return false;
    }

    private void triggerRevolveAnimationIfPossible(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var instanceId = GeoItem.getOrAssignId(stack, serverPlayer.serverLevel());
        triggerAnim(serverPlayer, instanceId, REVOLVE_CONTROLLER, nextRevolveAnimationName(stack));
        AudioTools.playSoundFromEntity(serverPlayer.serverLevel(), serverPlayer, SoundRegistry.REVOLVE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static String nextRevolveAnimationName(ItemStack stack) {
        var tag = stack.getOrCreateTag();
        var variant = tag.getInt(REVOLVE_ANIMATION_VARIANT_TAG);
        tag.putInt(REVOLVE_ANIMATION_VARIANT_TAG, variant + 1);
        return (variant & 1) == 0 ? REVOLVE_ANIMATION : REVOLVE_ANIMATION_ALT;
    }

    private int resolveEffectiveSpellLevel(Player player, ItemStack stack, AbstractSpell spell) {
        var spellData = getSelectedSpellData(stack);
        if (spellData != SpellData.EMPTY && spell.equals(spellData.getSpell())) {
            return spell.getLevelFor(spellData.getLevel(), player);
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && spell.getSpellId().equals(magicData.getCastingSpellId()) && magicData.getCastingSpellLevel() > 0) {
            return magicData.getCastingSpellLevel();
        }

        return spell.getLevelFor(1, player);
    }

    private void appendRevolvercastTooltip(List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                List.of(
                        ImbueTooltipHelper.translatableGray(
                                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
                        ),
                        ImbueTooltipHelper.translatableGray(
                                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown"
                        )
                ),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_swingcast_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectRestrictTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
    }

    private static List<Component> collectRestrictTooltipSection() {
        return ImbueTooltipHelper.collectCastTypeRestrictionLines(
                EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG)
        );
    }

    private static @Nullable Attribute getResolvedSchoolPowerAttribute(ItemStack stack) {
        return MagicTools.resolveSchoolPowerAttribute(getResolvedCalibrationSchool(stack));
    }

    private static @NotNull ItemStack getCalibrationItem(@NotNull ItemStack staffStack, String listName,
                                                         int slot, int slotCount) {
        if (!isValidCalibrationAccess(staffStack, slot, slotCount)) {
            return ItemStack.EMPTY;
        }

        var calibrationTag = staffStack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null || !calibrationTag.contains(listName, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }

        var list = calibrationTag.getList(listName, Tag.TAG_COMPOUND);
        for (var index = 0; index < list.size(); ++index) {
            var entry = list.getCompound(index);
            if (entry.getInt(SLOT_TAG) != slot || !entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            return ItemStack.of(entry.getCompound(ITEM_TAG));
        }
        return ItemStack.EMPTY;
    }

    private static void setCalibrationItem(@NotNull ItemStack staffStack, String listName, int slot, int slotCount,
                                           @NotNull ItemStack stack) {
        if (!isValidCalibrationAccess(staffStack, slot, slotCount)) {
            return;
        }

        var calibrationTag = staffStack.getOrCreateTagElement(CALIBRATION_TAG);
        var list = calibrationTag.contains(listName, Tag.TAG_LIST)
                ? calibrationTag.getList(listName, Tag.TAG_COMPOUND)
                : new ListTag();
        removeCalibrationItem(list, slot);

        if (!stack.isEmpty()) {
            var storedStack = stack.copy();
            storedStack.setCount(1);
            var entry = new CompoundTag();
            entry.putInt(SLOT_TAG, slot);
            entry.put(ITEM_TAG, storedStack.save(new CompoundTag()));
            list.add(entry);
        }

        if (list.isEmpty()) {
            calibrationTag.remove(listName);
        } else {
            calibrationTag.put(listName, list);
        }
        if (calibrationTag.isEmpty()) {
            staffStack.removeTagKey(CALIBRATION_TAG);
        }
    }

    private static int findFirstValidScrollIndex(@NotNull ItemStack staffStack) {
        var enabledSlotCount = getEnabledCalibrationScrollSlotCount(staffStack);
        for (var slot = 0; slot < enabledSlotCount; ++slot) {
            if (isValidScrollSpell(getCalibrationScroll(staffStack, slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static int findNextValidScrollIndex(@NotNull ItemStack staffStack, int currentIndex) {
        var enabledSlotCount = getEnabledCalibrationScrollSlotCount(staffStack);
        if (enabledSlotCount <= 0) {
            return -1;
        }

        var startIndex = currentIndex < 0 ? 0 : currentIndex + 1;
        for (var offset = 0; offset < enabledSlotCount; ++offset) {
            var slot = (startIndex + offset) % enabledSlotCount;
            if (isValidScrollSpell(getCalibrationScroll(staffStack, slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isValidScrollSpell(@NotNull ItemStack scrollStack) {
        return getScrollSpellData(scrollStack) != SpellData.EMPTY;
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
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private static boolean isCurrentSelectedSpellContainer(@NotNull ItemStack staffStack, @NotNull SpellData spellData) {
        var current = ISpellContainer.get(staffStack);
        if (current == null) {
            return false;
        }

        var currentSpell = current.getSpellAtIndex(0);
        return current.getMaxSpellCount() == 1
                && current.isSpellWheel()
                && !current.mustEquip()
                && currentSpell != SpellData.EMPTY
                && currentSpell.getSpell() == spellData.getSpell()
                && currentSpell.getLevel() == spellData.getLevel()
                && !currentSpell.isLocked();
    }

    private static void setStoredSelectedScrollIndex(@NotNull ItemStack staffStack, int selectedScrollIndex) {
        if (selectedScrollIndex < 0 || selectedScrollIndex >= CALIBRATION_SCROLL_SLOT_COUNT) {
            clearSelectedScrollIndex(staffStack);
            return;
        }

        staffStack.getOrCreateTagElement(CALIBRATION_TAG).putInt(SELECTED_SCROLL_INDEX_TAG, selectedScrollIndex);
    }

    private static void clearSelectedScrollIndex(@NotNull ItemStack staffStack) {
        var calibrationTag = staffStack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null) {
            return;
        }

        calibrationTag.remove(SELECTED_SCROLL_INDEX_TAG);
        if (calibrationTag.isEmpty()) {
            staffStack.removeTagKey(CALIBRATION_TAG);
        }
    }

    private static ResourceLocation getResolvedCalibrationSchoolId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        var calibrationTag = stack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null || !calibrationTag.contains(SCHOOL_POWER_SCHOOL_TAG, Tag.TAG_STRING)) {
            return null;
        }

        return ResourceLocation.tryParse(calibrationTag.getString(SCHOOL_POWER_SCHOOL_TAG));
    }

    private static void setResolvedCalibrationSchoolId(ItemStack stack, ResourceLocation schoolId) {
        stack.getOrCreateTagElement(CALIBRATION_TAG).putString(SCHOOL_POWER_SCHOOL_TAG, schoolId.toString());
    }

    private static void clearResolvedCalibrationSchool(ItemStack stack) {
        var calibrationTag = stack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null) {
            return;
        }

        calibrationTag.remove(SCHOOL_POWER_SCHOOL_TAG);
        if (calibrationTag.isEmpty()) {
            stack.removeTagKey(CALIBRATION_TAG);
        }
    }

    private static void removeCalibrationItem(ListTag list, int slot) {
        for (var index = list.size() - 1; index >= 0; --index) {
            if (list.getCompound(index).getInt(SLOT_TAG) == slot) {
                list.remove(index);
            }
        }
    }

    private static boolean isValidCalibrationAccess(@NotNull ItemStack staffStack, int slot, int slotCount) {
        return !staffStack.isEmpty()
                && staffStack.getItem() instanceof RevolvercastStaff
                && slot >= 0
                && slot < slotCount;
    }

    private static String resolveSpellSelectionSlot(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
    }
}
