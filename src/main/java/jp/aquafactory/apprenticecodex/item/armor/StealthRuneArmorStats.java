package jp.aquafactory.apprenticecodex.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class StealthRuneArmorStats {
    public static final ArmorMaterial MATERIAL = new StealthRuneArmorMaterial();

    private static final int DURABILITY_MULTIPLIER = 5;
    private static final int ENCHANTMENT_VALUE = 22;
    private static final float TOUGHNESS = 0.0F;
    private static final float KNOCKBACK_RESISTANCE = 0.0F;
    private static final SoundEvent EQUIP_SOUND = SoundRegistry.VANILLA_INSCRIBE_MANA.get();

    private static final Map<ArmorItem.Type, Integer> BASE_DURABILITY = Map.of(
            ArmorItem.Type.HELMET, 11,
            ArmorItem.Type.CHESTPLATE, 16,
            ArmorItem.Type.LEGGINGS, 15,
            ArmorItem.Type.BOOTS, 13
    );

    private static final Map<ArmorItem.Type, Integer> DEFENSE = Map.of(
            ArmorItem.Type.HELMET, 1,
            ArmorItem.Type.CHESTPLATE, 1,
            ArmorItem.Type.LEGGINGS, 1,
            ArmorItem.Type.BOOTS, 1
    );

    private static final List<AttributeBonus> COMMON_ATTRIBUTE_BONUSES = List.of(
            new AttributeBonus(AttributeRegistry.MAX_MANA, 50.0D, AttributeModifier.Operation.ADDITION, "max_mana"),
            new AttributeBonus(AttributeRegistry.SPELL_POWER, 0.05D, AttributeModifier.Operation.MULTIPLY_BASE, "spell_power")
    );

    private static final Map<ArmorItem.Type, List<AttributeBonus>> ATTRIBUTE_BONUSES = Map.of(
            ArmorItem.Type.HELMET, COMMON_ATTRIBUTE_BONUSES,
            ArmorItem.Type.CHESTPLATE, COMMON_ATTRIBUTE_BONUSES,
            ArmorItem.Type.LEGGINGS, COMMON_ATTRIBUTE_BONUSES,
            ArmorItem.Type.BOOTS, COMMON_ATTRIBUTE_BONUSES
    );

    private StealthRuneArmorStats() {
    }

    public static int enchantmentValue() {
        return ENCHANTMENT_VALUE;
    }

    public static Multimap<Attribute, AttributeModifier> createAttributeModifiers(ArmorItem.Type type) {
        var bonuses = ATTRIBUTE_BONUSES.get(type);
        if (bonuses == null || bonuses.isEmpty()) {
            return ImmutableMultimap.of();
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        var prefix = "apprenticecodex.stealth_rune_armor." + typeToken(type);
        for (int i = 0; i < bonuses.size(); ++i) {
            var bonus = bonuses.get(i);
            var attribute = bonus.attributeSupplier().get();
            if (attribute == null) {
                continue;
            }

            var modifierSeed = prefix + "." + bonus.key() + "." + i;
            var modifierId = UUID.nameUUIDFromBytes(modifierSeed.getBytes(StandardCharsets.UTF_8));
            builder.put(
                    attribute,
                    new AttributeModifier(modifierId, modifierSeed, bonus.amount(), bonus.operation())
            );
        }
        return builder.build();
    }

    private static int durabilityFor(ArmorItem.Type type) {
        return BASE_DURABILITY.getOrDefault(type, 0) * DURABILITY_MULTIPLIER;
    }

    private static int defenseFor(ArmorItem.Type type) {
        return DEFENSE.getOrDefault(type, 0);
    }

    private static String typeToken(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
        };
    }

    private record AttributeBonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        private AttributeBonus {
            Objects.requireNonNull(attributeSupplier);
            Objects.requireNonNull(operation);
            Objects.requireNonNull(key);
        }
    }

    private static final class StealthRuneArmorMaterial implements ArmorMaterial {
        @Override
        public int getDurabilityForType(ArmorItem.@NotNull Type type) {
            return durabilityFor(type);
        }

        @Override
        public int getDefenseForType(ArmorItem.@NotNull Type type) {
            return defenseFor(type);
        }

        @Override
        public int getEnchantmentValue() {
            return ENCHANTMENT_VALUE;
        }

        @Override
        public @NotNull SoundEvent getEquipSound() {
            return EQUIP_SOUND;
        }

        @Override
        public @NotNull Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public @NotNull String getName() {
            return ApprenticeCodex.MODID + ":stealth_rune_armor";
        }

        @Override
        public float getToughness() {
            return TOUGHNESS;
        }

        @Override
        public float getKnockbackResistance() {
            return KNOCKBACK_RESISTANCE;
        }
    }
}
