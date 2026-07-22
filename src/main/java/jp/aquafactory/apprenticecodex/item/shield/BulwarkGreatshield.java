package jp.aquafactory.apprenticecodex.item.shield;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class BulwarkGreatshield extends AbstractImbueShieldItem
        implements GeoItem, IJeiInfoItem, SpellCalibrationAdjustmentTarget, AttributeEnchantmentPolicy {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.bulwark_greatshield.desc_";

    public static final int DURABILITY = 2031;
    public static final int ENCHANTMENT_VALUE = 15;
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            CalibrationAdjustmentProfile.of(
                    CalibrationAdjustmentRule.repeatable(
                            ScrollcasterSchoolRuneResolver::isSchoolRune,
                            CalibrationAdjustmentHints.schoolRunes()
                    ),
                    CalibrationAdjustmentRule.unique(
                            BulwarkGreatshield::isWisdomShard,
                            CalibrationAdjustmentHints.wisdomShard()
                    )
            );
    public static final int DURABILITY_SUPPRESSION_TICKS = 20;
    public static final int CONTINUOUS_CAST_DELAY_TICKS = 20;
    public static final int MANA_RECOVERY_COOLDOWN_TICKS = 20;

    private static final String CALIBRATION_TAG = "BulwarkGreatshieldCalibration";
    private static final ResourceLocation GENERIC_RESIST_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "bulwark_greatshield/generic_spell_resist");
    private static final ResourceLocation SCHOOL_RESIST_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "bulwark_greatshield/school_spell_resist");
    private static final ItemStack SHIELD_ENCHANTMENT_PROBE = new ItemStack(Items.SHIELD);
    private static final Set<AttributeEnchantmentType> DIRECT_ATTRIBUTE_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.REFLUX,
            AttributeEnchantmentType.RESERVOIR
    );
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BulwarkGreatshield() {
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
                "item." + ApprenticeCodex.MODID + ".bulwark_greatshield.desc"));
        var castTooltip = hasWisdomShardAdjustment(stack) ? "cast_wisdom" : "cast_default";
        lines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".bulwark_greatshield." + castTooltip));
    }

    @Override
    protected boolean shouldPrimeImmediateShieldBlock() {
        return false;
    }

    @Override
    public boolean supportsBlockTriggeredImbuedSpell() {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        var result = super.use(level, player, hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && result.getResult().consumesAction()) {
            BulwarkGreatshieldRuntime.beginUse(serverPlayer);
        }
        return result;
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null && spell.getCastType() == CastType.CONTINUOUS;
    }

    @Override
    public boolean supportsManaBypass(@Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    protected List<Component> getImbueShieldAbilityTooltipSection() {
        return List.of(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_hold_continuous"));
    }

    @Override
    protected List<Component> getImbueShieldRestrictionTooltipSection() {
        return List.of(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_continuous_only"));
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player
                && getUseDuration(stack, livingEntity) - remainingUseDuration >= CONTINUOUS_CAST_DELAY_TICKS) {
            BulwarkGreatshieldRuntime.tryStartContinuousCast(player, stack, player.getUsedItemHand());
            BulwarkGreatshieldRuntime.tickContinuousCast(player, stack);
        }
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            BulwarkGreatshieldRuntime.finishUse(player);
        }
        super.releaseUsing(stack, level, livingEntity, timeLeft);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var builder = ItemAttributeModifiers.builder();
        builder.add(AttributeRegistry.SPELL_RESIST, new AttributeModifier(
                GENERIC_RESIST_MODIFIER_ID,
                ApprenticeCodexServerConfig.bulwarkGreatshieldGenericSpellResist(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ), net.minecraft.world.entity.EquipmentSlotGroup.OFFHAND);
        var schoolRuneCounts = new LinkedHashMap<SchoolType, Integer>();
        for (var school : getResolvedCalibrationSchools(stack)) {
            schoolRuneCounts.merge(school, 1, Integer::sum);
        }
        var schoolSpellResist = ApprenticeCodexServerConfig.bulwarkGreatshieldSchoolSpellResist();
        for (var entry : schoolRuneCounts.entrySet()) {
            var school = entry.getKey();
            var schoolResist = MagicTools.resolveSchoolResistAttribute(school);
            if (schoolResist != null) {
                builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(schoolResist), new AttributeModifier(
                        SCHOOL_RESIST_MODIFIER_ID,
                        schoolSpellResist * entry.getValue(),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ), net.minecraft.world.entity.EquipmentSlotGroup.OFFHAND);
            }
        }
        AttributeEnchantmentResolver.addModifiers(
                builder,
                stack,
                net.minecraft.world.entity.EquipmentSlotGroup.OFFHAND,
                "bulwark_greatshield_offhand_enchant"
        );
        return builder.build();
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

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
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    private static @NotNull ItemStack readCalibrationAdjustment(@NotNull ItemStack shieldStack, int slot) {
        if (!isValidCalibrationAccess(shieldStack, slot)) {
            return ItemStack.EMPTY;
        }
        return ShieldCalibrationData.get(shieldStack, CALIBRATION_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    private static void writeCalibrationAdjustment(@NotNull ItemStack shieldStack, int slot, @NotNull ItemStack adjustment) {
        if (!isValidCalibrationAccess(shieldStack, slot)) {
            return;
        }
        ShieldCalibrationData.set(shieldStack, CALIBRATION_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, adjustment);
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

    public static boolean isWisdomShard(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemRegistry.WISDOM_SHARD.get());
    }

    public static boolean hasWisdomShardAdjustment(ItemStack stack) {
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isWisdomShard(readCalibrationAdjustment(stack, slot))) {
                return true;
            }
        }
        return false;
    }

    public static @NotNull List<SchoolType> getResolvedCalibrationSchools(ItemStack stack) {
        var schools = new ArrayList<SchoolType>();
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            ScrollcasterSchoolRuneResolver.resolveSchool(readCalibrationAdjustment(stack, slot))
                    .ifPresent(schools::add);
        }
        return List.copyOf(schools);
    }

    public static @Nullable SchoolType getResolvedCalibrationSchool(ItemStack stack) {
        var schools = getResolvedCalibrationSchools(stack);
        return schools.isEmpty() ? null : schools.get(0);
    }

    @Nullable
    public static SpellData resolveCastSpell(ServerPlayer player, ItemStack stack) {
        if (!hasWisdomShardAdjustment(stack)) {
            return ((BulwarkGreatshield) stack.getItem()).getPrimarySpellData(stack);
        }
        var selection = new SpellSelectionManager(player).getSelection();
        return selection == null || selection.spellData == SpellData.EMPTY ? null : selection.spellData;
    }

    public static CastSource resolveCastSource(ServerPlayer player, ItemStack stack) {
        if (hasWisdomShardAdjustment(stack)) {
            var selection = new SpellSelectionManager(player).getSelection();
            if (selection != null) {
                return selection.getCastSource();
            }
        }
        return CastSource.SWORD;
    }

    public static void recoverManaAfterBlock(ServerPlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        var maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        MagicTools.recoverManaSafely(player, magicData, (float) (maxMana * 0.1D));
    }

    private static boolean isValidCalibrationAccess(ItemStack stack, int slot) {
        return slot >= 0 && slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT
                && !stack.isEmpty() && stack.getItem() instanceof BulwarkGreatshield;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
