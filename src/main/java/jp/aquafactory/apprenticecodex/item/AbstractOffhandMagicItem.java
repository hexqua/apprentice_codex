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
    private final String itemKey;
    private final List<AttributeBonus> offhandBonuses;
    private final Multimap<Attribute, AttributeModifier> offhandModifiers;

    protected AbstractOffhandMagicItem(
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            String itemKey,
            List<AttributeBonus> offhandBonuses
    ) {
        super(new Item.Properties().stacksTo(1));
        this.configuredSpell = Objects.requireNonNull(configuredSpell);
        this.configuredSpellLevel = configuredSpellLevel;
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

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        ISpellContainer.createImbuedContainer(configuredSpell.get(), configuredSpellLevel, itemStack);
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

    protected static AttributeBonus bonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attributeSupplier, amount, operation, null);
    }

    protected static AttributeBonus bonus(
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(() -> attribute, amount, operation, null);
    }

    protected static AttributeBonus bonus(
            Holder<Attribute> attributeHolder,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attributeHolder::value, amount, operation, null);
    }

    protected static AttributeBonus bonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attributeSupplier, amount, operation, key);
    }

    protected static AttributeBonus bonus(
            Holder<Attribute> attributeHolder,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attributeHolder::value, amount, operation, key);
    }

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
