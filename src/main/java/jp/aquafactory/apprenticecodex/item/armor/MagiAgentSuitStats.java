package jp.aquafactory.apprenticecodex.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class MagiAgentSuitStats {
    public static final ArmorMaterial MATERIAL = new MagiAgentSuitMaterial();
    public static final double MAX_MANA_BONUS = 125.0D;

    private static final int DURABILITY_MULTIPLIER = 33;
    private static final int ENCHANTMENT_VALUE = 22;
    private static final float MATERIAL_TOUGHNESS = 0.0F;
    private static final float KNOCKBACK_RESISTANCE = 0.0F;
    private static final SoundEvent EQUIP_SOUND = SoundRegistry.VANILLA_ARMOR_EQUIP_ROBE.get();
    private static final Supplier<Ingredient> REPAIR_INGREDIENT =
            () -> Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get());

    private static final Map<ArmorItem.Type, Integer> BASE_DURABILITY = Map.of(
            ArmorItem.Type.HELMET, 11,
            ArmorItem.Type.CHESTPLATE, 16,
            ArmorItem.Type.LEGGINGS, 15,
            ArmorItem.Type.BOOTS, 13
    );

    private static final Map<ArmorItem.Type, Integer> DEFENSE = Map.of(
            ArmorItem.Type.HELMET, 3,
            ArmorItem.Type.CHESTPLATE, 6,
            ArmorItem.Type.LEGGINGS, 6,
            ArmorItem.Type.BOOTS, 3
    );

    private static final Map<ArmorItem.Type, Double> TOUGHNESS = Map.of(
            ArmorItem.Type.HELMET, 1.0D,
            ArmorItem.Type.CHESTPLATE, 1.0D,
            ArmorItem.Type.LEGGINGS, 2.0D,
            ArmorItem.Type.BOOTS, 1.0D
    );

    private MagiAgentSuitStats() {
    }

    public static int enchantmentValue() {
        return ENCHANTMENT_VALUE;
    }

    public static boolean isRepairIngredient(ItemStack stack) {
        return REPAIR_INGREDIENT.get().test(stack);
    }

    public static Multimap<Attribute, AttributeModifier> createAttributeModifiers(ArmorItem.Type type) {
        var bonuses = List.of(
                new AttributeBonus(AttributeRegistry.MAX_MANA, MAX_MANA_BONUS, AttributeModifier.Operation.ADDITION, "max_mana"),
                new AttributeBonus(() -> Attributes.ARMOR_TOUGHNESS, toughnessFor(type), AttributeModifier.Operation.ADDITION, "armor_toughness")
        );

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        var prefix = "apprenticecodex.magi_agent_suit." + typeToken(type);
        for (int i = 0; i < bonuses.size(); ++i) {
            var bonus = bonuses.get(i);
            var attribute = bonus.attributeSupplier().get();
            if (attribute == null || bonus.amount() == 0.0D) {
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

    static void addSpellPowerModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ArmorItem.Type type,
            double amount
    ) {
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.SPELL_POWER.get(),
                amount,
                AttributeModifier.Operation.MULTIPLY_BASE,
                "apprenticecodex.magi_agent_suit." + typeToken(type) + ".spell_power.1"
        );
    }

    static void addSchoolSpellPowerModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            Attribute attribute,
            ArmorItem.Type type,
            double amount
    ) {
        MagicArmorAttributeHelper.addModifier(
                builder,
                attribute,
                amount,
                AttributeModifier.Operation.MULTIPLY_BASE,
                "apprenticecodex.magi_agent_suit." + typeToken(type) + ".school_spell_power.1"
        );
    }

    static String typeToken(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
        };
    }

    private static int durabilityFor(ArmorItem.Type type) {
        return BASE_DURABILITY.getOrDefault(type, 0) * DURABILITY_MULTIPLIER;
    }

    private static int defenseFor(ArmorItem.Type type) {
        return DEFENSE.getOrDefault(type, 0);
    }

    private static double toughnessFor(ArmorItem.Type type) {
        return TOUGHNESS.getOrDefault(type, 0.0D);
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

    private static final class MagiAgentSuitMaterial implements ArmorMaterial {
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
            return REPAIR_INGREDIENT.get();
        }

        @Override
        public @NotNull String getName() {
            return ApprenticeCodex.MODID + ":magi_agent_suit";
        }

        @Override
        public float getToughness() {
            return MATERIAL_TOUGHNESS;
        }

        @Override
        public float getKnockbackResistance() {
            return KNOCKBACK_RESISTANCE;
        }
    }
}
