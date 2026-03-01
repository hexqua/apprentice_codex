package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractOffhandMagicItem extends Item implements IPresetSpellContainer {
    private static final double ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL = 0.02D;
    private static final double REFLUX_MANA_REGEN_PER_LEVEL = 0.05D;
    private static final double RESERVOIR_MAX_MANA_PER_LEVEL = 20.0D;
    private static final double SURGE_SPELL_POWER_PER_LEVEL = 0.02D;
    private static final double ATTUNEMENT_SPELL_POWER_PER_LEVEL = 0.04D;
    private static final double TENSE_CAST_TIME_REDUCTION_PER_LEVEL = 0.05D;

    private final Supplier<? extends AbstractSpell> configuredSpell;
    private final int configuredSpellLevel;
    private final boolean startsWithPresetSpell;
    private final String itemKey;
    private final List<AttributeBonus> offhandBonuses;
    private final Multimap<Attribute, AttributeModifier> offhandModifiers;

    // 既定魔法入りで開始するアイテム向けコンストラクタ.
    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            String itemKey,
            List<AttributeBonus> offhandBonuses
    ) {
        super(new Item.Properties().stacksTo(1));
        this.configuredSpell = Objects.requireNonNull(configuredSpell);
        this.configuredSpellLevel = configuredSpellLevel;
        this.startsWithPresetSpell = true;
        this.itemKey = normalizeKeyToken(itemKey);
        this.offhandBonuses = List.copyOf(offhandBonuses);
        this.offhandModifiers = buildBaseOffhandModifiers();
    }

    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            String itemKey,
            AttributeBonus... offhandBonuses
    ) {
        this(configuredSpell, configuredSpellLevel, itemKey, List.of(offhandBonuses));
    }

    // 空スロットで開始し、Imbue による魔法追加だけを受け付けたいアイテム向けコンストラクタ.
    protected AbstractOffhandMagicItem(
            String itemKey,
            List<AttributeBonus> offhandBonuses
    ) {
        super(new Item.Properties().stacksTo(1));
        this.configuredSpell = null;
        this.configuredSpellLevel = 0;
        this.startsWithPresetSpell = false;
        this.itemKey = normalizeKeyToken(itemKey);
        this.offhandBonuses = List.copyOf(offhandBonuses);
        this.offhandModifiers = buildBaseOffhandModifiers();
    }

    protected AbstractOffhandMagicItem(
            String itemKey,
            AttributeBonus... offhandBonuses
    ) {
        this(itemKey, List.of(offhandBonuses));
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        if (startsWithPresetSpell) {
            ISpellContainer.createImbuedContainer(configuredSpell.get(), configuredSpellLevel, itemStack);
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, false));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.OFFHAND) {
            return buildOffhandModifiers(stack);
        }

        return super.getAttributeModifiers(slot, stack);
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
        // 非耐久アイテムでもエンチャント台/金床で扱えるようにする.
        return getEnchantmentValue(stack) > 0;
    }

    private Multimap<Attribute, AttributeModifier> buildBaseOffhandModifiers() {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        var prefix = "apprenticecodex." + itemKey + ".offhand";
        for (int i = 0; i < offhandBonuses.size(); ++i) {
            var bonus = offhandBonuses.get(i);
            var attribute = bonus.attributeSupplier().get();
            if (attribute == null) {
                continue;
            }

            var attributeKey = resolveAttributeKey(bonus, attribute, i);
            var modifierIdSeed = prefix + "." + attributeKey + "." + i;
            var modifierId = UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8));

            builder.put(
                    attribute,
                    new AttributeModifier(modifierId, modifierIdSeed, bonus.amount(), bonus.operation())
            );
        }
        return builder.build();
    }

    private Multimap<Attribute, AttributeModifier> buildOffhandModifiers(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return offhandModifiers;
        }
        if (!stack.isEnchanted()) {
            return offhandModifiers;
        }
        if (!EnchantmentRegistry.ALACRITY.isPresent()
                || !EnchantmentRegistry.REFLUX.isPresent()
                || !EnchantmentRegistry.RESERVOIR.isPresent()
                || !EnchantmentRegistry.SURGE.isPresent()
                || !EnchantmentRegistry.ATTUNEMENT.isPresent()
                || !EnchantmentRegistry.TENSE.isPresent()) {
            return offhandModifiers;
        }

        var alacrityLevel = stack.getEnchantmentLevel(EnchantmentRegistry.ALACRITY.get());
        var refluxLevel = stack.getEnchantmentLevel(EnchantmentRegistry.REFLUX.get());
        var reservoirLevel = stack.getEnchantmentLevel(EnchantmentRegistry.RESERVOIR.get());
        var surgeLevel = stack.getEnchantmentLevel(EnchantmentRegistry.SURGE.get());
        var attunementLevel = stack.getEnchantmentLevel(EnchantmentRegistry.ATTUNEMENT.get());
        var tenseLevel = stack.getEnchantmentLevel(EnchantmentRegistry.TENSE.get());

        if (alacrityLevel <= 0
                && refluxLevel <= 0
                && reservoirLevel <= 0
                && surgeLevel <= 0
                && attunementLevel <= 0
                && tenseLevel <= 0) {
            return offhandModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(offhandModifiers);
        var prefix = "apprenticecodex." + itemKey + ".offhand.enchant";

        addEnchantmentModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION.get(),
                alacrityLevel * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".alacrity.cooldown_reduction"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MANA_REGEN.get(),
                refluxLevel * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".reflux.mana_regen"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MAX_MANA.get(),
                reservoirLevel * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADDITION,
                prefix + ".reservoir.max_mana"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.SPELL_POWER.get(),
                surgeLevel * SURGE_SPELL_POWER_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".surge.spell_power"
        );
        if (attunementLevel > 0) {
            var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
            var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            addEnchantmentModifier(
                    builder,
                    attunementSpellPowerAttribute,
                    attunementLevel * ATTUNEMENT_SPELL_POWER_PER_LEVEL,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    prefix + ".attunement.spell_power"
            );
        }
        addEnchantmentModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION.get(),
                tenseLevel * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".tense.cast_time_reduction"
        );

        return mergeTooltipEquivalentModifiers(builder.build(), prefix + ".merged");
    }

    private static void addEnchantmentModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String modifierIdSeed
    ) {
        if (attribute == null || amount == 0.0D) {
            return;
        }

        var modifierId = UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8));
        builder.put(attribute, new AttributeModifier(modifierId, modifierIdSeed, amount, operation));
    }

    private static Multimap<Attribute, AttributeModifier> mergeTooltipEquivalentModifiers(
            Multimap<Attribute, AttributeModifier> modifiers,
            String modifierSeedPrefix
    ) {
        if (modifiers.isEmpty()) {
            return modifiers;
        }

        var merged = new LinkedHashMap<MergeTarget, Double>();
        var passthrough = new java.util.ArrayList<Map.Entry<Attribute, AttributeModifier>>();
        for (var entry : modifiers.entries()) {
            var modifier = entry.getValue();
            var operation = modifier.getOperation();
            // MULTIPLY_TOTAL は線形合算できないため、挙動維持のためそのまま残す.
            if (operation != AttributeModifier.Operation.ADDITION
                    && operation != AttributeModifier.Operation.MULTIPLY_BASE) {
                passthrough.add(entry);
                continue;
            }

            var key = new MergeTarget(entry.getKey(), operation);
            merged.merge(key, modifier.getAmount(), Double::sum);
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        for (var entry : merged.entrySet()) {
            var target = entry.getKey();
            var amount = entry.getValue();
            if (amount == 0.0D) {
                continue;
            }

            var operationToken = target.operation().name().toLowerCase(Locale.ROOT);
            var attributeToken = resolveAttributeToken(target.attribute());
            var modifierIdSeed = modifierSeedPrefix + "." + attributeToken + "." + operationToken;
            var modifierId = UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8));
            builder.put(
                    target.attribute(),
                    new AttributeModifier(modifierId, modifierIdSeed, amount, target.operation())
            );
        }

        for (var entry : passthrough) {
            builder.put(entry);
        }
        return builder.build();
    }

    private static String resolveAttributeKey(AttributeBonus bonus, Attribute attribute, int index) {
        if (bonus.key() != null && !bonus.key().isBlank()) {
            return normalizeKeyToken(bonus.key());
        }

        var registryKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (registryKey != null) {
            return normalizeKeyToken(registryKey.toString());
        }

        // 登録キーが得られない属性でもUUIDが毎回安定するようフォールバックを固定化する.
        return "unknown_" + index;
    }

    private static String resolveAttributeToken(Attribute attribute) {
        var registryKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (registryKey == null) {
            return "unknown";
        }
        return normalizeKeyToken(registryKey.toString());
    }

    private static String normalizeKeyToken(String token) {
        return Objects.requireNonNull(token)
                .toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");
    }

    // `bonus` ヘルパーは属性参照の受け取り方ごとにオーバーロードしている.
    // 将来のアイテムで属性の持ち方が異なっても同じ書き味で定義できるようにしている.

    // Forge の RegistryObject や Deferred 登録由来の Supplier をそのまま渡す用途.
    protected static AttributeBonus bonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attributeSupplier, amount, operation, null);
    }

    // 既に Attribute 実体を持っているケース向け.
    protected static AttributeBonus bonus(
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(() -> attribute, amount, operation, null);
    }

    // バニラの Attributes.* のような Holder 経由の属性指定向け.
    protected static AttributeBonus bonus(
            Holder<Attribute> attributeHolder,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attributeHolder::value, amount, operation, null);
    }

    // 属性名の解決が不安定なケースで、UUID シード用キーを明示したい場合の Supplier 版.
    protected static AttributeBonus bonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attributeSupplier, amount, operation, key);
    }

    // 属性名の解決が不安定なケースで、UUID シード用キーを明示したい場合の Holder 版.
    protected static AttributeBonus bonus(
            Holder<Attribute> attributeHolder,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attributeHolder::value, amount, operation, key);
    }

    // 属性名の解決が不安定なケースで、UUID シード用キーを明示したい場合の Attribute 実体版.
    protected static AttributeBonus bonus(
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(() -> attribute, amount, operation, key);
    }

    // `key` は UUID 生成のシードに使う任意識別子.
    // null の場合は属性の登録キーを優先して使用する.
    private record MergeTarget(
            Attribute attribute,
            AttributeModifier.Operation operation
    ) {
    }

    protected record AttributeBonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        public AttributeBonus {
            Objects.requireNonNull(attributeSupplier);
            Objects.requireNonNull(operation);
        }
    }
}
