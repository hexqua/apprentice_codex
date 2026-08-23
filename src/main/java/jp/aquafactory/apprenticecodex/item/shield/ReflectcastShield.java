package jp.aquafactory.apprenticecodex.item.shield;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.ReflectcastShieldRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

public class ReflectcastShield extends AbstractImbueShieldItem
        implements GeoItem, SpellCalibrationAdjustmentTarget {

    public static final int DURABILITY = 1561;
    public static final int DURABILITY_SUPPRESSION_TICKS = 10;
    public static final int SPELL_TRIGGER_SUPPRESSION_TICKS = 10;
    public static final int ENCHANTMENT_VALUE = 22;
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            CalibrationAdjustmentProfile.of(
                    CalibrationAdjustmentRule.unique(
                            "silver_ring",
                            MithrilFreecastStaff::isSilverRing,
                            CalibrationAdjustmentHints.silverRing()
                    ).withEffectLines(CalibrationAdjustmentEffects.addAllSupport()),
                    CalibrationAdjustmentRule.unique(
                            "wisdom_shard",
                            stack -> stack.is(ItemRegistry.WISDOM_SHARD.get()),
                            CalibrationAdjustmentHints.wisdomShard()
                    ).withEffectLines(CalibrationAdjustmentEffects.changeImbueToSelected())
            );
    private static final float MINIMUM_DURABILITY_DAMAGE = 3.0F;
    private static final String CALIBRATION_TAG = "ReflectcastShieldCalibration";
    private static final String ADJUSTMENTS_TAG = "Adjustments";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final ItemStack SHIELD_ENCHANTMENT_PROBE = new ItemStack(Items.SHIELD);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ReflectcastShield() {
        super(new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.UNCOMMON));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    protected void appendAlwaysVisibleImbueTooltip(ItemStack stack, List<Component> lines) {
        var castTooltip = hasWisdomShard(stack) ? "cast_wisdom" : "cast_default";
        lines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".reflectcast_shield." + castTooltip));
    }

    @Override
    protected boolean shouldPrimeImmediateShieldBlock() {
        return false;
    }

    @Override
    public boolean supportsBlockTriggeredImbuedSpell() {
        // 専用イベントが非永続ランタイムを使うため、基底クラスの persistent NBT 発動窓は作らない。
        return false;
    }

    @Override
    public boolean supportsManaBypass(@Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null && spell != SpellRegistry.none()
                && (spell.getCastType() == CastType.INSTANT
                || spell.getCastType() == CastType.LONG
                || spell.getCastType() == CastType.CONTINUOUS)
                && spell.getRecastCount(spellLevel, null) <= 0;
    }

    public boolean canUseConfiguredSpell(ItemStack stack, @Nullable AbstractSpell spell, int spellLevel) {
        if (spell == null || spell == SpellRegistry.none() || spell.getRecastCount(spellLevel, null) > 0) {
            return false;
        }
        return spell.getCastType() == CastType.INSTANT
                || hasSilverRing(stack) && (spell.getCastType() == CastType.LONG
                || spell.getCastType() == CastType.CONTINUOUS);
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
    public List<Component> getImbueRestrictionTooltipLines() {
        return getImbueShieldRestrictionTooltipSection(ItemStack.EMPTY);
    }

    @Override
    protected List<Component> getImbueShieldRestrictionTooltipSection(ItemStack stack) {
        var lines = hasSilverRing(stack)
                ? new ArrayList<Component>()
                : new ArrayList<>(ImbueTooltipHelper.collectCastTypeRestrictionLines(
                EnumSet.of(SpellGunCastType.INSTANT)
        ));
        ImbueTooltipHelper.appendNoRecastRestrictionLine(lines, true);
        return lines;
    }

    @Override
    protected List<Component> getImbueShieldAbilityTooltipSection(ItemStack stack) {
        if (!hasSilverRing(stack)) {
            return List.of(ImbueTooltipHelper.translatableGray(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"));
        }
        return List.of(
                ImbueTooltipHelper.translatableGray(
                        "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"),
                ImbueTooltipHelper.translatableGray(
                        "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_hold_continuous"),
                ImbueTooltipHelper.translatableGray(
                        "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown")
        );
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            ReflectcastShieldRuntime.tickContinuousCast(player, stack);
        }
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            ReflectcastShieldRuntime.finishUse(player);
        }
        super.releaseUsing(stack, level, livingEntity, timeLeft);
    }

    public static int resolveBlockedDurabilityCost(float originalBlockedDamage) {
        if (originalBlockedDamage < MINIMUM_DURABILITY_DAMAGE) {
            return 0;
        }

        return 1 + Mth.floor(originalBlockedDamage);
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return repair.is(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(SHIELD_ENCHANTMENT_PROBE)
                || EnchantmentRegistry.TRANSCENDENCE.isPresent() && enchantment == EnchantmentRegistry.TRANSCENDENCE.get()
                || EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get();
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

    private static @NotNull ItemStack readCalibrationAdjustment(@NotNull ItemStack shieldStack, int slot) {
        return CalibrationAdjustmentStorage.get(shieldStack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    @Override
    public @NotNull java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(
            @NotNull ItemStack stack
    ) {
        return createCalibrationAdjustmentTooltip(stack);
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_PROFILE;
    }

    public static boolean hasSilverRing(ItemStack stack) {
        return hasAdjustment(stack, MithrilFreecastStaff::isSilverRing);
    }

    public static boolean hasWisdomShard(ItemStack stack) {
        return hasAdjustment(stack, adjustment -> adjustment.is(ItemRegistry.WISDOM_SHARD.get()));
    }

    @Nullable
    public static SpellData resolveCastSpell(ServerPlayer player, ItemStack stack) {
        if (!hasWisdomShard(stack)) {
            return ((ReflectcastShield) stack.getItem()).getPrimarySpellData(stack);
        }
        var selection = new SpellSelectionManager(player).getSelection();
        return selection == null || selection.spellData == SpellData.EMPTY ? null : selection.spellData;
    }

    public static CastSource resolveCastSource(ServerPlayer player, ItemStack stack) {
        if (hasWisdomShard(stack)) {
            var selection = new SpellSelectionManager(player).getSelection();
            if (selection != null) {
                return selection.getCastSource();
            }
        }
        return CastSource.SWORD;
    }

    private static boolean hasAdjustment(ItemStack stack, java.util.function.Predicate<ItemStack> predicate) {
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; slot++) {
            if (predicate.test(readCalibrationAdjustment(stack, slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidCalibrationAccess(ItemStack stack, int slot) {
        return slot >= 0 && slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT
                && !stack.isEmpty() && stack.getItem() instanceof ReflectcastShield;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ReflectcastShieldRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ReflectcastShieldRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
