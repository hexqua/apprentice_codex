package jp.aquafactory.apprenticecodex.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.armor.ElementMaidenRobeRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ElementMaidenRobeItem extends ArmorItem implements GeoItem, IPresetSpellContainer, UniqueItem {
    private static final ResourceLocation ARMOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/element_maiden_robe.png");
    private static final String DESCRIPTION_KEY = "item." + ApprenticeCodex.MODID + ".element_maiden_robe.desc";
    private static final String SPELLBOOK_SCHOOL_POWER_BONUSES_TAG = "ElementMaidenRobeSpellbookSchoolPowerBonuses";
    private static final String ATTRIBUTE_TAG = "Attribute";
    private static final String AMOUNT_TAG = "Amount";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Type armorType;
    private final Multimap<Attribute, AttributeModifier> armorAttributeModifiers;

    public ElementMaidenRobeItem(Type type) {
        super(ElementMaidenRobeStats.MATERIAL, type, new Properties().rarity(Rarity.EPIC).fireResistant());
        this.armorType = type;
        this.armorAttributeModifiers = ElementMaidenRobeStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
    }

    public Type getArmorType() {
        return armorType;
    }

    public boolean hasImbueSlot() {
        return armorType == Type.CHESTPLATE;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                   EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) {
                    renderer = new ElementMaidenRobeRenderer();
                }

                renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
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
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return false;
        }

        if (ApprenticeCodex.MODID.equals(enchantmentId.getNamespace())) {
            return isSupportedRobeEnchantment(enchantment);
        }

        return enchantment.canApplyAtEnchantingTable(createArmorProbeStack());
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

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var baseModifiers = super.getAttributeModifiers(slot, stack);
        if (slot != armorType.getSlot()) {
            return baseModifiers;
        }

        var extraBuilder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        extraBuilder.putAll(armorAttributeModifiers);
        ElementMaidenRobeStats.addSpellPowerModifier(
                extraBuilder,
                armorType,
                ApprenticeCodexServerConfig.elementMaidenRobeSpellPowerBonus()
        );
        addSpellbookSchoolPowerModifiers(extraBuilder, stack);
        addChestMagicEnchantmentModifiers(extraBuilder, stack);

        var mergedExtraModifiers = MagicArmorAttributeHelper.mergeTooltipEquivalentModifiers(
                extraBuilder.build(),
                "apprenticecodex.element_maiden_robe." + ElementMaidenRobeStats.typeToken(armorType) + ".merged"
        );

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        builder.putAll(mergedExtraModifiers);
        return builder.build();
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable(DESCRIPTION_KEY).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // GeoArmor 描画以外の vanilla 問い合わせでも、同じ既存テクスチャへ解決して警告を避ける.
        return ARMOR_TEXTURE.toString();
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
            var attributeId = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            if (attributeId == null) {
                continue;
            }

            var bonusTag = new CompoundTag();
            bonusTag.putString(ATTRIBUTE_TAG, attributeId.toString());
            bonusTag.putDouble(AMOUNT_TAG, entry.getValue());
            bonusList.add(bonusTag);
        }

        stack.getOrCreateTag().put(SPELLBOOK_SCHOOL_POWER_BONUSES_TAG, bonusList);
        return true;
    }

    public static boolean clearSpellbookSchoolPowerBonuses(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(SPELLBOOK_SCHOOL_POWER_BONUSES_TAG)) {
            return false;
        }

        tag.remove(SPELLBOOK_SCHOOL_POWER_BONUSES_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
        return true;
    }

    public static Map<Attribute, Double> getSpellbookSchoolPowerBonuses(ItemStack stack) {
        var tag = stack.getTag();
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

            var attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
            var amount = bonusTag.getDouble(AMOUNT_TAG);
            if (attribute != null && amount > 0.0D) {
                result.put(attribute, amount);
            }
        }
        return result;
    }

    private void addSpellbookSchoolPowerModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack
    ) {
        for (var entry : getSpellbookSchoolPowerBonuses(stack).entrySet()) {
            var attributeId = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            if (attributeId == null) {
                continue;
            }

            MagicArmorAttributeHelper.addModifier(
                    builder,
                    entry.getKey(),
                    entry.getValue(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "apprenticecodex.element_maiden_robe." + ElementMaidenRobeStats.typeToken(armorType)
                            + ".spellbook_school_power." + normalizeAttributeId(attributeId)
            );
        }
    }

    private void addChestMagicEnchantmentModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack
    ) {
        if (!hasImbueSlot()) {
            return;
        }

        if (EnchantmentRegistry.SURGE.isPresent()) {
            var surgeLevel = stack.getEnchantmentLevel(EnchantmentRegistry.SURGE.get());
            ElementMaidenRobeStats.addSurgeSpellPowerModifier(builder, armorType, surgeLevel);
        }

        if (!EnchantmentRegistry.ATTUNEMENT.isPresent()) {
            return;
        }

        var attunementLevel = stack.getEnchantmentLevel(EnchantmentRegistry.ATTUNEMENT.get());
        if (attunementLevel <= 0) {
            return;
        }

        var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
        var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
        ElementMaidenRobeStats.addAttunementSpellPowerModifier(
                builder,
                attunementSpellPowerAttribute,
                armorType,
                attunementLevel
        );
    }

    private boolean isSupportedRobeEnchantment(Enchantment enchantment) {
        return (EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get())
                || (hasImbueSlot() && EnchantmentRegistry.TRANSCENDENCE.isPresent()
                && enchantment == EnchantmentRegistry.TRANSCENDENCE.get())
                || (hasImbueSlot() && EnchantmentRegistry.SURGE.isPresent()
                && enchantment == EnchantmentRegistry.SURGE.get())
                || (hasImbueSlot() && EnchantmentRegistry.ATTUNEMENT.isPresent()
                && enchantment == EnchantmentRegistry.ATTUNEMENT.get());
    }

    private ItemStack createArmorProbeStack() {
        return switch (armorType) {
            case HELMET -> new ItemStack(Items.LEATHER_HELMET);
            case CHESTPLATE -> new ItemStack(Items.LEATHER_CHESTPLATE);
            case LEGGINGS -> new ItemStack(Items.LEATHER_LEGGINGS);
            case BOOTS -> new ItemStack(Items.LEATHER_BOOTS);
        };
    }

    private static Map<Attribute, Double> normalizeSpellbookSchoolPowerBonuses(Map<Attribute, Double> bonuses) {
        if (bonuses.isEmpty()) {
            return Map.of();
        }

        var result = new LinkedHashMap<Attribute, Double>();
        bonuses.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0.0D)
                .filter(entry -> ForgeRegistries.ATTRIBUTES.getKey(entry.getKey()) != null)
                .sorted(Comparator.comparing(entry -> ForgeRegistries.ATTRIBUTES.getKey(entry.getKey()).toString()))
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result.isEmpty() ? Map.of() : result;
    }

    private static String normalizeAttributeId(ResourceLocation attributeId) {
        return attributeId.getNamespace() + "." + attributeId.getPath().replace('/', '.');
    }
}
