package jp.aquafactory.apprenticecodex.item.shield;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.CooldownInstance;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityAccessor;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicAttributeModifierHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class ParrycastBuckler extends AbstractImbueShieldItem
        implements GeoItem, IJeiInfoItem, SpellCalibrationAdjustmentTarget, AttributeEnchantmentPolicy {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.parrycast_buckler.desc_";
    public static final int DURABILITY = 1561;
    public static final int ENCHANTMENT_VALUE = 22;
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            CalibrationAdjustmentProfile.of(
                    CalibrationAdjustmentRule.unique(
                            MithrilFreecastStaff::isSilverRing,
                            CalibrationAdjustmentHints.silverRing()
                    ),
                    CalibrationAdjustmentRule.unique(
                            stack -> stack.is(ItemRegistry.WISDOM_SHARD.get()),
                            CalibrationAdjustmentHints.wisdomShard()
                    )
            );
    private static final String CALIBRATION_TAG = "ParrycastBucklerCalibration";
    private static final String USE_START_TICK_TAG = "ApprenticeCodexParrycastBucklerUseStart";
    private static final String SESSION_TRIGGERED_TAG = "ApprenticeCodexParrycastBucklerTriggered";
    private static final String LAST_DURABILITY_TICK_TAG = "ApprenticeCodexParrycastBucklerDurabilityTick";
    private static final String GRACE_TICK_TAG = "ApprenticeCodexParrycastBucklerGraceTick";
    private static final String GRACE_USES_TAG = "ApprenticeCodexParrycastBucklerGraceUses";
    private static final int REMOVE_ANIMATION_TICKS = 10;
    private static final Map<ItemStack, ClientAnimationState> CLIENT_ANIMATION_STATES = new WeakHashMap<>();
    private static final Map<LivingEntity, EnumMap<InteractionHand, ClientAnimationState>> CLIENT_ANIMATION_OWNERS = new WeakHashMap<>();
    private static long nextClientAnimationInstanceId = Long.MIN_VALUE;
    private static final ItemStack SHIELD_ENCHANTMENT_PROBE = new ItemStack(net.minecraft.world.item.Items.SHIELD);
    private static final Set<AttributeEnchantmentType> DIRECT_ATTRIBUTE_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.ALACRITY,
            AttributeEnchantmentType.TENSE
    );
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DEPLOY = RawAnimation.begin().thenPlayAndHold("deploy");
    private static final RawAnimation REMOVE_IDLE = RawAnimation.begin().thenPlay("remove").thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ParrycastBuckler() {
        super(new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.RARE).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    protected void appendAlwaysVisibleImbueTooltip(ItemStack stack, List<Component> lines) {
        lines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".parrycast_buckler.desc"));
        var castTooltip = hasWisdomShard(stack) ? "cast_wisdom" : "cast_default";
        lines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".parrycast_buckler." + castTooltip));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        var result = super.use(level, player, hand);
        if (result.getResult().consumesAction()) {
            if (!level.isClientSide) {
                var tag = player.getPersistentData();
                tag.putLong(USE_START_TICK_TAG, level.getGameTime());
                tag.putBoolean(SESSION_TRIGGERED_TAG, false);
            }
        }
        return result;
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof Player player && !consumeReleaseGrace(stack, level)) {
            applyReleaseCooldown(player);
        }
        super.releaseUsing(stack, level, entity, timeLeft);
    }

    public void finishFailedGuard(ServerPlayer player, ItemStack stack) {
        if (!player.isUsingItem() || player.getUseItem() != stack) {
            return;
        }
        // 使用キーを押し続けていても次tickの自動再使用を拒否できるよう、停止通知より先に同期する。
        applyReleaseCooldown(player);
        player.stopUsingItem();
    }

    private void applyReleaseCooldown(Player player) {
        var ticks = ApprenticeCodexServerConfig.parrycastBucklerReleaseCooldownTicks();
        if (ticks > 0 && !player.getCooldowns().isOnCooldown(this)) {
            player.getCooldowns().addCooldown(this, ticks);
        }
    }

    public static synchronized void observeClientUseAnimation(ItemStack stack, LivingEntity living, boolean using, long gameTime) {
        var hand = resolveRenderedHand(stack, living);
        if (hand == null) {
            return;
        }
        var handStates = CLIENT_ANIMATION_OWNERS.computeIfAbsent(living, ignored -> new EnumMap<>(InteractionHand.class));
        var state = handStates.computeIfAbsent(hand,
                ignored -> new ClientAnimationState(nextClientAnimationInstanceId++, using, gameTime));
        CLIENT_ANIMATION_STATES.put(stack, state);
        if (state.using && !using) {
            state.removeStartTick = gameTime;
        }
        state.using = using;
        if (!using && gameTime - state.removeStartTick >= REMOVE_ANIMATION_TICKS) {
            state.removeStartTick = Long.MIN_VALUE;
        }
    }

    public static synchronized long resolveClientAnimationInstanceId(ItemStack stack) {
        var state = CLIENT_ANIMATION_STATES.get(stack);
        return state != null ? state.instanceId : Long.MIN_VALUE + Integer.toUnsignedLong(System.identityHashCode(stack));
    }

    private static synchronized int resolveClientAnimationState(ItemStack stack) {
        var state = CLIENT_ANIMATION_STATES.get(stack);
        if (state == null) {
            return 0;
        }
        if (state.using) {
            return 1;
        }
        return state.removeStartTick != Long.MIN_VALUE ? 2 : 0;
    }

    private static @Nullable InteractionHand resolveRenderedHand(ItemStack stack, LivingEntity living) {
        if (living.getMainHandItem() == stack) {
            return InteractionHand.MAIN_HAND;
        }
        if (living.getOffhandItem() == stack) {
            return InteractionHand.OFF_HAND;
        }
        return usingSameStack(living, stack) ? living.getUsedItemHand() : null;
    }

    private static boolean usingSameStack(LivingEntity living, ItemStack stack) {
        return living.isUsingItem() && living.getUseItem() == stack;
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null && spell != SpellRegistry.none()
                && (spell.getCastType() == CastType.INSTANT || spell.getCastType() == CastType.LONG)
                && spell.getRecastCount(spellLevel, null) <= 0;
    }

    public boolean canUseConfiguredSpell(ItemStack stack, @Nullable AbstractSpell spell, int spellLevel) {
        return spell != null && spell != SpellRegistry.none()
                && (spell.getCastType() == CastType.INSTANT || spell.getCastType() == CastType.LONG && hasSilverRing(stack))
                && spell.getRecastCount(spellLevel, null) <= 0;
    }

    @Override
    public @NotNull SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull SpellData spellData
    ) {
        if (slot != 0 || !canImbueSpell(spellData)) {
            return SpellCalibrationImbueState.REJECTED;
        }
        return SpellCalibrationImbueState.accepted(
                canUseConfiguredSpell(targetStack, spellData.getSpell(), spellData.getLevel())
        );
    }

    public List<Component> getImbueRestrictionTooltipLines(ItemStack stack) {
        return getImbueShieldRestrictionTooltipSection(stack);
    }

    @Override
    protected List<Component> getImbueShieldRestrictionTooltipSection(ItemStack stack) {
        var supportedCastTypes = hasSilverRing(stack)
                ? EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG)
                : EnumSet.of(SpellGunCastType.INSTANT);
        var lines = new ArrayList<>(ImbueTooltipHelper.collectCastTypeRestrictionLines(supportedCastTypes));
        ImbueTooltipHelper.appendNoRecastRestrictionLine(lines, true);
        return lines;
    }

    @Override
    protected List<Component> getImbueShieldAbilityTooltipSection(ItemStack stack) {
        var lines = new ArrayList<Component>();
        lines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_no_mana"));
        if (hasSilverRing(stack)) {
            lines.add(ImbueTooltipHelper.translatableGray(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"));
        }
        return lines;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) { return true; }

    @Override
    public int getEnchantmentValue(ItemStack stack) { return ENCHANTMENT_VALUE; }

    @Override
    public Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return DIRECT_ATTRIBUTE_ENCHANTMENTS;
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return repair.is(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get());
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment)
                || SHIELD_ENCHANTMENT_PROBE.supportsEnchantment(enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) return false;
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var equippedBase = AttributeEnchantmentResolver.resolveMergedModifiers(
                ImmutableMultimap.<Holder<Attribute>, AttributeModifier>of(), stack, "parrycast_buckler");
        var merged = MagicAttributeModifierHelper.mergeLinearMagicModifiers(
                equippedBase,
                "apprenticecodex.parrycast_buckler.merged"
        );
        var result = ItemAttributeModifiers.builder();
        for (var entry : merged.entries()) {
            result.add(entry.getKey(), entry.getValue(), net.minecraft.world.entity.EquipmentSlotGroup.OFFHAND);
        }
        return result.build();
    }

    public boolean handlePerfectGuard(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (!isPerfectGuard(player)) return false;
        rememberReleaseGrace(stack, player.level().getGameTime());
        if (!player.getPersistentData().getBoolean(SESSION_TRIGGERED_TAG)) {
            player.getPersistentData().putBoolean(SESSION_TRIGGERED_TAG, true);
            tryCastOrReduceCooldown(player, stack, hand);
        }
        return true;
    }

    public static boolean isPerfectGuard(Player player) {
        var tag = player.getPersistentData();
        return tag.contains(USE_START_TICK_TAG)
                && player.level().getGameTime() - tag.getLong(USE_START_TICK_TAG)
                <= ApprenticeCodexServerConfig.parrycastBucklerPerfectGuardTicks();
    }

    public static int resolveDurabilityCost(float damage, boolean perfectGuard) {
        if (damage < 3.0F) return 0;
        int vanilla = 1 + Mth.floor(damage);
        return perfectGuard && vanilla >= 2 ? 1 : vanilla;
    }

    public static boolean isDurabilitySuppressed(ItemStack stack, long gameTime) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        var tag = customData == null ? null : customData.copyTag();
        return tag != null && tag.contains(LAST_DURABILITY_TICK_TAG, Tag.TAG_LONG)
                && gameTime - tag.getLong(LAST_DURABILITY_TICK_TAG)
                <= ApprenticeCodexServerConfig.parrycastBucklerPerfectGuardTicks();
    }

    public static void rememberDurabilityConsumed(ItemStack stack, long gameTime) {
        if (!stack.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(LAST_DURABILITY_TICK_TAG, gameTime));
        }
    }

    private void tryCastOrReduceCooldown(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) return;
        var selected = hasWisdomShard(stack) ? new SpellSelectionManager(player).getSelection() : null;
        SpellData spellData = selected != null ? selected.spellData : getPrimarySpellData(stack);
        CastSource castSource = selected != null ? selected.getCastSource() : CastSource.SWORD;
        if (spellData == null || spellData == SpellData.EMPTY) return;
        var spell = spellData.getSpell();
        int level = spell.getLevelFor(spellData.getLevel(), player);
        if (!canUseConfiguredSpell(stack, spell, level)) {
            player.displayClientMessage(Component.translatable(
                    "ui.apprenticecodex.parrycast.cannot_cast", spell.getDisplayName(player)), true);
            return;
        }
        var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
        if (cooldown != null) {
            if (hasWisdomShard(stack)) {
                reduceAllCooldowns(player, magicData);
            } else {
                reduceCooldown(player, magicData, cooldown);
            }
            return;
        }
        float borrowed = Math.max(0F, spell.getManaCost(level) - magicData.getMana());
        if (borrowed > 0F) magicData.addMana(borrowed);
        String slot = hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
        int remainingUseTicks = player.getUseItemRemainingTicks();
        boolean casted = spell.attemptInitiateCast(stack, level, player.level(), player, castSource, true, slot);
        if (!casted) {
            if (borrowed > 0F) magicData.setMana(Math.max(0F, magicData.getMana() - borrowed));
            return;
        }
        if (borrowed > 0F) ItemManaBypassCastEvent.reserveBorrowedMana(player, borrowed);
        if (spell.getCastType() == CastType.INSTANT) {
            // attemptInitiateCast は盾使用を止めるため、INSTANT をその場で完了してから構えを復元する。
            spell.castSpell(player.level(), level, player, magicData.getCastSource(), true);
            spell.onServerCastComplete(player.level(), level, player, magicData, false);
        } else {
            TriggeredSpellCastHelper.applyLongCastDurationOverride(player, level, spell, magicData, slot, 0);
        }
        if (player.getItemInHand(hand) == stack && !stack.isEmpty()) {
            player.startUsingItem(hand);
            ((LivingEntityAccessor) player).apprenticecodex$setUseItemRemaining(remainingUseTicks);
        }
    }

    private static void reduceAllCooldowns(ServerPlayer player, MagicData magicData) {
        for (var entry : magicData.getPlayerCooldowns().getSpellCooldowns().entrySet()) {
            reduceCooldown(player, magicData, entry.getValue());
        }
    }

    private static void reduceCooldown(ServerPlayer player, MagicData magicData, CooldownInstance cooldown) {
        int maximum = cooldown.getSpellCooldown();
        int reduction = resolveCooldownReductionTicks(maximum, cooldown.getCooldownRemaining());
        cooldown.decrementBy(Math.max(1, reduction));
        magicData.getPlayerCooldowns().syncToPlayer(player);
    }

    public static int resolveCooldownReductionTicks(int maximumCooldownTicks, int remainingCooldownTicks) {
        return maximumCooldownTicks > 0
                ? Mth.ceil(maximumCooldownTicks * 0.1D)
                : Mth.ceil(Math.max(0, remainingCooldownTicks) * 0.2D);
    }

    private static void rememberReleaseGrace(ItemStack stack, long gameTime) {
        int ticks = ApprenticeCodexServerConfig.parrycastBucklerPerfectGuardReleaseCooldownGraceTicks();
        int uses = ApprenticeCodexServerConfig.parrycastBucklerPerfectGuardReleaseCooldownGraceUses();
        if (ticks <= 0 || uses <= 0) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(GRACE_TICK_TAG, gameTime);
            tag.putInt(GRACE_USES_TAG, uses);
        });
    }

    private static boolean consumeReleaseGrace(ItemStack stack, Level level) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        var tag = customData == null ? null : customData.copyTag();
        if (tag == null || !tag.contains(GRACE_TICK_TAG) || !tag.contains(GRACE_USES_TAG)) return false;
        long elapsed = level.getGameTime() - tag.getLong(GRACE_TICK_TAG);
        int uses = tag.getInt(GRACE_USES_TAG);
        if (elapsed < 0 || elapsed > ApprenticeCodexServerConfig.parrycastBucklerPerfectGuardReleaseCooldownGraceTicks() || uses <= 0) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
                data.remove(GRACE_TICK_TAG);
                data.remove(GRACE_USES_TAG);
            });
            return false;
        }
        int remainingUses = uses - 1;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            if (remainingUses <= 0) {
                data.remove(GRACE_TICK_TAG);
                data.remove(GRACE_USES_TAG);
            } else {
                data.putInt(GRACE_USES_TAG, remainingUses);
            }
        });
        return true;
    }

    private static ItemStack readCalibrationAdjustment(ItemStack stack, int slot) {
        return ShieldCalibrationData.get(stack, CALIBRATION_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    private static void writeCalibrationAdjustment(ItemStack stack, int slot, ItemStack adjustment) {
        ShieldCalibrationData.set(stack, CALIBRATION_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, adjustment);
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

    public static boolean hasSilverRing(ItemStack stack) { return hasAdjustment(stack, MithrilFreecastStaff::isSilverRing); }
    public static boolean hasWisdomShard(ItemStack stack) { return hasAdjustment(stack, s -> s.is(ItemRegistry.WISDOM_SHARD.get())); }
    private static boolean hasAdjustment(ItemStack stack, java.util.function.Predicate<ItemStack> predicate) {
        for (int i = 0; i < CALIBRATION_ADJUSTMENT_SLOT_COUNT; i++) if (predicate.test(readCalibrationAdjustment(stack, i))) return true;
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 0, state -> {
            var stack = state.getData(DataTickets.ITEMSTACK);
            int animationState = stack == null ? 0 : resolveClientAnimationState(stack);
            state.setAnimation(animationState == 1 ? DEPLOY : animationState == 2 ? REMOVE_IDLE : IDLE);
            return PlayState.CONTINUE;
        }));
    }

    private static final class ClientAnimationState {
        private final long instanceId;
        private boolean using;
        private long removeStartTick;

        private ClientAnimationState(long instanceId, boolean using, long gameTime) {
            this.instanceId = instanceId;
            this.using = using;
            this.removeStartTick = using ? Long.MIN_VALUE : gameTime - REMOVE_ANIMATION_TICKS;
        }
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
