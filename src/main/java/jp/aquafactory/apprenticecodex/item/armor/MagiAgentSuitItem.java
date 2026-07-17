package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.renderer.armor.MagiAgentSuitRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class MagiAgentSuitItem extends ArmorItem
        implements GeoItem, IPresetSpellContainer, SpellCalibrationAdjustmentTarget, WisdomPolicy {
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 1;
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            CalibrationAdjustmentProfile.of(
                    CalibrationAdjustmentRule.unique(
                            ScrollcasterSchoolRuneResolver::isSchoolRune,
                            CalibrationAdjustmentHints.schoolRunes()
                    )
            );

    private static final String CALIBRATION_TAG = "MagiAgentSuitCalibration";
    private static final String ADJUSTMENT_ITEM_TAG = "AdjustmentItem";
    private static final String RUNE_HINT_KEY = "item.apprenticecodex.magi_agent_suit.rune_hint";
    private static final String SCHOOL_RUNE_KEY = "item.apprenticecodex.magi_agent_suit.school_rune";
    private static final String SPELL_HINT_KEY = "item.apprenticecodex.common.desc.spell_hint";
    private static final String SPELL_HINT_OPEN_KEY = "item.apprenticecodex.common.desc.spell_hint_open";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers armorAttributeModifiers;

    public MagiAgentSuitItem(Type type) {
        super(Holder.direct(MagiAgentSuitStats.MATERIAL), type, MagiAgentSuitStats.createProperties(type).fireResistant());
        this.armorAttributeModifiers = MagiAgentSuitStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean isWisdomActiveWhileHeld() {
        return false;
    }

    public Type getArmorType() {
        return getType();
    }

    public boolean hasImbueSlot() {
        return getType() == Type.CHESTPLATE;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private MagiAgentSuitRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity,
                    ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original
            ) {
                if (renderer == null) {
                    renderer = new MagiAgentSuitRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || !hasImbueSlot() || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, true));
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        return enchantmentId != null && enchantmentId.equals(Enchantments.WISDOM.location());
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
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var builder = ItemAttributeModifiers.builder();
        for (var entry : super.getDefaultAttributeModifiers(stack).modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        for (var entry : armorAttributeModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        MagiAgentSuitStats.addSpellPowerModifier(
                builder,
                getType(),
                ApprenticeCodexServerConfig.magiAgentSuitSpellPowerBonus()
        );
        var schoolPowerAttribute = getResolvedSchoolPowerAttribute(stack);
        if (schoolPowerAttribute != null) {
            MagiAgentSuitStats.addSchoolSpellPowerModifier(
                    builder,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(schoolPowerAttribute),
                    getType(),
                    ApprenticeCodexServerConfig.magiAgentSuitSchoolSpellPowerBonus()
            );
        }
        return MagicArmorAttributeHelper.mergeTooltipEquivalentModifiers(
                builder.build(),
                "apprenticecodex.magi_agent_suit." + MagiAgentSuitStats.typeToken(getType()) + ".merged"
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return MagiAgentSuitStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return MagiAgentSuitStats.isRepairIngredient(repair) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        appendSuitEffectHoverText(lines);
        var school = getResolvedCalibrationSchool(stack);
        if (school == null) {
            lines.add(Component.translatable(RUNE_HINT_KEY).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable(SCHOOL_RUNE_KEY, school.getDisplayName()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static @NotNull ItemStack readCalibrationAdjustment(@NotNull ItemStack suitStack, int slot) {
        if (!isValidCalibrationAccess(suitStack, slot)) {
            return ItemStack.EMPTY;
        }

        var calibrationTag = getCalibrationTag(suitStack);
        if (calibrationTag == null) {
            return ItemStack.EMPTY;
        }
        var itemId = ResourceLocation.tryParse(calibrationTag.getString(ADJUSTMENT_ITEM_TAG));
        if (itemId == null) {
            return ItemStack.EMPTY;
        }

        var item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static void writeCalibrationAdjustment(@NotNull ItemStack suitStack, int slot, @NotNull ItemStack stack) {
        if (!isValidCalibrationAccess(suitStack, slot)) {
            return;
        }

        if (stack.isEmpty()) {
            clearCalibrationAdjustment(suitStack);
            return;
        }

        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, suitStack, tag -> {
            var calibrationTag = tag.getCompound(CALIBRATION_TAG);
            calibrationTag.putString(ADJUSTMENT_ITEM_TAG, itemId.toString());
            tag.put(CALIBRATION_TAG, calibrationTag);
        });
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

    public static @Nullable SchoolType getResolvedCalibrationSchool(ItemStack stack) {
        return ScrollcasterSchoolRuneResolver.resolveSchool(readCalibrationAdjustment(stack, 0)).orElse(null);
    }

    private void appendSuitEffectHoverText(List<Component> lines) {
        var descriptionKey = getDescriptionId() + ".desc";
        if (getType() == Type.HELMET) {
            lines.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
            return;
        }

        if (!ImbueTooltipHelper.hasDetailsKeyDown()) {
            lines.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(SPELL_HINT_KEY).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        lines.add(Component.translatable(SPELL_HINT_OPEN_KEY).withStyle(ChatFormatting.GRAY));
        for (var spell : MagiAgentSuitEffects.targetSpells()) {
            if (getType() == Type.LEGGINGS && spell.getCastType() == CastType.INSTANT) {
                continue;
            }
            lines.add(Component.literal("- ")
                    .append(spell.getDisplayName(null))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static @Nullable Attribute getResolvedSchoolPowerAttribute(ItemStack stack) {
        return MagicTools.resolveSchoolPowerAttribute(getResolvedCalibrationSchool(stack));
    }

    private static boolean isValidCalibrationAccess(@NotNull ItemStack suitStack, int slot) {
        return slot >= 0
                && slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT
                && !suitStack.isEmpty()
                && suitStack.getItem() instanceof MagiAgentSuitItem;
    }

    private static void clearCalibrationAdjustment(@NotNull ItemStack suitStack) {
        var calibrationTag = getCalibrationTag(suitStack);
        if (calibrationTag == null) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, suitStack, data -> data.remove(CALIBRATION_TAG));
    }

    private static @Nullable CompoundTag getCalibrationTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        var tag = customData.copyTag();
        return tag.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)
                ? tag.getCompound(CALIBRATION_TAG)
                : null;
    }
}
