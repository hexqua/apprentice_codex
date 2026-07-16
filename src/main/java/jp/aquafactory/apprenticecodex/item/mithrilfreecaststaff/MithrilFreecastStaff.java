package jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff;

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
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.*;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.MithrilFreecastStaffRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class MithrilFreecastStaff extends AbstractRightClickMagicWeaponItem
        implements GeoItem, CastAnimationOverrideItem, IJeiInfoItem, SwingTriggeredMagicItem,
        ArcaneAnvilImbueBlockItem, SpellCalibrationAdjustmentTarget {
    private static final String ITEM_KEY = "mithril_freecast_staff";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.mithril_freecast_staff.desc_";
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            CalibrationAdjustmentProfile.of(
                    CalibrationAdjustmentRule.unique(
                            ScrollcasterSchoolRuneResolver::isSchoolRune,
                            CalibrationAdjustmentHints.schoolRunes()
                    ),
                    CalibrationAdjustmentRule.unique(
                            MithrilFreecastStaff::isSilverRing,
                            CalibrationAdjustmentHints.silverRing()
                    )
            );
    private static final String MAIN_CONTROLLER = "main";
    private static final String CALIBRATION_TAG = "SpellCalibration";
    private static final String ADJUSTMENTS_TAG = "Adjustments";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String SCHOOL_POWER_SCHOOL_TAG = "SchoolPowerSchool";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final int ENCHANTMENT_VALUE = 15;
    private static final double DISPLAYED_ATTACK_DAMAGE = 8.0D;
    private static final double DISPLAYED_ATTACK_SPEED = 1.8D;
    private static final double GENERAL_SPELL_POWER_BONUS = 0.10D;
    private static final double SCHOOL_SPELL_POWER_BONUS = 0.15D;
    private static final UUID SPELL_POWER_MODIFIER_ID = UUID.fromString("c5a73ed3-47f1-47f2-9d2d-8eb2dc6426f3");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/geo/" + ITEM_KEY + ".png"
    );

    public MithrilFreecastStaff() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                true,
                ENCHANTMENT_VALUE,
                ITEM_KEY,
                DISPLAYED_ATTACK_DAMAGE,
                DISPLAYED_ATTACK_SPEED - 4.0D
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
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
                        "apprenticecodex.mithril_freecast_staff.mainhand.spell_power",
                        spellPowerAmount,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
        return builder.build();
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
        initializeSpellContainer(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (EnchantmentRegistry.TRANSCENDENCE.isPresent() && enchantment == EnchantmentRegistry.TRANSCENDENCE.get()) {
            return false;
        }

        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public Handling transcendenceHandling() {
        return Handling.DISABLED;
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

        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var selectionOption = new SpellSelectionManager(player).getSelection();
        if (selectionOption == null || selectionOption.spellData == SpellData.EMPTY) {
            return false;
        }

        var spellData = selectionOption.spellData;
        var spell = spellData.getSpell();
        if (!canSwingCastSpell(spell, hasSilverRingAdjustment(stack))) {
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            return false;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var selectedCastSource = selectionOption.getCastSource();
        try (var swingTriggeredContext = SwingcastStaffCastContext.open(player.getUUID(), stack, spell);
             var ignored = MithrilFreecastStaffCastContext.open(player.getUUID(), stack, spell, selectedCastSource)) {
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
                return false;
            }

            MithrilFreecastStaffCastContext.retainUntilCooldown(
                    player.getUUID(),
                    stack,
                    spell,
                    selectedCastSource
            );
            TriggeredSpellCastHelper.applyLongCastDurationOverride(
                    player,
                    spellLevel,
                    spell,
                    magicData,
                    resolveSpellSelectionSlot(hand),
                    spell.getCastType() == CastType.LONG && hasSilverRingAdjustment(stack) ? 0 : null
            );
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Mithril Freecast Staff swing cast context failed to close.", exception);
        }
    }

    public int resolveSwingTriggeredCooldownTicks(Player player, AbstractSpell spell, int currentEffectiveCooldown) {
        var spellLevel = resolveEffectiveSpellLevel(player, spell);
        return currentEffectiveCooldown
                + (spell.getCastType() == CastType.LONG ? spell.getEffectiveCastTime(spellLevel, player) : 0);
    }

    public int resolveSwingTriggeredCooldownTicks(
            Player player,
            AbstractSpell spell,
            CastSource selectedCastSource
    ) {
        return resolveSwingTriggeredCooldownTicks(
                player,
                spell,
                WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, selectedCastSource)
        );
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("item.apprenticecodex.freecast.common.desc").withStyle(ChatFormatting.GRAY));
        var resolvedSchool = getResolvedCalibrationSchool(stack);
        if (resolvedSchool != null) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.revolvercast_staff.school_rune",
                    resolvedSchool.getDisplayName()
            ).withStyle(ChatFormatting.GRAY));
        }
        appendFreecastTooltip(stack, lines);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private MithrilFreecastStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MithrilFreecastStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                })
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
        return canSwingCastSpell(spell, false);
    }

    public static boolean canSwingCastSpell(@Nullable AbstractSpell spell, boolean enablesLongCast) {
        if (spell == null || spell == SpellRegistry.none()) {
            return false;
        }

        return getSupportedSwingCastTypes(enablesLongCast)
                .contains(SpellGunCastType.from(spell.getCastType()));
    }

    public static EnumSet<SpellGunCastType> getSupportedSwingCastTypes(@NotNull ItemStack staffStack) {
        return getSupportedSwingCastTypes(hasSilverRingAdjustment(staffStack));
    }

    public static EnumSet<SpellGunCastType> getSupportedSwingCastTypes(boolean enablesLongCast) {
        return enablesLongCast
                ? EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG)
                : EnumSet.of(SpellGunCastType.INSTANT);
    }

    private static @NotNull ItemStack readCalibrationAdjustment(@NotNull ItemStack staffStack, int slot) {
        return getCalibrationItem(staffStack, slot);
    }

    private static void writeCalibrationAdjustment(@NotNull ItemStack staffStack, int slot, @NotNull ItemStack stack) {
        setCalibrationItem(staffStack, slot, stack);
        refreshResolvedCalibrationSchool(staffStack);
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    @Override
    public @NotNull ItemStack getCalibrationAdjustment(@NotNull ItemStack targetStack, int slot) {
        return readCalibrationAdjustment(targetStack, slot);
    }

    @Override
    public boolean trySetCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment
    ) {
        if (!canPlaceCalibrationAdjustment(targetStack, slot, adjustment)) {
            return false;
        }
        writeCalibrationAdjustment(targetStack, slot, adjustment);
        return true;
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_PROFILE;
    }

    public static void refreshResolvedCalibrationSchool(@NotNull ItemStack staffStack) {
        if (!isValidCalibrationAccess(staffStack, 0)) {
            return;
        }

        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(readCalibrationAdjustment(staffStack, slot));
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

    public static boolean isSilverRing(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get();
    }

    public static boolean hasSilverRingAdjustment(@NotNull ItemStack staffStack) {
        if (!isValidCalibrationAccess(staffStack, 0)) {
            return false;
        }

        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isSilverRing(readCalibrationAdjustment(staffStack, slot))) {
                return true;
            }
        }
        return false;
    }

    private int resolveEffectiveSpellLevel(Player player, AbstractSpell spell) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null
                && Objects.equals(spell.getSpellId(), magicData.getCastingSpellId())
                && magicData.getCastingSpellLevel() > 0) {
            return magicData.getCastingSpellLevel();
        }

        var selectionOption = new SpellSelectionManager(player).getSelection();
        if (selectionOption != null && selectionOption.spellData != SpellData.EMPTY
                && spell.equals(selectionOption.spellData.getSpell())) {
            return spell.getLevelFor(selectionOption.spellData.getLevel(), player);
        }

        return spell.getLevelFor(1, player);
    }

    private static @Nullable Attribute getResolvedSchoolPowerAttribute(ItemStack stack) {
        return MagicTools.resolveSchoolPowerAttribute(getResolvedCalibrationSchool(stack));
    }

    private static @NotNull ItemStack getCalibrationItem(@NotNull ItemStack staffStack, int slot) {
        if (!isValidCalibrationAccess(staffStack, slot)) {
            return ItemStack.EMPTY;
        }

        var calibrationTag = staffStack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null || !calibrationTag.contains(ADJUSTMENTS_TAG, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }

        var list = calibrationTag.getList(ADJUSTMENTS_TAG, Tag.TAG_COMPOUND);
        for (var index = 0; index < list.size(); ++index) {
            var entry = list.getCompound(index);
            if (entry.getInt(SLOT_TAG) != slot || !entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            return ItemStack.of(entry.getCompound(ITEM_TAG));
        }
        return ItemStack.EMPTY;
    }

    private static void setCalibrationItem(@NotNull ItemStack staffStack, int slot, @NotNull ItemStack stack) {
        if (!isValidCalibrationAccess(staffStack, slot)) {
            return;
        }

        var calibrationTag = staffStack.getOrCreateTagElement(CALIBRATION_TAG);
        var list = calibrationTag.contains(ADJUSTMENTS_TAG, Tag.TAG_LIST)
                ? calibrationTag.getList(ADJUSTMENTS_TAG, Tag.TAG_COMPOUND)
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
            calibrationTag.remove(ADJUSTMENTS_TAG);
        } else {
            calibrationTag.put(ADJUSTMENTS_TAG, list);
        }
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

    private static boolean isValidCalibrationAccess(@NotNull ItemStack staffStack, int slot) {
        return !staffStack.isEmpty()
                && staffStack.getItem() instanceof MithrilFreecastStaff
                && slot >= 0
                && slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    private void appendFreecastTooltip(ItemStack stack, List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        var abilityLines = hasSilverRingAdjustment(stack)
                ? List.of(
                        ImbueTooltipHelper.translatableGray(
                                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
                        ),
                        ImbueTooltipHelper.translatableGray(
                                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown"
                        )
                )
                : List.<Component>of();
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                abilityLines,
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_freecast_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                ImbueTooltipHelper.collectCastTypeRestrictionLines(
                        getSupportedSwingCastTypes(stack)
                ),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_freecast_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
    }

    private static String resolveSpellSelectionSlot(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
    }
}
