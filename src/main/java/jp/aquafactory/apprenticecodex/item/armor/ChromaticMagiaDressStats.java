package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
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

public final class ChromaticMagiaDressStats {
    private static final int DURABILITY_MULTIPLIER = 37;
    private static final int ENCHANTMENT_VALUE = 22;
    private static final float TOUGHNESS = 1.0F;
    private static final float KNOCKBACK_RESISTANCE = 0.0F;
    private static final Supplier<Ingredient> REPAIR_INGREDIENT =
            () -> Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get());

    private static final Map<ArmorItem.Type, Integer> DEFENSE = Map.of(
            ArmorItem.Type.HELMET, 2,
            ArmorItem.Type.CHESTPLATE, 7,
            ArmorItem.Type.LEGGINGS, 6,
            ArmorItem.Type.BOOTS, 2
    );

    private static final List<AttributeBonus> COMMON_ATTRIBUTE_BONUSES = List.of(
            new AttributeBonus(AttributeRegistry.MAX_MANA, 125.0D, AttributeModifier.Operation.ADD_VALUE, "max_mana")
    );

    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            DEFENSE,
            ENCHANTMENT_VALUE,
            SoundRegistry.VANILLA_ARMOR_EQUIP_ROBE,
            REPAIR_INGREDIENT,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "chromatic_magia_dress"))),
            TOUGHNESS,
            KNOCKBACK_RESISTANCE
    );

    private ChromaticMagiaDressStats() {
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
        var builder = ItemAttributeModifiers.builder();
        var slotGroup = EquipmentSlotGroup.bySlot(type.getSlot());
        for (int i = 0; i < COMMON_ATTRIBUTE_BONUSES.size(); ++i) {
            var bonus = COMMON_ATTRIBUTE_BONUSES.get(i);
            builder.add(
                    bonus.attribute(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(
                                    ApprenticeCodex.MODID,
                                    "chromatic_magia_dress_" + typeToken(type) + "_" + bonus.key() + "_" + i
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
                "chromatic_magia_dress_" + typeToken(type) + "_spell_power_config"
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
