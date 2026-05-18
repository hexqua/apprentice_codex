package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
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

public abstract class AbstractOffhandMagicItem extends Item
        implements IPresetSpellContainer, IJeiInfoItem, NonDamageableAnvilMergeItem {
    private static final double ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL = 0.02D;
    private static final double REFLUX_MANA_REGEN_PER_LEVEL = 0.05D;
    private static final double RESERVOIR_MAX_MANA_PER_LEVEL = 20.0D;
    private static final double SURGE_SPELL_POWER_PER_LEVEL = 0.02D;
    private static final double ATTUNEMENT_SPELL_POWER_PER_LEVEL = 0.04D;
    private static final double TENSE_CAST_TIME_REDUCTION_PER_LEVEL = 0.05D;
    private static final String JEI_INFO_GROUP_ID = "offhand_magic_items";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.offhand_magic_items.desc_";

    private final Supplier<? extends AbstractSpell> configuredSpell;
    private final int configuredSpellLevel;
    private final boolean startsWithPresetSpell;
    private final String itemKey;
    private final List<AttributeBonus> offhandBonuses;
    private final ItemAttributeModifiers baseOffhandModifiers;

    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            Rarity rarity,
            String itemKey,
            List<AttributeBonus> offhandBonuses
    ) {
        super(createProperties(rarity));
        this.configuredSpell = Objects.requireNonNull(configuredSpell);
        this.configuredSpellLevel = configuredSpellLevel;
        this.startsWithPresetSpell = true;
        this.itemKey = normalizeKeyToken(itemKey);
        this.offhandBonuses = List.copyOf(offhandBonuses);
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
        super(createProperties(rarity));
        this.configuredSpell = null;
        this.configuredSpellLevel = 0;
        this.startsWithPresetSpell = false;
        this.itemKey = normalizeKeyToken(itemKey);
        this.offhandBonuses = List.copyOf(offhandBonuses);
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
        if (stack == null || stack.isEmpty() || !stack.isEnchanted()) {
            return hasStackDependentModifiers
                    ? mergeTooltipEquivalentModifiers(builder.build(), itemKey + "_offhand_merged")
                    : baseModifiers;
        }

        var alacrityLevel = Enchantments.getLevel(stack, Enchantments.ALACRITY);
        var refluxLevel = Enchantments.getLevel(stack, Enchantments.REFLUX);
        var reservoirLevel = Enchantments.getLevel(stack, Enchantments.RESERVOIR);
        var surgeLevel = Enchantments.getLevel(stack, Enchantments.SURGE);
        var attunementLevel = Enchantments.getLevel(stack, Enchantments.ATTUNEMENT);
        var tenseLevel = Enchantments.getLevel(stack, Enchantments.TENSE);

        if (alacrityLevel <= 0
                && refluxLevel <= 0
                && reservoirLevel <= 0
                && surgeLevel <= 0
                && attunementLevel <= 0
                && tenseLevel <= 0) {
            return hasStackDependentModifiers
                    ? mergeTooltipEquivalentModifiers(builder.build(), itemKey + "_offhand_merged")
                    : baseModifiers;
        }

        addEnchantmentModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION,
                alacrityLevel * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "alacrity_cooldown_reduction"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MANA_REGEN,
                refluxLevel * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "reflux_mana_regen"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MAX_MANA,
                reservoirLevel * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADD_VALUE,
                "reservoir_max_mana"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.SPELL_POWER,
                surgeLevel * SURGE_SPELL_POWER_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "surge_spell_power"
        );

        if (attunementLevel > 0) {
            var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
            var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            if (attunementSpellPowerAttribute != null) {
                addEnchantmentModifier(
                        builder,
                        BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementSpellPowerAttribute),
                        attunementLevel * ATTUNEMENT_SPELL_POWER_PER_LEVEL,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                        "attunement_spell_power"
                );
            }
        }

        addEnchantmentModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION,
                tenseLevel * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "tense_cast_time_reduction"
        );

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
        if (amount == 0.0D) {
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
        return new Item.Properties().stacksTo(1).rarity(Objects.requireNonNull(rarity));
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
