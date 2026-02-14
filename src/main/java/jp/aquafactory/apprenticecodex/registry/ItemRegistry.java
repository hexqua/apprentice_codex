package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.ScarletThirst;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ApprenticeCodex.MODID);

    public static final RegistryObject<Item> SKY_EDGE_SWORD =
            ITEMS.register("sky_edge_sword",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ARCHER_MULTIPLE_BOW =
            ITEMS.register("archer_multiple_bow",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COMMENCE_FIRE_RIFLE =
            ITEMS.register("commence_fire_rifle",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> QUICK_ARMS_HANDGUN =
            ITEMS.register("quick_arms_handgun",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BREACHING_ENEMY_SHOTGUN =
            ITEMS.register("breaching_enemy_shotgun",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FLY_SWATTER_LAUNCHER =
            ITEMS.register("fly_swatter_launcher",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SCARLET_THIRST =
            ITEMS.register("scarlet_thirst", ScarletThirst::new);

    private ItemRegistry() {
        // do nothing.
    }

    public static void register() {
        ItemProperties.register(
                ItemRegistry.ARCHER_MULTIPLE_BOW.get(),
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "stage"),
                (stack, level, living, seed) -> {
                    CompoundTag tag = stack.getTag();
                    int stage = (tag != null) ? tag.getInt("Stage") : 0;
                    return (float) stage;
                }
        );
    }
}
