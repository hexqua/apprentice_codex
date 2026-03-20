package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDeskMenu;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationMenu;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoireInscriptionMenu;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ApprenticeCodex.MODID);

    public static final RegistryObject<MenuType<PersonalShelfMenu>> PERSONAL_SHELF =
            MENUS.register("personal_shelf", () -> IForgeMenuType.create((windowId, inv, data) -> {
                var shelf = inv.player.getCapability(Capabilities.PERSONAL_INVENTORY).orElseThrow(() -> new IllegalStateException("personal inventory (for Personal Shelf spell) capability missing"));
                return new PersonalShelfMenu(windowId, inv, shelf.getHandler(), data.readBlockPos());
            }));

    public static final RegistryObject<MenuType<ApprenticeDeskMenu>> APPRENTICE_DESK =
            MENUS.register("apprentice_desk", () -> IForgeMenuType.create((windowId, inv, data) -> new ApprenticeDeskMenu(windowId, inv)));

    public static final RegistryObject<MenuType<SpellcasterWorkbenchMenu>> SPELLCASTER_WORKBENCH =
            MENUS.register("spellcaster_workbench", () -> IForgeMenuType.create((windowId, inv, data) -> new SpellcasterWorkbenchMenu(windowId, inv)));

    public static final RegistryObject<MenuType<AtelierStationMenu>> ATELIER_STATION =
            MENUS.register("atelier_station", () -> IForgeMenuType.create((windowId, inv, data) -> new AtelierStationMenu(windowId, inv, data.readBlockPos())));

    public static final RegistryObject<MenuType<EnderGrimoireInscriptionMenu>> ENDER_GRIMOIRE_INSCRIPTION =
            MENUS.register("ender_grimoire_inscription", () -> IForgeMenuType.create((windowId, inv, data) -> new EnderGrimoireInscriptionMenu(windowId, inv)));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
