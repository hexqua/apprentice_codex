package jp.aquafactory.apprenticecodex.item.shield;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.BulwarkGreatshieldRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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

import java.util.UUID;
import java.util.List;
import java.util.function.Consumer;

public class BulwarkGreatshield extends AbstractImbueShieldItem implements GeoItem, IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.bulwark_greatshield.desc_";

    public static final int DURABILITY = 2031;
    public static final int ENCHANTMENT_VALUE = 15;
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 1;
    public static final int DURABILITY_SUPPRESSION_TICKS = 20;
    public static final int CONTINUOUS_CAST_DELAY_TICKS = 20;
    public static final int MANA_RECOVERY_COOLDOWN_TICKS = 20;
    public static final double GENERIC_SPELL_RESIST = 0.25D;
    public static final double SCHOOL_SPELL_RESIST = 0.5D;

    private static final String CALIBRATION_TAG = "BulwarkGreatshieldCalibration";
    private static final String ADJUSTMENT_TAG = "Adjustment";
    private static final UUID GENERIC_RESIST_MODIFIER_ID = UUID.fromString("bd2b8a1f-b1a5-49ee-9370-4d2ab9385994");
    private static final UUID SCHOOL_RESIST_MODIFIER_ID = UUID.fromString("886ced13-4a4f-4623-9cbb-8900f65c52ac");
    private static final ItemStack SHIELD_ENCHANTMENT_PROBE = new ItemStack(Items.SHIELD);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BulwarkGreatshield() {
        super(new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.RARE));
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
                && getUseDuration(stack) - remainingUseDuration >= CONTINUOUS_CAST_DELAY_TICKS) {
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var base = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.OFFHAND) {
            return base;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder().putAll(base);
        builder.put(AttributeRegistry.SPELL_RESIST.get(), new AttributeModifier(
                GENERIC_RESIST_MODIFIER_ID,
                "Bulwark greatshield spell resist",
                GENERIC_SPELL_RESIST,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));
        var schoolResist = MagicTools.resolveSchoolResistAttribute(getResolvedCalibrationSchool(stack));
        if (schoolResist != null) {
            builder.put(schoolResist, new AttributeModifier(
                    SCHOOL_RESIST_MODIFIER_ID,
                    "Bulwark greatshield school spell resist",
                    SCHOOL_SPELL_RESIST,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ));
        }
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
        return enchantments.isEmpty() || enchantments.keySet().stream().allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    public static @NotNull ItemStack getCalibrationAdjustment(@NotNull ItemStack shieldStack, int slot) {
        if (!isValidCalibrationAccess(shieldStack, slot)) {
            return ItemStack.EMPTY;
        }
        var calibration = shieldStack.getTagElement(CALIBRATION_TAG);
        return calibration != null && calibration.contains(ADJUSTMENT_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.of(calibration.getCompound(ADJUSTMENT_TAG))
                : ItemStack.EMPTY;
    }

    public static void setCalibrationAdjustment(@NotNull ItemStack shieldStack, int slot, @NotNull ItemStack adjustment) {
        if (!isValidCalibrationAccess(shieldStack, slot)) {
            return;
        }
        if (adjustment.isEmpty()) {
            var calibration = shieldStack.getTagElement(CALIBRATION_TAG);
            if (calibration != null) {
                calibration.remove(ADJUSTMENT_TAG);
                if (calibration.isEmpty()) {
                    shieldStack.removeTagKey(CALIBRATION_TAG);
                }
            }
            return;
        }
        var stored = adjustment.copy();
        stored.setCount(1);
        shieldStack.getOrCreateTagElement(CALIBRATION_TAG).put(ADJUSTMENT_TAG, stored.save(new CompoundTag()));
    }

    public static boolean isCalibrationAdjustmentItem(@NotNull ItemStack stack) {
        return ScrollcasterSchoolRuneResolver.isSchoolRune(stack) || isWisdomShard(stack);
    }

    public static boolean isWisdomShard(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemRegistry.WISDOM_SHARD.get());
    }

    public static boolean hasWisdomShardAdjustment(ItemStack stack) {
        return isWisdomShard(getCalibrationAdjustment(stack, 0));
    }

    public static @Nullable io.redspace.ironsspellbooks.api.spells.SchoolType getResolvedCalibrationSchool(ItemStack stack) {
        return ScrollcasterSchoolRuneResolver.resolveSchool(getCalibrationAdjustment(stack, 0)).orElse(null);
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
        var maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA.get());
        MagicTools.recoverManaSafely(player, magicData, (float) (maxMana * 0.1D));
    }

    private static boolean isValidCalibrationAccess(ItemStack stack, int slot) {
        return slot == 0 && !stack.isEmpty() && stack.getItem() instanceof BulwarkGreatshield;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BulwarkGreatshieldRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new BulwarkGreatshieldRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
