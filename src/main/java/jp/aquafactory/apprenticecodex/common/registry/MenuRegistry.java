package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.capability.Capabilities;
import jp.aquafactory.apprenticecodex.common.capability.personalinventory.PersonalInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ApprenticeCodex.MODID);

    public static final RegistryObject<MenuType<PersonalInventoryMenu>> PERSONAL_SHELF =
            MENUS.register("personal_shelf", () -> IForgeMenuType.create((windowId, inv, data) -> {
                var shelf = inv.player.getCapability(Capabilities.PERSONAL_INVENTORY).orElseThrow(() -> new IllegalStateException("personal inventory (for Personal Shelf spell) capability missing"));
                return new PersonalInventoryMenu(windowId, inv, shelf.getHandler(), data.readBlockPos(), data.readInt());
            }));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
