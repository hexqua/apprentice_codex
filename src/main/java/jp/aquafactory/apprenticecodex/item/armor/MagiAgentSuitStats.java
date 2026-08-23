package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

public final class MagiAgentSuitStats {
    public static final double MAX_MANA_BONUS = 125.0D;

    private static final int DURABILITY_MULTIPLIER = 33;
    private static final int ENCHANTMENT_VALUE = 22;
    private static final float MATERIAL_TOUGHNESS = 0.0F;
    private static final float KNOCKBACK_RESISTANCE = 0.0F;
    private static final Supplier<Ingredient> REPAIR_INGREDIENT =
            () -> Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get());

    private static final Map<ArmorItem.Type, Integer> DEFENSE = Map.of(
            ArmorItem.Type.HELMET, 3,
            ArmorItem.Type.CHESTPLATE, 7,
            ArmorItem.Type.LEGGINGS, 6,
            ArmorItem.Type.BOOTS, 3
    );

    private static final Map<ArmorItem.Type, Double> TOUGHNESS = Map.of(
            ArmorItem.Type.HELMET, 1.0D,
            ArmorItem.Type.CHESTPLATE, 1.0D,
            ArmorItem.Type.LEGGINGS, 2.0D,
            ArmorItem.Type.BOOTS, 1.0D
    );

    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            DEFENSE,
            ENCHANTMENT_VALUE,
            SoundRegistry.VANILLA_ARMOR_EQUIP_ROBE,
            REPAIR_INGREDIENT,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "magi_agent_suit"))),
            MATERIAL_TOUGHNESS,
            KNOCKBACK_RESISTANCE
    );

    private MagiAgentSuitStats() {
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
        var bonuses = List.of(
                new AttributeBonus(AttributeRegistry.MAX_MANA, MAX_MANA_BONUS, AttributeModifier.Operation.ADD_VALUE, "max_mana"),
                new AttributeBonus(Attributes.ARMOR_TOUGHNESS, toughnessFor(type), AttributeModifier.Operation.ADD_VALUE, "armor_toughness")
        );

        var builder = ItemAttributeModifiers.builder();
        var slotGroup = EquipmentSlotGroup.bySlot(type.getSlot());
        for (int i = 0; i < bonuses.size(); ++i) {
            var bonus = bonuses.get(i);
            if (bonus.amount() == 0.0D) {
                continue;
            }

            builder.add(
                    bonus.attribute(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(
                                    ApprenticeCodex.MODID,
                                    "magi_agent_suit_" + typeToken(type) + "_" + bonus.key() + "_" + i
                            ),
                            bonus.amount(),
                            bonus.operation()
                    ),
                    slotGroup
            );
        }
        return builder.build();
    }

    static void addSpellPowerModifier(
            ItemAttributeModifiers.Builder builder,
            ArmorItem.Type type,
            double amount
    ) {
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.SPELL_POWER,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                EquipmentSlotGroup.bySlot(type.getSlot()),
                "magi_agent_suit_" + typeToken(type) + "_spell_power_config"
        );
    }

    static void addSchoolSpellPowerModifier(
            ItemAttributeModifiers.Builder builder,
            Holder<Attribute> attribute,
            ArmorItem.Type type,
            double amount
    ) {
        MagicArmorAttributeHelper.addModifier(
                builder,
                attribute,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                EquipmentSlotGroup.bySlot(type.getSlot()),
                "magi_agent_suit_" + typeToken(type) + "_school_spell_power_config"
        );
    }

    static String typeToken(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case BODY -> "body";
        };
    }

    private static double toughnessFor(ArmorItem.Type type) {
        return TOUGHNESS.getOrDefault(type, 0.0D);
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
