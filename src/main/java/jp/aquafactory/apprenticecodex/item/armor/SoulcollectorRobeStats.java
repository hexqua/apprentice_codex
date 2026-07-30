package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class SoulcollectorRobeStats {
    public static final double MAX_MANA_BONUS_PER_PIECE = 75.0D;
    private static final int DURABILITY_MULTIPLIER = 19;
    private static final int ENCHANTMENT_VALUE = 22;
    private static final Supplier<Ingredient> REPAIR_INGREDIENT = () -> Ingredient.of(ItemRegistry.MAGIC_CLOTH.get());
    private static final ResourceLocation LODESTONE_MAGIC_PROFICIENCY =
            ResourceLocation.fromNamespaceAndPath("lodestone", "magic_proficiency");
    private static final Map<ArmorItem.Type, Integer> DEFENSE = Map.of(
            ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 4,
            ArmorItem.Type.LEGGINGS, 3, ArmorItem.Type.BOOTS, 1
    );
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            DEFENSE, ENCHANTMENT_VALUE, SoundRegistry.VANILLA_ARMOR_EQUIP_ROBE, REPAIR_INGREDIENT,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "soulcollector_robe"))),
            0.0F, 0.0F
    );

    private SoulcollectorRobeStats() {
    }

    public static Item.Properties createProperties(ArmorItem.Type type) {
        return new Item.Properties().stacksTo(1).durability(type.getDurability(DURABILITY_MULTIPLIER));
    }

    public static int enchantmentValue() {
        return ENCHANTMENT_VALUE;
    }

    public static boolean isRepairIngredient(ItemStack stack) {
        return REPAIR_INGREDIENT.get().test(stack);
    }

    public static ItemAttributeModifiers createAttributeModifiers(ArmorItem.Type type) {
        var builder = ItemAttributeModifiers.builder();
        var slot = EquipmentSlotGroup.bySlot(type.getSlot());
        builder.add(AttributeRegistry.MAX_MANA, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "soulcollector_robe_" + token(type) + "_max_mana"),
                MAX_MANA_BONUS_PER_PIECE, AttributeModifier.Operation.ADD_VALUE), slot);
        return builder.build();
    }

    static void addConfiguredModifiers(ItemAttributeModifiers.Builder builder, ArmorItem.Type type,
                                       double spellPower, double magicProficiency) {
        var slot = EquipmentSlotGroup.bySlot(type.getSlot());
        MagicArmorAttributeHelper.addModifier(builder, AttributeRegistry.SPELL_POWER, spellPower,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE, slot,
                "soulcollector_robe_" + token(type) + "_spell_power_config");
        var proficiency = BuiltInRegistries.ATTRIBUTE.getOptional(LODESTONE_MAGIC_PROFICIENCY).orElse(null);
        if (proficiency != null) {
            MagicArmorAttributeHelper.addModifier(builder, BuiltInRegistries.ATTRIBUTE.wrapAsHolder(proficiency), magicProficiency,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE, slot,
                    "soulcollector_robe_" + token(type) + "_magic_proficiency_config");
        }
    }

    private static String token(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case BODY -> "body";
        };
    }
}
