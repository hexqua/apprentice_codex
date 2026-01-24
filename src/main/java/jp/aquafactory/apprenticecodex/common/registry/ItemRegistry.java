package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ApprenticeCodex.MODID);

    public static final RegistryObject<Item> SKY_EDGE_SWORD =
            ITEMS.register("sky_edge_sword",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ARCHER_MULTIPLE_BOW =
            ITEMS.register("archer_multiple_bow",
                    () -> new Item(new Item.Properties()));

    private ItemRegistry() {
        // do nothing.
    }
}
