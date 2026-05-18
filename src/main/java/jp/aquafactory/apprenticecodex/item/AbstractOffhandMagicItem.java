package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractOffhandMagicItem extends Item
        implements IPresetSpellContainer, IJeiInfoItem, NonDamageableAnvilMergeItem {
    private static final String JEI_INFO_GROUP_ID = "offhand_magic_items";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.offhand_magic_items.desc_";

    private final Supplier<? extends AbstractSpell> configuredSpell;
    private final int configuredSpellLevel;
    private final boolean startsWithPresetSpell;
    private final String itemKey;
    private final List<AttributeBonus> attributeBonuses;
    private final Multimap<Attribute, AttributeModifier> baseModifiers;

    // 既定魔法入りで開始するアイテム向けコンストラクタ.
    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            Rarity rarity,
            String itemKey,
            List<AttributeBonus> attributeBonuses
    ) {
        super(createProperties(rarity));
        this.configuredSpell = Objects.requireNonNull(configuredSpell);
        this.configuredSpellLevel = configuredSpellLevel;
        this.startsWithPresetSpell = true;
        this.itemKey = normalizeKeyToken(itemKey);
        this.attributeBonuses = List.copyOf(attributeBonuses);
        this.baseModifiers = buildBaseModifiers();
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

    // 空スロットで開始し、Imbue による魔法追加だけを受け付けたいアイテム向けコンストラクタ.
    protected AbstractOffhandMagicItem(
            Rarity rarity,
            String itemKey,
            List<AttributeBonus> attributeBonuses
    ) {
        super(createProperties(rarity));
        this.configuredSpell = null;
        this.configuredSpellLevel = 0;
        this.startsWithPresetSpell = false;
        this.itemKey = normalizeKeyToken(itemKey);
        this.attributeBonuses = List.copyOf(attributeBonuses);
        this.baseModifiers = buildBaseModifiers();
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.OFFHAND) {
            return buildEquippedModifiers(stack);
        }

        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.OFFHAND;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return OffhandMagicModifierHelper.enchantmentValue();
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        // 非耐久アイテムでもエンチャント台/金床で扱えるようにする.
        return OffhandMagicModifierHelper.isEnchantable(stack);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public String getJeiInfoGroupId() {
        return JEI_INFO_GROUP_ID;
    }

    private Multimap<Attribute, AttributeModifier> buildBaseModifiers() {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        var prefix = "apprenticecodex." + itemKey;
        for (int i = 0; i < attributeBonuses.size(); ++i) {
            var bonus = attributeBonuses.get(i);
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

    private Multimap<Attribute, AttributeModifier> buildEquippedModifiers(ItemStack stack) {
        return OffhandMagicModifierHelper.buildEquippedModifiers(baseModifiers, stack, itemKey, this::addStackDependentModifiers);
    }

    // Imbue 内容に応じた補正など、stack の状態を見て増減させたい派生アイテム向け.
    protected boolean addStackDependentModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack,
            String modifierSeedPrefix
    ) {
        return false;
    }

    // Better Combat は両手武器中に OFFHAND 装備参照を空へ差し替えるため、
    // 救済可否の責務を offhand 専用品側へ寄せて個別調整できるようにする。
    public boolean allowsBetterCombatOffhandRescue(ItemStack stack) {
        return true;
    }

    protected static void addEquippedModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String modifierIdSeed
    ) {
        OffhandMagicModifierHelper.addEquippedModifier(builder, attribute, amount, operation, modifierIdSeed);
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

    private static Item.Properties createProperties(Rarity rarity) {
        return new Item.Properties().stacksTo(1).rarity(Objects.requireNonNull(rarity));
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
