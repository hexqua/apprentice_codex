package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.armor.ElementMaidenRobeRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ElementMaidenRobeItem extends ArmorItem
        implements GeoItem, IPresetSpellContainer, UniqueItem, TranscendencePolicy {
    private static final String DESCRIPTION_KEY = "item." + ApprenticeCodex.MODID + ".element_maiden_robe.desc";
    private static final String SPELLBOOK_SCHOOL_POWER_BONUSES_TAG = "ElementMaidenRobeSpellbookSchoolPowerBonuses";
    private static final String ATTRIBUTE_TAG = "Attribute";
    private static final String AMOUNT_TAG = "Amount";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers armorAttributeModifiers;

    public ElementMaidenRobeItem(Type type) {
        super(Holder.direct(ElementMaidenRobeStats.MATERIAL), type, ElementMaidenRobeStats.createProperties(type).rarity(Rarity.EPIC).fireResistant());
        this.armorAttributeModifiers = ElementMaidenRobeStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
    }

    public Type getArmorType() {
        return getType();
    }

    public boolean hasImbueSlot() {
        return getType() == Type.CHESTPLATE;
    }

    @Override
    public boolean isTranscendenceActiveWhileHeld() {
        return false;
    }

    @Override
    public boolean supportsDirectTranscendenceApplication() {
        return hasImbueSlot();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ElementMaidenRobeRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity,
                    ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original
            ) {
                if (renderer == null) {
                    renderer = new ElementMaidenRobeRenderer();
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

        // 胴体だけを固有潜在魔法の Imbue 対象にする.
        ISpellContainer.createImbuedContainer(SpellRegistry.DIVINE_POSSESSION.get(), 1, itemStack);
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
        return enchantmentId != null && isSupportedRobeEnchantment(enchantmentId);
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

        return enchantments.keySet().stream().allMatch(enchantment -> supportsEnchantment(stack, enchantment));
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
        ElementMaidenRobeStats.addSpellPowerModifier(
                builder,
                getType(),
                ApprenticeCodexServerConfig.elementMaidenRobeSpellPowerBonus()
        );
        addSpellbookSchoolPowerModifiers(builder, stack);
        addChestMagicEnchantmentModifiers(builder, stack);
        return MagicArmorAttributeHelper.mergeTooltipEquivalentModifiers(
                builder.build(),
                "apprenticecodex.element_maiden_robe." + ElementMaidenRobeStats.typeToken(getType()) + ".merged"
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ElementMaidenRobeStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return ElementMaidenRobeStats.isRepairIngredient(repair) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(DESCRIPTION_KEY).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static boolean setSpellbookSchoolPowerBonuses(ItemStack stack, Map<Attribute, Double> bonuses) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ElementMaidenRobeItem)) {
            return false;
        }

        var normalizedBonuses = normalizeSpellbookSchoolPowerBonuses(bonuses);
        if (normalizedBonuses.isEmpty()) {
            return clearSpellbookSchoolPowerBonuses(stack);
        }
        if (getSpellbookSchoolPowerBonuses(stack).equals(normalizedBonuses)) {
            return false;
        }

        var bonusList = new ListTag();
        for (var entry : normalizedBonuses.entrySet()) {
            var attributeId = BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey());
            if (attributeId == null) {
                continue;
            }

            var bonusTag = new CompoundTag();
            bonusTag.putString(ATTRIBUTE_TAG, attributeId.toString());
            bonusTag.putDouble(AMOUNT_TAG, entry.getValue());
            bonusList.add(bonusTag);
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(SPELLBOOK_SCHOOL_POWER_BONUSES_TAG, bonusList));
        return true;
    }

    public static boolean clearSpellbookSchoolPowerBonuses(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        if (tag == null || !tag.contains(SPELLBOOK_SCHOOL_POWER_BONUSES_TAG)) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.remove(SPELLBOOK_SCHOOL_POWER_BONUSES_TAG));
        return true;
    }

    public static Map<Attribute, Double> getSpellbookSchoolPowerBonuses(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        if (tag == null || !tag.contains(SPELLBOOK_SCHOOL_POWER_BONUSES_TAG, Tag.TAG_LIST)) {
            return Map.of();
        }

        var result = new LinkedHashMap<Attribute, Double>();
        var bonusList = tag.getList(SPELLBOOK_SCHOOL_POWER_BONUSES_TAG, Tag.TAG_COMPOUND);
        for (var index = 0; index < bonusList.size(); ++index) {
            var bonusTag = bonusList.getCompound(index);
            var attributeId = ResourceLocation.tryParse(bonusTag.getString(ATTRIBUTE_TAG));
            if (attributeId == null) {
                continue;
            }

            var attribute = BuiltInRegistries.ATTRIBUTE.get(attributeId);
            var amount = bonusTag.getDouble(AMOUNT_TAG);
            if (attribute != null && amount > 0.0D) {
                result.put(attribute, amount);
            }
        }
        return result;
    }

    private void addSpellbookSchoolPowerModifiers(
            ItemAttributeModifiers.Builder builder,
            ItemStack stack
    ) {
        for (var entry : getSpellbookSchoolPowerBonuses(stack).entrySet()) {
            var attributeId = BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey());
            if (attributeId == null) {
                continue;
            }

            MagicArmorAttributeHelper.addModifier(
                    builder,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(entry.getKey()),
                    entry.getValue(),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    net.minecraft.world.entity.EquipmentSlotGroup.bySlot(getType().getSlot()),
                    "element_maiden_robe_" + ElementMaidenRobeStats.typeToken(getType())
                            + "_spellbook_school_power_" + normalizeAttributeId(attributeId)
            );
        }
    }

    private void addChestMagicEnchantmentModifiers(
            ItemAttributeModifiers.Builder builder,
            ItemStack stack
    ) {
        if (!hasImbueSlot()) {
            return;
        }

        var surgeLevel = Enchantments.getLevel(stack, Enchantments.SURGE);
        ElementMaidenRobeStats.addSurgeSpellPowerModifier(builder, getType(), surgeLevel);

        var attunementLevel = Enchantments.getLevel(stack, Enchantments.ATTUNEMENT);
        if (attunementLevel <= 0) {
            return;
        }

        var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
        var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
        if (attunementSpellPowerAttribute != null) {
            ElementMaidenRobeStats.addAttunementSpellPowerModifier(
                    builder,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementSpellPowerAttribute),
                    getType(),
                    attunementLevel
            );
        }
    }

    private boolean isSupportedRobeEnchantment(ResourceLocation enchantmentId) {
        return enchantmentId.equals(Enchantments.WISDOM.location())
                || (hasImbueSlot() && enchantmentId.equals(Enchantments.TRANSCENDENCE.location()))
                || (hasImbueSlot() && enchantmentId.equals(Enchantments.SURGE.location()))
                || (hasImbueSlot() && enchantmentId.equals(Enchantments.ATTUNEMENT.location()));
    }

    private static Map<Attribute, Double> normalizeSpellbookSchoolPowerBonuses(Map<Attribute, Double> bonuses) {
        if (bonuses.isEmpty()) {
            return Map.of();
        }

        var result = new LinkedHashMap<Attribute, Double>();
        bonuses.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0.0D)
                .filter(entry -> BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey()) != null)
                .sorted(Comparator.comparing(entry -> BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey()).toString()))
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result.isEmpty() ? Map.of() : result;
    }

    private static @Nullable CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    private static String normalizeAttributeId(ResourceLocation attributeId) {
        return attributeId.getNamespace() + "." + attributeId.getPath().replace('/', '.');
    }
}
