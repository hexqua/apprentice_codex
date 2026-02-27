package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractOffhandMagicItem extends Item implements IPresetSpellContainer {
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
        this.offhandModifiers = buildOffhandModifiers();
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
        this.offhandModifiers = buildOffhandModifiers();
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

    public SpellData getConfiguredSpellData(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return SpellData.EMPTY;
        }

        return ISpellContainer.get(stack).getSpellAtIndex(0);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.OFFHAND) {
            return offhandModifiers;
        }

        return super.getAttributeModifiers(slot, stack);
    }

    private Multimap<Attribute, AttributeModifier> buildOffhandModifiers() {
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
