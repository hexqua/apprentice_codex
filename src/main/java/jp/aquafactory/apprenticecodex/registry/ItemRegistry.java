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
    private ItemRegistry() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ApprenticeCodex.MODID);

    private static RegistryObject<Item> simple(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    public static final RegistryObject<Item> SKY_EDGE_SWORD = simple("sky_edge_sword");
    public static final RegistryObject<Item> ARCHER_MULTIPLE_BOW = simple("archer_multiple_bow");
    public static final RegistryObject<Item> COMMENCE_FIRE_RIFLE = simple("commence_fire_rifle");
    public static final RegistryObject<Item> QUICK_ARMS_HANDGUN = simple("quick_arms_handgun");
    public static final RegistryObject<Item> BREACHING_ENEMY_SHOTGUN = simple("breaching_enemy_shotgun");
    public static final RegistryObject<Item> FLY_SWATTER_LAUNCHER = simple("fly_swatter_launcher");

    public static final RegistryObject<Item> SCARLET_THIRST =
            ITEMS.register("scarlet_thirst", ScarletThirst::new);

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
