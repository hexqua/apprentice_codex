package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, ApprenticeCodex.MODID);

    public static final DeferredHolder<Item, Item> SKY_EDGE_SWORD =
            ITEMS.register("sky_edge_sword",
                    () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> ARCHER_MULTIPLE_BOW =
            ITEMS.register("archer_multiple_bow",
                    () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> COMMENCE_FIRE_RIFLE =
            ITEMS.register("commence_fire_rifle",
                    () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> QUICK_ARMS_HANDGUN =
            ITEMS.register("quick_arms_handgun",
                    () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> BREACHING_ENEMY_SHOTGUN =
            ITEMS.register("breaching_enemy_shotgun",
                    () -> new Item(new Item.Properties()));

    private ItemRegistry() {
        // do nothing.
    }

    public static void register() {
        ItemProperties.register(
                ItemRegistry.ARCHER_MULTIPLE_BOW.get(),
                ResourceLocation.fromNamespaceAndPath("apprenticecodex", "stage"),
                (stack, level, living, seed) -> {
                    @SuppressWarnings("deprecation")
                    var customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
                    int stage = customData.getInt("Stage");
                    return (float) stage;
                }
        );
    }
}
