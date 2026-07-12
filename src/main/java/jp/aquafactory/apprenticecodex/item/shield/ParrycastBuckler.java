package jp.aquafactory.apprenticecodex.item.shield;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.CooldownInstance;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.OffhandMagicModifierHelper;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.ParrycastBucklerRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class ParrycastBuckler extends AbstractImbueShieldItem implements GeoItem {
    public static final int DURABILITY = 1561;
    public static final int ENCHANTMENT_VALUE = 22;
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    private static final double SCHOOL_POWER_BONUS = 0.1D;
    private static final String CALIBRATION_TAG = "ParrycastBucklerCalibration";
    private static final String ADJUSTMENTS_TAG = "Adjustments";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String USE_START_TICK_TAG = "ApprenticeCodexParrycastBucklerUseStart";
    private static final String SESSION_TRIGGERED_TAG = "ApprenticeCodexParrycastBucklerTriggered";
    private static final String LAST_DURABILITY_TICK_TAG = "ApprenticeCodexParrycastBucklerDurabilityTick";
    private static final String GRACE_TICK_TAG = "ApprenticeCodexParrycastBucklerGraceTick";
    private static final String GRACE_USES_TAG = "ApprenticeCodexParrycastBucklerGraceUses";
    private static final String ANIMATION_STATE_TAG = "ApprenticeCodexParrycastBucklerAnimationState";
    private static final String ANIMATION_REMOVE_START_TAG = "ApprenticeCodexParrycastBucklerRemoveStart";
    private static final ItemStack SHIELD_ENCHANTMENT_PROBE = new ItemStack(Items.SHIELD);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DEPLOY = RawAnimation.begin().thenPlayAndHold("deploy");
    private static final RawAnimation REMOVE_IDLE = RawAnimation.begin().thenPlay("remove").thenLoop("idle");
    private static final UUID[] SCHOOL_POWER_IDS = {
            UUID.fromString("23da0bec-c636-4454-9802-866f841b0a50"),
            UUID.fromString("64de25e1-e97c-49d2-b522-58122aac8470"),
            UUID.fromString("f4501063-c5a5-45d6-bdf5-dd67fde71768")
    };
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ParrycastBuckler() {
        super(new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.RARE));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        var result = super.use(level, player, hand);
        if (result.getResult().consumesAction()) {
            var stack = player.getItemInHand(hand);
            stack.getOrCreateTag().putInt(ANIMATION_STATE_TAG, 1);
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
        stack.getOrCreateTag().putInt(ANIMATION_STATE_TAG, 2);
        stack.getOrCreateTag().putLong(ANIMATION_REMOVE_START_TAG, level.getGameTime());
        if (!level.isClientSide && entity instanceof Player player && !consumeReleaseGrace(stack, level)) {
            var ticks = ApprenticeCodexServerConfig.parrycastBucklerReleaseCooldownTicks();
            if (ticks > 0 && !player.getCooldowns().isOnCooldown(this)) {
                player.getCooldowns().addCooldown(this, ticks);
            }
        }
        super.releaseUsing(stack, level, entity, timeLeft);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull net.minecraft.world.entity.Entity entity,
                              int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        var tag = stack.getTag();
        if (tag != null && tag.getInt(ANIMATION_STATE_TAG) == 2
                && level.getGameTime() - tag.getLong(ANIMATION_REMOVE_START_TAG) >= 10) {
            tag.putInt(ANIMATION_STATE_TAG, 0);
            tag.remove(ANIMATION_REMOVE_START_TAG);
        }
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

    public boolean isMismatchedCastConditionAt(ItemStack targetStack, int slot) {
        if (slot != 0) {
            return false;
        }
        var spellData = getPrimarySpellData(targetStack);
        return spellData != SpellData.EMPTY
                && spellData != null
                && spellData.getSpell() != null
                && !canUseConfiguredSpell(targetStack, spellData.getSpell(), spellData.getLevel());
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
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(SHIELD_ENCHANTMENT_PROBE)
                || enchantment == EnchantmentRegistry.TENSE.get()
                || enchantment == EnchantmentRegistry.ALACRITY.get()
                || enchantment == EnchantmentRegistry.TRANSCENDENCE.get()
                || enchantment == EnchantmentRegistry.WISDOM.get();
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) return false;
        var enchantments = EnchantmentHelper.getEnchantments(book);
        return enchantments.isEmpty() || enchantments.keySet().stream().allMatch(e -> canApplyAtEnchantingTable(stack, e));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var base = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.OFFHAND) return base;
        var equippedBase = OffhandMagicModifierHelper.buildEquippedModifiers(base, stack, "parrycast_buckler");
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder().putAll(equippedBase);
        Set<net.minecraft.resources.ResourceLocation> seen = new HashSet<>();
        for (int i = 0; i < CALIBRATION_ADJUSTMENT_SLOT_COUNT; i++) {
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(getCalibrationAdjustment(stack, i)).orElse(null);
            if (school == null || !seen.add(school.getId())) continue;
            var attribute = MagicTools.resolveSchoolPowerAttribute(school);
            if (attribute != null) {
                builder.put(attribute, new AttributeModifier(SCHOOL_POWER_IDS[i],
                        "Parrycast buckler school spell power", SCHOOL_POWER_BONUS, AttributeModifier.Operation.MULTIPLY_BASE));
            }
        }
        return builder.build();
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
        var tag = stack.getTag();
        return tag != null && tag.contains(LAST_DURABILITY_TICK_TAG)
                && gameTime - tag.getLong(LAST_DURABILITY_TICK_TAG)
                <= ApprenticeCodexServerConfig.parrycastBucklerPerfectGuardTicks();
    }

    public static void rememberDurabilityConsumed(ItemStack stack, long gameTime) {
        if (!stack.isEmpty()) stack.getOrCreateTag().putLong(LAST_DURABILITY_TICK_TAG, gameTime);
    }

    private void tryCastOrReduceCooldown(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) return;
        var selected = hasWisdomShard(stack) ? new SpellSelectionManager(player).getSelection() : null;
        SpellData spellData = selected != null ? selected.spellData : getPrimarySpellData(stack);
        CastSource castSource = selected != null ? selected.getCastSource() : CastSource.SWORD;
        if (hasWisdomShard(stack)) reduceAllCooldowns(player, magicData);
        if (spellData == null || spellData == SpellData.EMPTY) return;
        var spell = spellData.getSpell();
        int level = spell.getLevelFor(spellData.getLevel(), player);
        if (!canUseConfiguredSpell(stack, spell, level)) {
            player.displayClientMessage(Component.translatable(
                    "ui.apprenticecodex.parrycast.cannot_long_cast", spell.getDisplayName(player)), true);
            return;
        }
        var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
        if (cooldown != null) {
            if (!hasWisdomShard(stack)) reduceCooldown(player, magicData, cooldown);
            return;
        }
        float borrowed = Math.max(0F, spell.getManaCost(level) - magicData.getMana());
        if (borrowed > 0F) magicData.addMana(borrowed);
        String slot = hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
        boolean casted = spell.attemptInitiateCast(stack, level, player.level(), player, castSource, true, slot);
        if (!casted) {
            if (borrowed > 0F) magicData.setMana(Math.max(0F, magicData.getMana() - borrowed));
            return;
        }
        if (borrowed > 0F) ItemManaBypassCastEvent.reserveBorrowedMana(player, borrowed);
        TriggeredSpellCastHelper.applyLongCastDurationOverride(player, level, spell, magicData, slot, 0);
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
        stack.getOrCreateTag().putLong(GRACE_TICK_TAG, gameTime);
        stack.getOrCreateTag().putInt(GRACE_USES_TAG, uses);
    }

    private static boolean consumeReleaseGrace(ItemStack stack, Level level) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(GRACE_TICK_TAG) || !tag.contains(GRACE_USES_TAG)) return false;
        long elapsed = level.getGameTime() - tag.getLong(GRACE_TICK_TAG);
        int uses = tag.getInt(GRACE_USES_TAG);
        if (elapsed < 0 || elapsed > ApprenticeCodexServerConfig.parrycastBucklerPerfectGuardReleaseCooldownGraceTicks() || uses <= 0) {
            tag.remove(GRACE_TICK_TAG); tag.remove(GRACE_USES_TAG); return false;
        }
        if (--uses <= 0) { tag.remove(GRACE_TICK_TAG); tag.remove(GRACE_USES_TAG); }
        else tag.putInt(GRACE_USES_TAG, uses);
        return true;
    }

    public static ItemStack getCalibrationAdjustment(ItemStack stack, int slot) {
        if (slot < 0 || slot >= CALIBRATION_ADJUSTMENT_SLOT_COUNT) return ItemStack.EMPTY;
        var calibration = stack.getTagElement(CALIBRATION_TAG);
        if (calibration == null) return ItemStack.EMPTY;
        var list = calibration.getList(ADJUSTMENTS_TAG, Tag.TAG_COMPOUND);
        for (Tag value : list) {
            var entry = (CompoundTag) value;
            if (entry.getInt(SLOT_TAG) == slot) return ItemStack.of(entry.getCompound(ITEM_TAG));
        }
        return ItemStack.EMPTY;
    }

    public static void setCalibrationAdjustment(ItemStack stack, int slot, ItemStack adjustment) {
        if (slot < 0 || slot >= CALIBRATION_ADJUSTMENT_SLOT_COUNT) return;
        var calibration = stack.getOrCreateTagElement(CALIBRATION_TAG);
        var old = calibration.getList(ADJUSTMENTS_TAG, Tag.TAG_COMPOUND);
        var replacement = new ListTag();
        for (Tag value : old) if (((CompoundTag) value).getInt(SLOT_TAG) != slot) replacement.add(value.copy());
        if (!adjustment.isEmpty()) {
            var entry = new CompoundTag(); entry.putInt(SLOT_TAG, slot);
            var stored = adjustment.copy(); stored.setCount(1); entry.put(ITEM_TAG, stored.save(new CompoundTag()));
            replacement.add(entry);
        }
        if (replacement.isEmpty()) stack.removeTagKey(CALIBRATION_TAG); else calibration.put(ADJUSTMENTS_TAG, replacement);
    }

    public static boolean isCalibrationAdjustmentItem(ItemStack stack) {
        return ScrollcasterSchoolRuneResolver.isSchoolRune(stack) || MithrilFreecastStaff.isSilverRing(stack)
                || stack.is(ItemRegistry.WISDOM_SHARD.get());
    }

    public static boolean hasSilverRing(ItemStack stack) { return hasAdjustment(stack, MithrilFreecastStaff::isSilverRing); }
    public static boolean hasWisdomShard(ItemStack stack) { return hasAdjustment(stack, s -> s.is(ItemRegistry.WISDOM_SHARD.get())); }
    private static boolean hasAdjustment(ItemStack stack, java.util.function.Predicate<ItemStack> predicate) {
        for (int i = 0; i < CALIBRATION_ADJUSTMENT_SLOT_COUNT; i++) if (predicate.test(getCalibrationAdjustment(stack, i))) return true;
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ParrycastBucklerRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ParrycastBucklerRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 0, state -> {
            var stack = state.getData(DataTickets.ITEMSTACK);
            int animationState = stack == null ? 0 : stack.getOrCreateTag().getInt(ANIMATION_STATE_TAG);
            state.setAnimation(animationState == 1 ? DEPLOY : animationState == 2 ? REMOVE_IDLE : IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
