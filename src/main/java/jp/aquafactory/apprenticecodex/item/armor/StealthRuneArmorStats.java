package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class StealthRuneArmorStats {
    private static final int DURABILITY_MULTIPLIER = 5;
    private static final int ENCHANTMENT_VALUE = 22;
    private static final float TOUGHNESS = 0.0F;
    private static final float KNOCKBACK_RESISTANCE = 0.0F;
    private static final Supplier<Ingredient> REPAIR_INGREDIENT = () -> Ingredient.EMPTY;

    private static final Map<ArmorItem.Type, Integer> DEFENSE = Map.of(
            ArmorItem.Type.HELMET, 1,
            ArmorItem.Type.CHESTPLATE, 1,
            ArmorItem.Type.LEGGINGS, 1,
            ArmorItem.Type.BOOTS, 1
    );

    private static final List<AttributeBonus> COMMON_ATTRIBUTE_BONUSES = List.of(
            new AttributeBonus(AttributeRegistry.MAX_MANA, 50.0D, AttributeModifier.Operation.ADD_VALUE, "max_mana"),
            new AttributeBonus(AttributeRegistry.SPELL_POWER, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "spell_power")
    );

    private static final Map<ArmorItem.Type, List<AttributeBonus>> ATTRIBUTE_BONUSES = Map.of(
            ArmorItem.Type.HELMET, COMMON_ATTRIBUTE_BONUSES,
            ArmorItem.Type.CHESTPLATE, COMMON_ATTRIBUTE_BONUSES,
            ArmorItem.Type.LEGGINGS, COMMON_ATTRIBUTE_BONUSES,
            ArmorItem.Type.BOOTS, COMMON_ATTRIBUTE_BONUSES
    );

    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            DEFENSE,
            ENCHANTMENT_VALUE,
            Holder.direct(SoundEvents.ENCHANTMENT_TABLE_USE),
            REPAIR_INGREDIENT,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "stealth_rune_armor"))),
            TOUGHNESS,
            KNOCKBACK_RESISTANCE
    );

    private StealthRuneArmorStats() {
    }

    public static Item.Properties createProperties(ArmorItem.Type type) {
        return new Item.Properties()
                .stacksTo(1)
                .durability(type.getDurability(DURABILITY_MULTIPLIER));
    }

    public static int enchantmentValue() {
        return ENCHANTMENT_VALUE;
    }

    public static boolean isRepairIngredient(ItemStack stack) {
        return REPAIR_INGREDIENT.get().test(stack);
    }

    public static ItemAttributeModifiers createAttributeModifiers(ArmorItem.Type type) {
        var bonuses = ATTRIBUTE_BONUSES.get(type);
        if (bonuses == null || bonuses.isEmpty()) {
            return ItemAttributeModifiers.EMPTY;
        }

        var builder = ItemAttributeModifiers.builder();
        var slotGroup = EquipmentSlotGroup.bySlot(type.getSlot());
        for (int i = 0; i < bonuses.size(); ++i) {
            var bonus = bonuses.get(i);
            builder.add(
                    bonus.attribute(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(
                                    ApprenticeCodex.MODID,
                                    "stealth_rune_armor_" + typeToken(type) + "_" + bonus.key() + "_" + i
                            ),
                            bonus.amount(),
                            bonus.operation()
                    ),
                    slotGroup
            );
        }

        return builder.build();
    }

    private static String typeToken(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case BODY -> "body";
        };
    }

    private record AttributeBonus(
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        private AttributeBonus {
            Objects.requireNonNull(attribute);
            Objects.requireNonNull(operation);
            Objects.requireNonNull(key);
        }
    }
}
