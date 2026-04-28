package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class Enchantments {
    private Enchantments() {
    }

    public static final ResourceKey<Enchantment> REFLUX = key("reflux");
    public static final ResourceKey<Enchantment> RESERVOIR = key("reservoir");
    public static final ResourceKey<Enchantment> ALACRITY = key("alacrity");
    public static final ResourceKey<Enchantment> TENSE = key("tense");
    public static final ResourceKey<Enchantment> SURGE = key("surge");
    public static final ResourceKey<Enchantment> ATTUNEMENT = key("attunement");
    public static final ResourceKey<Enchantment> TRANSCENDENCE = key("transcendence");
    public static final ResourceKey<Enchantment> WISDOM = key("wisdom");
    public static final ResourceKey<Enchantment> PLUNDER = key("plunder");
    public static final ResourceKey<Enchantment> GUZZLE = key("guzzle");
    public static final ResourceKey<Enchantment> LARGE_MUG = key("large_mug");
    public static final ResourceKey<Enchantment> RED_ENERGY = key("red_energy");
    public static final ResourceKey<Enchantment> GLOW_ENERGY = key("glow_energy");
    public static final ResourceKey<Enchantment> SYNTHESIS = key("synthesis");
    public static final ResourceKey<Enchantment> SHELL = key("shell");
    public static final ResourceKey<Enchantment> SYNCHRONIZATION = key("synchronization");
    public static final ResourceKey<Enchantment> NEUTRALIZATION = key("neutralization");

    public static final TagKey<Item> MAGIC_ITEM_ENCHANTABLE = itemTag("magic_item_enchantable");
    public static final TagKey<Item> OFFHAND_MAGIC_ENCHANTABLE = itemTag("offhand_magic_enchantable");
    public static final TagKey<Item> OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE = itemTag("offhand_or_armor_magic_enchantable");
    public static final TagKey<Item> SPELL_CONTAINER_MAGIC_ENCHANTABLE = itemTag("spell_container_magic_enchantable");
    public static final TagKey<Item> SPELL_GUN_ENCHANTABLE = itemTag("spell_gun_enchantable");
    public static final TagKey<Item> DRINKABLE_FLASK_ENCHANTABLE = itemTag("drinkable_flask_enchantable");
    public static final TagKey<Item> ALCHEMISTS_FLASK_ENCHANTABLE = itemTag("alchemists_flask_enchantable");
    public static final TagKey<Item> FLASK_ENCHANTABLE = itemTag("flask_enchantable");
    public static final TagKey<Item> TRANSCENDENCE_ENCHANTABLE = itemTag("transcendence_enchantable");
    public static final TagKey<Item> WISDOM_ENCHANTABLE = itemTag("wisdom_enchantable");
    public static final TagKey<Item> PLUNDER_ENCHANTABLE = itemTag("plunder_enchantable");
    public static final TagKey<Item> SYNTHESIS_ENCHANTABLE = itemTag("synthesis_enchantable");
    public static final TagKey<Item> MANA_SHIELD_CHARM_ENCHANTABLE = itemTag("mana_shield_charm_enchantable");
    public static final TagKey<Enchantment> EXCLUSIVE_REFLUX_RESERVOIR = enchantmentTag("exclusive_set/reflux_reservoir");
    public static final TagKey<Enchantment> EXCLUSIVE_ALACRITY_TENSE = enchantmentTag("exclusive_set/alacrity_tense");
    public static final TagKey<Enchantment> EXCLUSIVE_SURGE_ATTUNEMENT_TRANSCENDENCE =
            enchantmentTag("exclusive_set/surge_attunement_transcendence");
    public static final TagKey<Enchantment> EXCLUSIVE_RED_GLOW_ENERGY =
            enchantmentTag("exclusive_set/red_glow_energy");
    public static final TagKey<Enchantment> EXCLUSIVE_SYNTHESIS =
            enchantmentTag("exclusive_set/synthesis");
    public static final TagKey<Enchantment> EXCLUSIVE_MANA_SHIELD_CHARM =
            enchantmentTag("exclusive_set/mana_shield_charm");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name)
        );
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path)
        );
    }

    private static TagKey<Enchantment> enchantmentTag(String path) {
        return TagKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path)
        );
    }

    public static int getLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.isEmpty()) {
            return 0;
        }

        for (var holder : enchantments.keySet()) {
            if (holder.is(enchantmentKey)) {
                return enchantments.getLevel(holder);
            }
        }

        return 0;
    }

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> itemLookup = context.lookup(Registries.ITEM);
        HolderGetter<Enchantment> enchantmentLookup = context.lookup(Registries.ENCHANTMENT);
        var magicItems = itemLookup.getOrThrow(MAGIC_ITEM_ENCHANTABLE);
        var offhandOrArmorMagicItems = itemLookup.getOrThrow(OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE);
        var spellContainerMagicItems = itemLookup.getOrThrow(SPELL_CONTAINER_MAGIC_ENCHANTABLE);
        var spellGunItems = itemLookup.getOrThrow(SPELL_GUN_ENCHANTABLE);
        var drinkableFlaskItems = itemLookup.getOrThrow(DRINKABLE_FLASK_ENCHANTABLE);
        var flaskItems = itemLookup.getOrThrow(FLASK_ENCHANTABLE);
        var transcendenceItems = itemLookup.getOrThrow(TRANSCENDENCE_ENCHANTABLE);
        var wisdomItems = itemLookup.getOrThrow(WISDOM_ENCHANTABLE);
        var plunderItems = itemLookup.getOrThrow(PLUNDER_ENCHANTABLE);
        var synthesisItems = itemLookup.getOrThrow(SYNTHESIS_ENCHANTABLE);
        var manaShieldCharmItems = itemLookup.getOrThrow(MANA_SHIELD_CHARM_ENCHANTABLE);

        register(
                context,
                REFLUX,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        magicItems,
                                        magicItems,
                                        5,
                                        5,
                                        Enchantment.dynamicCost(1, 10),
                                        Enchantment.dynamicCost(51, 10),
                                        1,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_REFLUX_RESERVOIR))
        );

        register(
                context,
                RESERVOIR,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        magicItems,
                                        magicItems,
                                        10,
                                        5,
                                        Enchantment.dynamicCost(1, 10),
                                        Enchantment.dynamicCost(51, 10),
                                        1,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_REFLUX_RESERVOIR))
        );

        register(
                context,
                ALACRITY,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        offhandOrArmorMagicItems,
                                        offhandOrArmorMagicItems,
                                        5,
                                        5,
                                        Enchantment.dynamicCost(5, 8),
                                        Enchantment.dynamicCost(25, 8),
                                        1,
                                        EquipmentSlotGroup.OFFHAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_ALACRITY_TENSE))
        );

        register(
                context,
                TENSE,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        offhandOrArmorMagicItems,
                                        offhandOrArmorMagicItems,
                                        10,
                                        5,
                                        Enchantment.dynamicCost(5, 8),
                                        Enchantment.dynamicCost(25, 8),
                                        1,
                                        EquipmentSlotGroup.OFFHAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_ALACRITY_TENSE))
        );

        register(
                context,
                SURGE,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        magicItems,
                                        magicItems,
                                        10,
                                        5,
                                        Enchantment.dynamicCost(5, 8),
                                        Enchantment.dynamicCost(25, 8),
                                        2,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_SURGE_ATTUNEMENT_TRANSCENDENCE))
        );

        register(
                context,
                ATTUNEMENT,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        spellContainerMagicItems,
                                        spellContainerMagicItems,
                                        5,
                                        5,
                                        Enchantment.dynamicCost(5, 11),
                                        Enchantment.dynamicCost(25, 11),
                                        2,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_SURGE_ATTUNEMENT_TRANSCENDENCE))
        );

        register(
                context,
                TRANSCENDENCE,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        transcendenceItems,
                                        transcendenceItems,
                                        1,
                                        3,
                                        Enchantment.dynamicCost(25, 8),
                                        Enchantment.dynamicCost(75, 8),
                                        8,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_SURGE_ATTUNEMENT_TRANSCENDENCE))
        );

        register(
                context,
                WISDOM,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        wisdomItems,
                                        wisdomItems,
                                        2,
                                        3,
                                        Enchantment.dynamicCost(15, 9),
                                        Enchantment.dynamicCost(65, 9),
                                        2,
                                        EquipmentSlotGroup.HAND
                                )
                        )
        );

        register(
                context,
                PLUNDER,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        plunderItems,
                                        plunderItems,
                                        2,
                                        3,
                                        Enchantment.dynamicCost(15, 9),
                                        Enchantment.dynamicCost(65, 9),
                                        2,
                                        EquipmentSlotGroup.HAND
                                )
                        )
        );

        register(
                context,
                GUZZLE,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        drinkableFlaskItems,
                                        drinkableFlaskItems,
                                        5,
                                        5,
                                        Enchantment.dynamicCost(5, 11),
                                        Enchantment.dynamicCost(25, 11),
                                        1,
                                        EquipmentSlotGroup.HAND
                                )
                        )
        );

        register(
                context,
                LARGE_MUG,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        flaskItems,
                                        flaskItems,
                                        10,
                                        4,
                                        Enchantment.dynamicCost(1, 10),
                                        Enchantment.dynamicCost(51, 10),
                                        1,
                                        EquipmentSlotGroup.HAND
                                )
                        )
        );

        register(
                context,
                RED_ENERGY,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        flaskItems,
                                        flaskItems,
                                        2,
                                        4,
                                        Enchantment.dynamicCost(20, 9),
                                        Enchantment.dynamicCost(70, 9),
                                        2,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_RED_GLOW_ENERGY))
        );

        register(
                context,
                GLOW_ENERGY,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        flaskItems,
                                        flaskItems,
                                        1,
                                        3,
                                        Enchantment.dynamicCost(20, 9),
                                        Enchantment.dynamicCost(70, 9),
                                        2,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_RED_GLOW_ENERGY))
        );

        register(
                context,
                SYNTHESIS,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        synthesisItems,
                                        synthesisItems,
                                        1,
                                        1,
                                        Enchantment.dynamicCost(20, 10),
                                        Enchantment.dynamicCost(50, 10),
                                        1,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_SYNTHESIS))
        );

        register(
                context,
                SHELL,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        manaShieldCharmItems,
                                        manaShieldCharmItems,
                                        2,
                                        1,
                                        Enchantment.constantCost(20),
                                        Enchantment.constantCost(50),
                                        1,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_MANA_SHIELD_CHARM))
        );

        register(
                context,
                SYNCHRONIZATION,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        manaShieldCharmItems,
                                        manaShieldCharmItems,
                                        2,
                                        1,
                                        Enchantment.constantCost(20),
                                        Enchantment.constantCost(50),
                                        1,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_MANA_SHIELD_CHARM))
        );

        register(
                context,
                NEUTRALIZATION,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        manaShieldCharmItems,
                                        manaShieldCharmItems,
                                        2,
                                        1,
                                        Enchantment.constantCost(20),
                                        Enchantment.constantCost(50),
                                        1,
                                        EquipmentSlotGroup.HAND
                                )
                        )
                        .exclusiveWith(enchantmentLookup.getOrThrow(EXCLUSIVE_MANA_SHIELD_CHARM))
        );
    }

    private static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        context.register(key, builder.build(key.location()));
    }
}
