package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CreativeTabRegistry {
    private CreativeTabRegistry(){}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ApprenticeCodex.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ApprenticeCodex.MODID + ".main"))
                    .icon(() -> new ItemStack(ItemRegistry.APPRENTICE_DESK.get()))
                    .displayItems(CreativeTabRegistry::addItemsToTab)
                    .build()
            );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private static void addItemsToTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        output.accept(ItemRegistry.APPRENTICE_DESK.get());
        output.accept(ItemRegistry.SCARLET_THIRST.get());
        output.accept(ItemRegistry.CRAFTSMANS_DELIGHT.get());
    }
}
