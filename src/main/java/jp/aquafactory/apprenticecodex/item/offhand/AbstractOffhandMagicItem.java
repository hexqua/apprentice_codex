package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;

public abstract class AbstractOffhandMagicItem extends Item
        implements IPresetSpellContainer, IJeiInfoItem, NonDamageableAnvilMergeItem, TranscendencePolicy,
        AttributeEnchantmentPolicy {
    private static final String JEI_INFO_GROUP_ID = "offhand_magic_items";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.offhand_magic_items.desc_";
    private static final int ENCHANTMENT_VALUE = 1;

    private final Supplier<? extends AbstractSpell> configuredSpell;
    private final int configuredSpellLevel;
    private final boolean startsWithPresetSpell;
    private final String itemKey;
    private final List<AttributeBonus> offhandBonuses;
    private final ItemAttributeModifiers baseOffhandModifiers;

    @Override
    public java.util.Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return ALL_ATTRIBUTE_ENCHANTMENTS;
    }

    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            Rarity rarity,
            String itemKey,
            List<AttributeBonus> offhandBonuses
    ) {
        this(configuredSpell, configuredSpellLevel, rarity, itemKey, offhandBonuses, false);
    }

    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            Rarity rarity,
            String itemKey,
            List<AttributeBonus> attributeBonuses,
            boolean fireResistant
    ) {
        super(createProperties(rarity, fireResistant));
        this.configuredSpell = Objects.requireNonNull(configuredSpell);
        this.configuredSpellLevel = configuredSpellLevel;
        this.startsWithPresetSpell = true;
        this.itemKey = normalizeKeyToken(itemKey);
        this.offhandBonuses = List.copyOf(attributeBonuses);
        this.baseOffhandModifiers = buildBaseOffhandModifiers();
    }

    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            String itemKey,
            List<AttributeBonus> attributeBonuses
    ) {
        this(configuredSpell, configuredSpellLevel, Rarity.COMMON, itemKey, attributeBonuses);
    }

    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            Rarity rarity,
            String itemKey,
            AttributeBonus... attributeBonuses
    ) {
        this(configuredSpell, configuredSpellLevel, rarity, itemKey, List.of(attributeBonuses));
    }

    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            Rarity rarity,
            String itemKey,
            boolean fireResistant,
            AttributeBonus... attributeBonuses
    ) {
        this(configuredSpell, configuredSpellLevel, rarity, itemKey, List.of(attributeBonuses), fireResistant);
    }

    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            String itemKey,
            AttributeBonus... attributeBonuses
    ) {
        this(configuredSpell, configuredSpellLevel, Rarity.COMMON, itemKey, List.of(attributeBonuses));
    }

    protected AbstractOffhandMagicItem(
            Rarity rarity,
            String itemKey,
            List<AttributeBonus> offhandBonuses
    ) {
        this(rarity, itemKey, offhandBonuses, false);
    }

    protected AbstractOffhandMagicItem(
            Rarity rarity,
            String itemKey,
            List<AttributeBonus> attributeBonuses,
            boolean fireResistant
    ) {
        super(createProperties(rarity, fireResistant));
        this.configuredSpell = null;
        this.configuredSpellLevel = 0;
        this.startsWithPresetSpell = false;
        this.itemKey = normalizeKeyToken(itemKey);
        this.offhandBonuses = List.copyOf(attributeBonuses);
        this.baseOffhandModifiers = buildBaseOffhandModifiers();
    }

    protected AbstractOffhandMagicItem(
            String itemKey,
            List<AttributeBonus> attributeBonuses
    ) {
        this(Rarity.COMMON, itemKey, attributeBonuses);
    }

    protected AbstractOffhandMagicItem(
            Rarity rarity,
            String itemKey,
            AttributeBonus... attributeBonuses
    ) {
        this(rarity, itemKey, List.of(attributeBonuses));
    }

    protected AbstractOffhandMagicItem(
            Rarity rarity,
            String itemKey,
            boolean fireResistant,
            AttributeBonus... attributeBonuses
    ) {
        this(rarity, itemKey, List.of(attributeBonuses), fireResistant);
    }

    protected AbstractOffhandMagicItem(
            String itemKey,
            AttributeBonus... attributeBonuses
    ) {
        this(Rarity.COMMON, itemKey, List.of(attributeBonuses));
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty() || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        if (startsWithPresetSpell) {
            InitialSpellContainerHelper.setInitialContainer(
                    itemStack,
                    1,
                    true,
                    false,
                    configuredSpell,
                    configuredSpellLevel
            );
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, false));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return buildOffhandModifiers(stack, baseOffhandModifiers);
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.OFFHAND;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public String getJeiInfoGroupId() {
        return JEI_INFO_GROUP_ID;
    }

    private ItemAttributeModifiers buildBaseOffhandModifiers() {
        var builder = ItemAttributeModifiers.builder();
        for (int i = 0; i < offhandBonuses.size(); ++i) {
            var bonus = offhandBonuses.get(i);
            var attribute = bonus.attribute();
            if (attribute == null || bonus.amount() == 0.0D) {
                continue;
            }

            var attributeKey = resolveAttributeKey(bonus, i);
            var modifierId = ResourceLocation.fromNamespaceAndPath(
                    "apprenticecodex",
                    itemKey + "_offhand_" + attributeKey + "_" + i
            );

            builder.add(
                    attribute,
                    new AttributeModifier(modifierId, bonus.amount(), bonus.operation()),
                    EquipmentSlotGroup.OFFHAND
            );
        }
        return builder.build();
    }

    private ItemAttributeModifiers buildOffhandModifiers(ItemStack stack, ItemAttributeModifiers defaultModifiers) {
        var baseModifiers = stripManagedEnchantmentModifiers(defaultModifiers);
        var builder = ItemAttributeModifiers.builder();
        for (var entry : baseModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }

        var hasStackDependentModifiers = addStackDependentModifiers(builder, stack, itemKey + "_offhand_stack");
        var hasEnchantmentModifiers = AttributeEnchantmentResolver.addModifiers(
                builder,
                stack,
                EquipmentSlotGroup.OFFHAND,
                itemKey + "_offhand_enchant"
        );
        if (!hasStackDependentModifiers && !hasEnchantmentModifiers) {
            return baseModifiers;
        }

        return mergeTooltipEquivalentModifiers(builder.build(), itemKey + "_offhand_merged");
    }

    // Imbue 内容に応じた補正など、stack の状態を見て増減させたい派生アイテム向け.
    protected boolean addStackDependentModifiers(
            ItemAttributeModifiers.Builder builder,
            ItemStack stack,
            String modifierKeyPrefix
    ) {
        return false;
    }

    // Better Combat は両手武器中に OFFHAND 装備参照を空へ差し替えるため、
    // 救済可否の責務を offhand 専用品側へ寄せて個別調整できるようにする。
    public boolean allowsBetterCombatOffhandRescue(ItemStack stack) {
        return true;
    }

    private ItemAttributeModifiers stripManagedEnchantmentModifiers(ItemAttributeModifiers modifiers) {
        if (modifiers.modifiers().isEmpty()) {
            return modifiers;
        }

        var enchantModifierPrefix = itemKey + "_offhand_enchant_";
        var mergedModifierPrefix = itemKey + "_offhand_merged_";
        var builder = ItemAttributeModifiers.builder();
        boolean changed = false;
        for (var entry : modifiers.modifiers()) {
            var modifierPath = entry.modifier().id().getPath();
            if (modifierPath.startsWith(enchantModifierPrefix) || modifierPath.startsWith(mergedModifierPrefix)) {
                changed = true;
                continue;
            }

            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        return changed ? builder.build() : modifiers;
    }

    private void addEnchantmentModifier(
            ItemAttributeModifiers.Builder builder,
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        if (attribute == null || amount == 0.0D) {
            return;
        }

        var modifierId = ResourceLocation.fromNamespaceAndPath(
                "apprenticecodex",
                itemKey + "_offhand_enchant_" + key
        );
        builder.add(
                attribute,
                new AttributeModifier(modifierId, amount, operation),
                EquipmentSlotGroup.OFFHAND
        );
    }

    protected void addStackDependentModifier(
            ItemAttributeModifiers.Builder builder,
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        if (attribute == null) {
            return;
        }

        addEnchantmentModifier(
                builder,
                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute),
                amount,
                operation,
                key
        );
    }

    private static ItemAttributeModifiers mergeTooltipEquivalentModifiers(
            ItemAttributeModifiers modifiers,
            String modifierPathPrefix
    ) {
        if (modifiers.modifiers().isEmpty()) {
            return modifiers;
        }

        var merged = new LinkedHashMap<MergeTarget, MergedModifier>();
        var passthrough = new ArrayList<ItemAttributeModifiers.Entry>();
        int unknownIndex = 0;

        for (var entry : modifiers.modifiers()) {
            var operation = entry.modifier().operation();
            if (operation != AttributeModifier.Operation.ADD_VALUE
                    && operation != AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                passthrough.add(entry);
                continue;
            }

            var attributeToken = resolveAttributeToken(entry.attribute(), unknownIndex++);
            var target = new MergeTarget(attributeToken, operation, entry.slot());
            var existing = merged.get(target);
            if (existing == null) {
                merged.put(target, new MergedModifier(entry.attribute(), entry.modifier().amount()));
            } else {
                merged.put(target, new MergedModifier(existing.attribute(), existing.amount() + entry.modifier().amount()));
            }
        }

        var builder = ItemAttributeModifiers.builder();
        int mergedIndex = 0;
        for (Map.Entry<MergeTarget, MergedModifier> entry : merged.entrySet()) {
            var target = entry.getKey();
            var mergedModifier = entry.getValue();
            if (mergedModifier.amount() == 0.0D) {
                continue;
            }

            var operationToken = target.operation().name().toLowerCase(Locale.ROOT);
            var modifierId = ResourceLocation.fromNamespaceAndPath(
                    "apprenticecodex",
                    modifierPathPrefix + "_" + target.attributeToken() + "_" + operationToken + "_" + mergedIndex++
            );

            builder.add(
                    mergedModifier.attribute(),
                    new AttributeModifier(modifierId, mergedModifier.amount(), target.operation()),
                    target.slot()
            );
        }

        for (var entry : passthrough) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }

        return builder.build();
    }

    private static String resolveAttributeToken(Holder<Attribute> attribute, int index) {
        return attribute.unwrapKey()
                .map(resourceKey -> normalizeKeyToken(resourceKey.location().toString()))
                .orElse("unknown_" + index);
    }

    private static String resolveAttributeKey(AttributeBonus bonus, int index) {
        if (bonus.key() != null && !bonus.key().isBlank()) {
            return normalizeKeyToken(bonus.key());
        }

        return bonus.attribute().unwrapKey()
                .map(resourceKey -> normalizeKeyToken(resourceKey.location().toString()))
                .orElse("unknown_" + index);
    }

    private static String normalizeKeyToken(String token) {
        return Objects.requireNonNull(token)
                .toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_')
                .replace('.', '_')
                .replaceAll("[^a-z0-9_-]", "_");
    }

    private static Item.Properties createProperties(Rarity rarity) {
        return createProperties(rarity, false);
    }

    private static Item.Properties createProperties(Rarity rarity, boolean fireResistant) {
        var properties = new Item.Properties().stacksTo(1).rarity(Objects.requireNonNull(rarity));
        return fireResistant ? properties.fireResistant() : properties;
    }

    // `bonus` ヘルパーは属性参照の受け取り方ごとにオーバーロードしている.
    // 将来のアイテムで属性の持ち方が異なっても同じ書き味で定義できるようにしている.

    // Forge の RegistryObject や Deferred 登録由来の Supplier をそのまま渡す用途.
    protected static AttributeBonus bonus(
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attribute, amount, operation, null);
    }

    protected static AttributeBonus bonus(
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attribute, amount, operation, key);
    }

    protected record AttributeBonus(
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            @Nullable String key
    ) {
        public AttributeBonus {
            Objects.requireNonNull(attribute);
            Objects.requireNonNull(operation);
        }
    }

    private record MergeTarget(
            String attributeToken,
            AttributeModifier.Operation operation,
            EquipmentSlotGroup slot
    ) {
    }

    private record MergedModifier(
            Holder<Attribute> attribute,
            double amount
    ) {
    }
}
