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

    public static final TagKey<Item> MAGIC_ITEM_ENCHANTABLE = itemTag("magic_item_enchantable");
    public static final TagKey<Item> OFFHAND_MAGIC_ENCHANTABLE = itemTag("offhand_magic_enchantable");
    public static final TagKey<Enchantment> EXCLUSIVE_REFLUX_RESERVOIR = enchantmentTag("exclusive_set/reflux_reservoir");
    public static final TagKey<Enchantment> EXCLUSIVE_ALACRITY_TENSE = enchantmentTag("exclusive_set/alacrity_tense");
    public static final TagKey<Enchantment> EXCLUSIVE_SURGE_ATTUNEMENT_TRANSCENDENCE =
            enchantmentTag("exclusive_set/surge_attunement_transcendence");

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
        var offhandMagicItems = itemLookup.getOrThrow(OFFHAND_MAGIC_ENCHANTABLE);

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
                                        offhandMagicItems,
                                        offhandMagicItems,
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
                                        offhandMagicItems,
                                        offhandMagicItems,
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
                                        magicItems,
                                        magicItems,
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
                                        magicItems,
                                        magicItems,
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
    }

    private static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        context.register(key, builder.build(key.location()));
    }
}
