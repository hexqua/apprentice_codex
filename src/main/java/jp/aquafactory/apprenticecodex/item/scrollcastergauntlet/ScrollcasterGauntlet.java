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
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.*;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.ScrollcasterGauntletRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.HandStackResolver;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class ScrollcasterGauntlet extends Item implements GeoItem, IPresetSpellContainer, UniqueItem,
        ItemTransformPreservingCastAnimationItem, ArcaneAnvilScrollImbueBlockItem,
        BetterCombatOffhandDualWieldingPolicyItem, SwingTriggeredMagicItem, PriorityOffhandUseDeferringItem, IJeiInfoItem,
        ImmediateSneakSelectionUiItem, StoredSpellCalibrationImbueTarget, SpellCalibrationAdjustmentTarget,
        TranscendencePolicy, AttributeEnchantmentPolicy, WisdomPolicy {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.scrollcaster_gauntlet.desc_";

    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    public static final int CALIBRATION_SCROLL_SLOT_COUNT = 10;
    public static final int BASE_CALIBRATION_SCROLL_SLOT_COUNT = 4;
    public static final int CALIBRATION_SCROLL_SLOTS_PER_UPGRADE = 2;
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            CalibrationAdjustmentProfile.of(
                    CalibrationAdjustmentRule.repeatable(
                            ScrollcasterGauntlet::isCalibrationSlotUpgrade,
                            CalibrationAdjustmentHints.slotUpgrades()
                    ),
                    CalibrationAdjustmentRule.uniqueBy(
                            stack -> stack.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_ENCHANTMENT_BOOKS),
                            stack -> {
                                var candidate = readFirstBookEnchantment(stack);
                                return candidate == null ? null : candidate.enchantment();
                            },
                            CalibrationAdjustmentHints.enchantmentBooks()
                    ),
                    CalibrationAdjustmentRule.unique(
                            ScrollcasterGauntlet::isFreecastStaffAdjustment,
                            CalibrationAdjustmentHints.mithrilFreecastStaff()
                    ),
                    CalibrationAdjustmentRule.unique(
                            ScrollcasterSchoolRuneResolver::isSchoolRune,
                            CalibrationAdjustmentHints.schoolRunes()
                    )
            );

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
        return SpellCalibrationImbueState.ACCEPTED_USABLE;
    }

    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final ResourceLocation MALUM_REPLENISHING =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "replenishing");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_hunter_weapon")
    );
    private static final String MAIN_CONTROLLER = "main";
    private static final String CALIBRATION_TAG = "SpellCalibration";
    private static final String ADJUSTMENTS_TAG = "Adjustments";
    private static final String SCROLLS_TAG = "Scrolls";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String SCHOOL_POWER_SCHOOL_TAG = "SchoolPowerSchool";
    private static final String SELECTED_SCROLL_INDEX_TAG = "SelectedScrollIndex";
    private static final String VANILLA_ENCHANTMENTS_TAG = "Enchantments";
    private static final String STORED_ENCHANTMENTS_TAG = "StoredEnchantments";
    private static final String ENCHANTMENT_ID_TAG = "id";
    private static final String ENCHANTMENT_LEVEL_TAG = "lvl";
    private static final String CAST_ANIMATION = "cast";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_CAST = RawAnimation.begin().thenPlay("cast");
    private static final double ATTACK_DAMAGE_BONUS = 5.0D;
    private static final double ATTACK_SPEED_BONUS = -2.2D;
    private static final double EPIC_FIGHT_ATTACK_DAMAGE_BONUS = 2.0D;
    private static final double EPIC_FIGHT_ATTACK_SPEED_BONUS = 0.0D;
    private static final double SPELL_POWER_BONUS = 0.05D;
    private static final double SCHOOL_SPELL_POWER_BONUS = 0.10D;
    private static final UUID SPELL_POWER_MODIFIER_ID = UUID.fromString("be797f84-cdc5-41fd-871f-685cebb23f5c");
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_SWORD);
    private static final ItemStack PICKAXE_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_PICKAXE);
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public boolean supportsDirectTranscendenceApplication() {
        return false;
    }

    public ScrollcasterGauntlet() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return slot == EquipmentSlot.MAINHAND
                ? AttributeEnchantmentResolver.resolveMergedModifiers(
                        buildMainhandModifiers(stack),
                        stack,
                        "apprenticecodex.scrollcaster_gauntlet"
                )
                : super.getAttributeModifiers(slot, stack);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean supportsDirectWisdomApplication() {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
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

        var spellData = getSelectedSpellData(stack);
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
        if (hasAnyCalibrationScroll(stack) && !isCurrentSelectedSpellContainer(stack)) {
            refreshSelectedSpellContainer(stack);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
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
        if (hasFreecastStaffAdjustment(stack)) {
            lines.add(Component.translatable("item.apprenticecodex.freecast.common.desc")
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, lines, flag);
    }

    @Override
    public boolean canTriggerSpellOnSwing(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        return stack.getItem() == this && hasFreecastStaffAdjustment(stack);
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

        refreshSelectedSpellContainer(stack);
        var spellData = getSelectedSpellData(stack);
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
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ScrollcasterGauntletRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ScrollcasterGauntletRenderer();
                }

                return renderer;
            }
        });
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

    private static Multimap<Attribute, AttributeModifier> buildMainhandModifiers(ItemStack stack) {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        var attackDamageBonus = getAttackDamageBonus();
        var attackSpeedBonus = getAttackSpeedBonus();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        attackDamageBonus,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_UUID,
                        "Weapon modifier",
                        attackSpeedBonus,
                        AttributeModifier.Operation.ADDITION
                )
        );
        var schoolPowerAttribute = getResolvedSchoolPowerAttribute(stack);
        if (schoolPowerAttribute != null) {
            builder.put(
                    schoolPowerAttribute,
                    new AttributeModifier(
                            SPELL_POWER_MODIFIER_ID,
                            "apprenticecodex.scrollcaster_gauntlet.mainhand.school_spell_power",
                            SCHOOL_SPELL_POWER_BONUS,
                            AttributeModifier.Operation.MULTIPLY_BASE
                    )
            );
        } else if (shouldApplyBaseSpellPowerBonus(stack)) {
            builder.put(
                    AttributeRegistry.SPELL_POWER.get(),
                    new AttributeModifier(
                            SPELL_POWER_MODIFIER_ID,
                            "apprenticecodex.scrollcaster_gauntlet.mainhand.spell_power",
                            SPELL_POWER_BONUS,
                            AttributeModifier.Operation.MULTIPLY_BASE
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

    private static boolean shouldApplyBaseSpellPowerBonus(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !hasResolvedCalibrationSchool(stack);
    }

    private static boolean isCompatibleWithResolvedEnchantments(
            Enchantment enchantment,
            Map<Enchantment, Integer> resolvedEnchantments
    ) {
        for (var resolvedEnchantment : resolvedEnchantments.keySet()) {
            if (!enchantment.isCompatibleWith(resolvedEnchantment)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSupportedCalibrationEnchantment(ItemStack gauntletStack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null || ApprenticeCodexServerConfig.isScrollcasterGauntletEnchantmentDenied(enchantmentId)) {
            return false;
        }

        return isExplicitlySupportedMagicEnchantment(enchantment)
                || isMalumSpiritPlunder(gauntletStack, enchantment)
                || ApprenticeCodexServerConfig.isScrollcasterGauntletCompatAdditionalAllowedEnchantment(enchantmentId)
                || ((enchantment.canApplyAtEnchantingTable(SWORD_ENCHANTMENT_PROBE_STACK)
                        || enchantment.canApplyAtEnchantingTable(PICKAXE_ENCHANTMENT_PROBE_STACK))
                && !enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK));
    }

    private static boolean isExplicitlySupportedMagicEnchantment(Enchantment enchantment) {
        return MALUM_REPLENISHING.equals(ForgeRegistries.ENCHANTMENTS.getKey(enchantment))
                || AttributeEnchantmentType.from(enchantment).isPresent()
                || matches(enchantment, EnchantmentRegistry.TRANSCENDENCE)
                || matches(enchantment, EnchantmentRegistry.WISDOM);
    }

    private static boolean isMalumSpiritPlunder(ItemStack gauntletStack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        return MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && gauntletStack.is(MALUM_SOUL_HUNTER_WEAPON);
    }

    private static boolean matches(Enchantment enchantment, RegistryObject<Enchantment> registryObject) {
        return registryObject.isPresent() && enchantment == registryObject.get();
    }

    private static @Nullable CalibrationEnchantmentCandidate readFirstBookEnchantment(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_ENCHANTMENT_BOOKS)) {
            return null;
        }

        var tag = stack.getTag();
        if (tag == null || !tag.contains(STORED_ENCHANTMENTS_TAG, Tag.TAG_LIST)) {
            return null;
        }

        var storedEnchantments = tag.getList(STORED_ENCHANTMENTS_TAG, Tag.TAG_COMPOUND);
        if (storedEnchantments.isEmpty()) {
            return null;
        }

        // tooltipをソートするMODに左右されないよう、エンチャント本の保存順で先頭だけを読む。
        var firstEnchantmentTag = storedEnchantments.getCompound(0);
        var enchantmentId = ResourceLocation.tryParse(firstEnchantmentTag.getString(ENCHANTMENT_ID_TAG));
        if (enchantmentId == null) {
            return null;
        }

        var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        var level = firstEnchantmentTag.getInt(ENCHANTMENT_LEVEL_TAG);
        if (enchantment == null || level <= 0) {
            return null;
        }
        return new CalibrationEnchantmentCandidate(enchantment, level);
    }

    private static Attribute getResolvedSchoolPowerAttribute(ItemStack stack) {
        return MagicTools.resolveSchoolPowerAttribute(getResolvedCalibrationSchool(stack));
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
        return getCalibrationItem(gauntletStack, ADJUSTMENTS_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    private static void writeCalibrationAdjustment(@NotNull ItemStack gauntletStack, int slot, @NotNull ItemStack stack) {
        setCalibrationItem(gauntletStack, ADJUSTMENTS_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, stack);
        refreshCalibrationEnchantments(gauntletStack);
        refreshResolvedCalibrationSchool(gauntletStack);
        refreshSelectedSpellContainer(gauntletStack);
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

    public static @NotNull ItemStack getCalibrationScroll(@NotNull ItemStack gauntletStack, int slot) {
        return getCalibrationItem(gauntletStack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT);
    }

    public static void setCalibrationScroll(@NotNull ItemStack gauntletStack, int slot, @NotNull ItemStack stack) {
        setCalibrationItem(gauntletStack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT, stack);
        refreshSelectedSpellContainer(gauntletStack);
    }

    public static boolean hasAnyCalibrationScroll(@NotNull ItemStack gauntletStack) {
        return findFirstValidScrollIndex(gauntletStack) >= 0;
    }

    public static boolean hasFreecastStaffAdjustment(@NotNull ItemStack gauntletStack) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return false;
        }

        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isFreecastStaffAdjustment(readCalibrationAdjustment(gauntletStack, slot))) {
                return true;
            }
        }
        return false;
    }

    public static void refreshCalibrationEnchantments(@NotNull ItemStack gauntletStack) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return;
        }

        var candidatesByEnchantment = new LinkedHashMap<Enchantment, CalibrationEnchantmentCandidate>();
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            var candidate = readFirstBookEnchantment(readCalibrationAdjustment(gauntletStack, slot));
            if (candidate == null || !isSupportedCalibrationEnchantment(gauntletStack, candidate.enchantment())) {
                continue;
            }

            var existing = candidatesByEnchantment.get(candidate.enchantment());
            if (existing == null || candidate.level() > existing.level()) {
                candidatesByEnchantment.put(candidate.enchantment(), candidate);
            }
        }

        var resolvedEnchantments = new LinkedHashMap<Enchantment, Integer>();
        for (var candidate : candidatesByEnchantment.values()) {
            if (isCompatibleWithResolvedEnchantments(candidate.enchantment(), resolvedEnchantments)) {
                resolvedEnchantments.put(candidate.enchantment(), candidate.level());
            }
        }

        if (resolvedEnchantments.isEmpty()) {
            gauntletStack.removeTagKey(VANILLA_ENCHANTMENTS_TAG);
            return;
        }

        EnchantmentHelper.setEnchantments(resolvedEnchantments, gauntletStack);
    }

    public static int getEnabledCalibrationScrollSlotCount(@NotNull ItemStack gauntletStack) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return 0;
        }

        var upgradeCount = 0;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isCalibrationSlotUpgrade(readCalibrationAdjustment(gauntletStack, slot))) {
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

        var calibrationTag = gauntletStack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null || !calibrationTag.contains(SELECTED_SCROLL_INDEX_TAG, Tag.TAG_INT)) {
            return -1;
        }

        var index = calibrationTag.getInt(SELECTED_SCROLL_INDEX_TAG);
        return index >= 0 && index < CALIBRATION_SCROLL_SLOT_COUNT ? index : -1;
    }

    public static void setSelectedScrollIndex(@NotNull ItemStack gauntletStack, int selectedScrollIndex) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return;
        }

        if (!isSelectableScrollIndex(gauntletStack, selectedScrollIndex)) {
            refreshSelectedSpellContainer(gauntletStack);
            return;
        }

        setStoredSelectedScrollIndex(gauntletStack, selectedScrollIndex);
        refreshSelectedSpellContainer(gauntletStack);
    }

    public static boolean isSelectableScrollIndex(@NotNull ItemStack gauntletStack, int selectedScrollIndex) {
        return isValidCalibrationAccess(gauntletStack, 0, 1)
                && selectedScrollIndex >= 0
                && selectedScrollIndex < getEnabledCalibrationScrollSlotCount(gauntletStack)
                && isValidScrollSpell(getCalibrationScroll(gauntletStack, selectedScrollIndex));
    }

    public static int normalizeSelectedScrollIndex(@NotNull ItemStack gauntletStack) {
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return -1;
        }

        var selectedIndex = getSelectedScrollIndex(gauntletStack);
        if (isSelectableScrollIndex(gauntletStack, selectedIndex)) {
            return selectedIndex;
        }

        var firstValidIndex = findFirstValidScrollIndex(gauntletStack);
        if (firstValidIndex < 0) {
            clearSelectedScrollIndex(gauntletStack);
            ISpellContainer.remove(gauntletStack);
            return -1;
        }

        setStoredSelectedScrollIndex(gauntletStack, firstValidIndex);
        return firstValidIndex;
    }

    public static @NotNull SpellData getSelectedSpellData(@NotNull ItemStack gauntletStack) {
        var selectedIndex = normalizeSelectedScrollIndex(gauntletStack);
        if (selectedIndex < 0) {
            return SpellData.EMPTY;
        }

        return getScrollSpellData(getCalibrationScroll(gauntletStack, selectedIndex));
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
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return List.of();
        }

        normalizeSelectedScrollIndex(gauntletStack);
        var enabledSlotCount = getEnabledCalibrationScrollSlotCount(gauntletStack);
        var views = new ArrayList<SneakSelectionView>();
        for (var slot = 0; slot < enabledSlotCount; ++slot) {
            var spellData = getScrollSpellData(getCalibrationScroll(gauntletStack, slot));
            views.add(SneakSelectionView.forSpell(
                    slot,
                    spellData,
                    isSelectableScrollIndex(gauntletStack, slot)
            ));
        }
        return List.copyOf(views);
    }

    public static void refreshSelectedSpellContainer(@NotNull ItemStack gauntletStack) {
        var spellData = getSelectedSpellData(gauntletStack);
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
        var spellData = getSelectedSpellData(gauntletStack);
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
        if (!isValidCalibrationAccess(gauntletStack, 0, 1)) {
            return;
        }

        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(readCalibrationAdjustment(gauntletStack, slot));
            if (school.isPresent()) {
                setResolvedCalibrationSchoolId(gauntletStack, school.get().getId());
                return;
            }
        }

        clearResolvedCalibrationSchool(gauntletStack);
    }

    private static @NotNull ItemStack getCalibrationItem(@NotNull ItemStack gauntletStack, String listName,
                                                         int slot, int slotCount) {
        if (!isValidCalibrationAccess(gauntletStack, slot, slotCount)) {
            return ItemStack.EMPTY;
        }

        var calibrationTag = gauntletStack.getTagElement(CALIBRATION_TAG);
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

    private static void setCalibrationItem(@NotNull ItemStack gauntletStack, String listName, int slot, int slotCount,
                                           @NotNull ItemStack stack) {
        if (!isValidCalibrationAccess(gauntletStack, slot, slotCount)) {
            return;
        }

        var calibrationTag = gauntletStack.getOrCreateTagElement(CALIBRATION_TAG);
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
            gauntletStack.removeTagKey(CALIBRATION_TAG);
        }
    }

    private static int findFirstValidScrollIndex(@NotNull ItemStack gauntletStack) {
        var enabledSlotCount = getEnabledCalibrationScrollSlotCount(gauntletStack);
        for (var slot = 0; slot < enabledSlotCount; ++slot) {
            if (isValidScrollSpell(getCalibrationScroll(gauntletStack, slot))) {
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

        var spellData = getSelectedSpellData(stack);
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

        gauntletStack.getOrCreateTagElement(CALIBRATION_TAG).putInt(SELECTED_SCROLL_INDEX_TAG, selectedScrollIndex);
    }

    private static void clearSelectedScrollIndex(@NotNull ItemStack gauntletStack) {
        var calibrationTag = gauntletStack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null) {
            return;
        }

        calibrationTag.remove(SELECTED_SCROLL_INDEX_TAG);
        if (calibrationTag.isEmpty()) {
            gauntletStack.removeTagKey(CALIBRATION_TAG);
        }
    }

    private static boolean hasResolvedCalibrationSchool(ItemStack stack) {
        return getResolvedCalibrationSchoolId(stack) != null;
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

    private static boolean isValidCalibrationAccess(@NotNull ItemStack gauntletStack, int slot, int slotCount) {
        return !gauntletStack.isEmpty()
                && gauntletStack.getItem() instanceof ScrollcasterGauntlet
                && slot >= 0
                && slot < slotCount;
    }

    private record CalibrationEnchantmentCandidate(
            Enchantment enchantment,
            int level
    ) {
    }
}
