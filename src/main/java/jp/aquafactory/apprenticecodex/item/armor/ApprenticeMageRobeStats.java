package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
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

public final class ApprenticeMageRobeStats {
    // 耐久力係数、鉄=15、ダイヤ=33、ネザライト=37。魔法防具なので鉄より固く、ダイヤTierではない想定。
    private static final int DURABILITY_MULTIPLIER = 24;
    private static final int ENCHANTMENT_VALUE = 22;
    private static final float TOUGHNESS = 0.0F;
    private static final float KNOCKBACK_RESISTANCE = 0.0F;
    private static final Supplier<Ingredient> REPAIR_INGREDIENT = () -> Ingredient.of(ItemRegistry.ARCANE_ESSENCE.get());

    private static final Map<ArmorItem.Type, Integer> DEFENSE = Map.of(
            ArmorItem.Type.HELMET, 2,
            ArmorItem.Type.CHESTPLATE, 5,
            ArmorItem.Type.LEGGINGS, 3,
            ArmorItem.Type.BOOTS, 1
    );

    private static final List<AttributeBonus> DEFAULT_ATTRIBUTE_BONUSES = List.of(
            new AttributeBonus(AttributeRegistry.MAX_MANA, 50.0D, AttributeModifier.Operation.ADD_VALUE, "max_mana")
    );

    private static final Map<ArmorItem.Type, List<AttributeBonus>> ATTRIBUTE_BONUSES = Map.of(
            ArmorItem.Type.HELMET, DEFAULT_ATTRIBUTE_BONUSES,
            ArmorItem.Type.CHESTPLATE, DEFAULT_ATTRIBUTE_BONUSES,
            ArmorItem.Type.LEGGINGS, DEFAULT_ATTRIBUTE_BONUSES,
            ArmorItem.Type.BOOTS, DEFAULT_ATTRIBUTE_BONUSES
    );

    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            DEFENSE,
            ENCHANTMENT_VALUE,
            SoundRegistry.VANILLA_ARMOR_EQUIP_ROBE,
            REPAIR_INGREDIENT,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_mage"))),
            TOUGHNESS,
            KNOCKBACK_RESISTANCE
    );

    private ApprenticeMageRobeStats() {
    }

    public static Item.Properties createProperties(ArmorItem.Type type) {
        return new Item.Properties()
                .stacksTo(1)
                .durability(type.getDurability(DURABILITY_MULTIPLIER));
    }

    public static boolean isRepairIngredient(ItemStack stack) {
        return REPAIR_INGREDIENT.get().test(stack);
    }

    public static int enchantmentValue() {
        return ENCHANTMENT_VALUE;
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
                                    "apprentice_mage_robe_" + typeToken(type) + "_" + bonus.key() + "_" + i
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
                "apprentice_mage_robe_" + typeToken(type) + "_spell_power_config"
        );
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
